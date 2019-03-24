/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import net.mm2d.log.Logger
import net.mm2d.upnp.Http
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.internal.message.SsdpRequest
import net.mm2d.upnp.internal.message.SsdpResponse
import net.mm2d.upnp.internal.thread.TaskExecutors

import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * SSDP M-SEARCHとそのレスポンス受信を行うクラス。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SsdpSearchServer(
        private val delegate: SsdpServerDelegate
) : SsdpServer by delegate {
    private var listener: ((SsdpMessage) -> Unit)? = null

    constructor(
            taskExecutors: TaskExecutors,
            address: Address,
            ni: NetworkInterface
    ) : this(SsdpServerDelegate(taskExecutors, address, ni)) {
        delegate.setReceiver { sourceAddress, data, length -> onReceive(sourceAddress, data, length) }
    }

    /**
     * レスポンス受信リスナーを登録する。
     *
     * @param listener リスナー
     */
    fun setResponseListener(listener: ((SsdpMessage) -> Unit)?) {
        this.listener = listener
    }

    /**
     * M-SEARCHを実行する。
     *
     * @param st STの値
     */
    fun search(st: String? = null) {
        send { makeSearchMessage(if (st.isNullOrEmpty()) ST_ALL else st) }
    }

    private fun makeSearchMessage(st: String): SsdpRequest {
        return SsdpRequest.create().also {
            it.method = SsdpMessage.M_SEARCH
            it.uri = "*"
            it.setHeader(Http.HOST, delegate.getSsdpAddressString())
            it.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER)
            it.setHeader(Http.MX, "1")
            it.setHeader(Http.ST, st)
        }
    }

    // VisibleForTesting
    internal fun onReceive(sourceAddress: InetAddress, data: ByteArray, length: Int) {
        try {
            val message = SsdpResponse.create(delegate.getLocalAddress(), data, length)
            Logger.v {
                "receive ssdp search response from $sourceAddress to ${delegate.getLocalAddress()}:\n$message"
            }
            if (SsdpServerDelegate.isInvalidLocation(message, sourceAddress)) {
                Logger.e { "isInvalidLocation:${message.location} $sourceAddress" }
                return
            }
            listener?.invoke(message)
        } catch (ignored: IOException) {
        }
    }

    companion object {
        /**
         * ST(SearchType) 全機器。
         */
        const val ST_ALL = "ssdp:all"
        /**
         * ST(SearchType) rootdevice。
         */
        const val ST_ROOTDEVICE = "upnp:rootdevice"
    }
}
