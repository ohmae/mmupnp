/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message

import net.mm2d.upnp.Http.Status
import net.mm2d.upnp.SingleHttpResponse
import net.mm2d.upnp.SsdpMessage
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetAddress

/**
 * SSDP Response message.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SsdpResponse(
// VisibleForTesting
    internal val message: SingleHttpResponse,
    private val delegate: SsdpMessageDelegate
) : SsdpMessage by delegate {
    fun getStatusCode(): Int = message.getStatusCode()
    fun setStatusCode(code: Int): Unit = message.setStatusCode(code)
    fun getReasonPhrase(): String = message.getReasonPhrase()
    fun setReasonPhrase(reasonPhrase: String): Unit = message.setReasonPhrase(reasonPhrase)
    fun getStatus(): Status = message.getStatus()
    fun setStatus(status: Status): Unit = message.setStatus(status)
    override fun toString(): String = delegate.toString()

    companion object {
        @Throws(IOException::class)
        fun create(address: InetAddress, data: ByteArray, length: Int): SsdpResponse =
            SingleHttpResponse.create().apply {
                readData(ByteArrayInputStream(data, 0, length))
            }.let { SsdpResponse(it, SsdpMessageDelegate(it, address)) }
    }
}
