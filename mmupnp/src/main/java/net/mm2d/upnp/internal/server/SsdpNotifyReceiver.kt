/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import net.mm2d.log.Logger
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.internal.message.SsdpRequest
import net.mm2d.upnp.internal.thread.TaskExecutors
import java.io.IOException
import java.net.*

/**
 * SSDP NOTIFYを受信するクラス
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SsdpNotifyReceiver(
        private val delegate: SsdpServerDelegate
) : SsdpServer by delegate {
    private var listener: ((SsdpMessage) -> Unit)? = null
    private var segmentCheckEnabled: Boolean = false
    // VisibleForTesting
    internal val interfaceAddress: InterfaceAddress
        get() = delegate.interfaceAddress

    /**
     * インスタンス作成。
     *
     * @param ni 使用するインターフェース
     */
    constructor(
            taskExecutors: TaskExecutors,
            address: Address,
            ni: NetworkInterface
    ) : this(SsdpServerDelegate(taskExecutors, address, ni, SsdpServer.SSDP_PORT)) {
        delegate.setReceiver { sourceAddress, data, length ->
            onReceive(sourceAddress, data, length)
        }
    }

    fun setSegmentCheckEnabled(enabled: Boolean) {
        segmentCheckEnabled = enabled
    }

    /**
     * NOTIFY受信リスナーを登録する。
     *
     * @param listener リスナー
     */
    fun setNotifyListener(listener: ((SsdpMessage) -> Unit)?) {
        this.listener = listener
    }

    // VisibleForTesting
    internal fun onReceive(sourceAddress: InetAddress, data: ByteArray, length: Int) {
        if (invalidAddress(sourceAddress)) {
            return
        }
        try {
            val message = createSsdpRequestMessage(data, length)
            // M-SEARCHパケットは無視する
            if (message.method == SsdpMessage.M_SEARCH) {
                return
            }
            Logger.v {
                "receive ssdp notify from $sourceAddress in ${delegate.getLocalAddress()}:\n$message"
            }
            // ByeByeは通信を行わないためアドレスの問題有無にかかわらず受け入れる
            if (message.nts != SsdpMessage.SSDP_BYEBYE && SsdpServerDelegate.isInvalidLocation(message, sourceAddress)) {
                return
            }
            listener?.invoke(message)
        } catch (ignored: IOException) {
        }
    }

    @Throws(IOException::class)
    fun createSsdpRequestMessage(data: ByteArray, length: Int): SsdpRequest {
        return SsdpRequest.create(delegate.getLocalAddress(), data, length)
    }

    // VisibleForTesting
    internal fun invalidAddress(sourceAddress: InetAddress): Boolean {
        if (invalidVersion(sourceAddress)) {
            Logger.w { "IP version mismatch:$sourceAddress $interfaceAddress" }
            return true
        }
        // アドレス設定が間違っている場合でもマルチキャストパケットの送信はできてしまう。
        // セグメント情報が間違っており、マルチキャスト以外のやり取りができない相手からのパケットは
        // 受け取っても無駄なので破棄する。
        if (segmentCheckEnabled
                && delegate.address == Address.IP_V4
                && invalidSegment(interfaceAddress, sourceAddress)) {
            Logger.w { "Invalid segment:$sourceAddress $interfaceAddress" }
            return true
        }
        return false
    }

    private fun invalidVersion(sourceAddress: InetAddress): Boolean {
        return if (delegate.address == Address.IP_V4) {
            sourceAddress is Inet6Address
        } else sourceAddress is Inet4Address
    }

    private fun invalidSegment(interfaceAddress: InterfaceAddress, sourceAddress: InetAddress): Boolean {
        val a = interfaceAddress.address.address
        val b = sourceAddress.address
        val pref = interfaceAddress.networkPrefixLength.toInt()
        val bytes = pref / 8
        val bits = pref % 8
        for (i in 0 until bytes) {
            if (a[i] != b[i]) {
                return true
            }
        }
        if (bits != 0) {
            val mask = (0xff shl 8 - bits) and 0xff
            return (a[bytes].toInt() and mask) != (b[bytes].toInt() and mask)
        }
        return false
    }
}
