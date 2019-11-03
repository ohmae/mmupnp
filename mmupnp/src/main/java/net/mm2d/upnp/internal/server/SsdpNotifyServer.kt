/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import net.mm2d.log.Logger
import net.mm2d.upnp.common.ServerConst
import net.mm2d.upnp.common.SsdpMessage
import net.mm2d.upnp.common.internal.message.SsdpRequest
import net.mm2d.upnp.common.internal.thread.TaskExecutors
import java.io.IOException
import java.net.*

/**
 * Receiver for SSDP NOTIFY
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SsdpNotifyServer(
    private val delegate: SsdpServerDelegate
) : SsdpServer by delegate {
    private var notifyListener: ((SsdpMessage) -> Unit)? = null
    private var segmentCheckEnabled: Boolean = false
    private var shouldNotAccept: SsdpMessage.() -> Boolean = { false }
    // VisibleForTesting
    internal val interfaceAddress: InterfaceAddress
        get() = delegate.interfaceAddress

    constructor(
        taskExecutors: TaskExecutors,
        address: Address,
        ni: NetworkInterface
    ) : this(SsdpServerDelegate(taskExecutors, address, ni, ServerConst.SSDP_PORT)) {
        delegate.setReceiver { sourceAddress, data, length ->
            onReceive(sourceAddress, data, length)
        }
    }

    fun setSegmentCheckEnabled(enabled: Boolean) {
        segmentCheckEnabled = enabled
    }

    fun setNotifyListener(listener: ((SsdpMessage) -> Unit)?) {
        notifyListener = listener
    }

    fun setFilter(predicate: (SsdpMessage) -> Boolean) {
        shouldNotAccept = { !predicate(this) }
    }

    // VisibleForTesting
    internal fun onReceive(sourceAddress: InetAddress, data: ByteArray, length: Int) {
        if (sourceAddress.isInvalidAddress()) {
            return
        }
        try {
            val message = createSsdpRequestMessage(data, length)
            Logger.v { "receive ssdp notify from $sourceAddress in ${delegate.getLocalAddress()}:\n$message" }

            if (message.shouldNotAccept()) return
            // ignore M-SEARCH packet
            if (message.getMethod() == SsdpMessage.M_SEARCH) return
            if (message.isNotUpnp()) return
            // ByeBye accepts it regardless of address problems because it does not communicate
            if (message.nts != SsdpMessage.SSDP_BYEBYE &&
                message.hasInvalidLocation(sourceAddress)
            ) return

            notifyListener?.invoke(message)
        } catch (ignored: IOException) {
        }
    }

    @Throws(IOException::class)
    fun createSsdpRequestMessage(data: ByteArray, length: Int): SsdpRequest =
        SsdpRequest.create(delegate.getLocalAddress(), data, length)

    // VisibleForTesting
    internal fun InetAddress.isInvalidAddress(): Boolean {
        if (isInvalidVersion()) {
            Logger.w { "IP version mismatch:$this $interfaceAddress" }
            return true
        }
        // Even if the address setting is incorrect, multicast packets can be sent.
        // Since the segment information is incorrect and packets from parties
        // that can not be exchanged except for multicast are useless even if received, they are discarded.
        if (segmentCheckEnabled &&
            delegate.address == Address.IP_V4 &&
            isInvalidSegment(interfaceAddress)
        ) {
            Logger.w { "Invalid segment:$this $interfaceAddress" }
            return true
        }
        return false
    }

    private fun InetAddress.isInvalidVersion(): Boolean {
        return if (delegate.address == Address.IP_V4)
            this is Inet6Address
        else
            this is Inet4Address
    }

    private fun InetAddress.isInvalidSegment(interfaceAddress: InterfaceAddress): Boolean {
        val a = interfaceAddress.address.address
        val b = address
        val pref = interfaceAddress.networkPrefixLength.toInt()
        val bytes = pref / 8
        for (i in 0 until bytes) {
            if (a[i] != b[i]) {
                return true
            }
        }
        val bits = pref % 8
        if (bits != 0) {
            val mask = (0xff shl 8 - bits) and 0xff
            return (a[bytes].toInt() and mask) != (b[bytes].toInt() and mask)
        }
        return false
    }
}
