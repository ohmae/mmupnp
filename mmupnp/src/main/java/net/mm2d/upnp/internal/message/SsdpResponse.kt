/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message

import net.mm2d.upnp.Http.Status
import net.mm2d.upnp.HttpResponse
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
    val message: HttpResponse,
    private val delegate: SsdpMessageDelegate
) : SsdpMessage by delegate {
    fun getStatusCode(): Int = message.getStatusCode()

    fun setStatusCode(code: Int) {
        message.setStatusCode(code)
    }

    fun getReasonPhrase(): String = message.getReasonPhrase()

    fun setReasonPhrase(reasonPhrase: String) {
        message.setReasonPhrase(reasonPhrase)
    }

    fun getStatus(): Status = message.getStatus()

    fun setStatus(status: Status) {
        message.setStatus(status)
    }

    override fun toString(): String {
        return delegate.toString()
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun create(address: InetAddress, data: ByteArray, length: Int): SsdpResponse {
            return HttpResponse.create().apply {
                readData(ByteArrayInputStream(data, 0, length))
            }.let { SsdpResponse(it, SsdpMessageDelegate(it, address)) }
        }
    }
}
