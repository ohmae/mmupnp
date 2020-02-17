/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.server

import net.mm2d.log.Logger
import net.mm2d.upnp.common.Http
import net.mm2d.upnp.common.ServerConst
import net.mm2d.upnp.common.SsdpMessage
import net.mm2d.upnp.common.internal.message.SsdpRequest
import net.mm2d.upnp.common.internal.server.*
import net.mm2d.upnp.common.internal.thread.TaskExecutors
import java.io.IOException
import java.net.InetAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface

/**
 * Receiver for SSDP NOTIFY
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SsdpNotifyReceiver(
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
        if (sourceAddress.isInvalidAddress(delegate.address, interfaceAddress, segmentCheckEnabled)) {
            return
        }
        try {
            val message = createSsdpRequestMessage(data, length)
            Logger.v { "receive ssdp multicast message from $sourceAddress in ${delegate.getLocalAddress()}:\n$message" }

            if (message.shouldNotAccept()) return
            // receive only Notify method
            if (message.getMethod() != Http.NOTIFY) return
            if (message.isNotUpnp()) return
            // ByeBye accepts it regardless of address problems because it does not communicate
            if (message.nts != SsdpMessage.SSDP_BYEBYE && message.hasInvalidLocation(sourceAddress)) return

            notifyListener?.invoke(message)
        } catch (ignored: IOException) {
        }
    }

    @Throws(IOException::class)
    fun createSsdpRequestMessage(data: ByteArray, length: Int): SsdpRequest =
        SsdpRequest.create(delegate.getLocalAddress(), data, length)
}
