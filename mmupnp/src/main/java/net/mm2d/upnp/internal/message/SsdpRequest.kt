/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message

import net.mm2d.upnp.SingleHttpRequest
import net.mm2d.upnp.SsdpMessage
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetAddress

/**
 * SSDP Request message
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SsdpRequest(
    val message: SingleHttpRequest,
    private val delegate: SsdpMessageDelegate
) : SsdpMessage by delegate {
    fun getMethod(): String = message.getMethod()
    fun setMethod(method: String): Unit = message.setMethod(method)
    fun getUri(): String = message.getUri()
    fun setUri(uri: String): Unit = message.setUri(uri)
    fun updateLocation(): Unit = delegate.updateLocation()
    override fun toString(): String = delegate.toString()

    companion object {
        fun create(): SsdpRequest = SingleHttpRequest.create().let {
            SsdpRequest(it, SsdpMessageDelegate(it))
        }

        @Throws(IOException::class)
        fun create(address: InetAddress, data: ByteArray, length: Int): SsdpRequest =
            SingleHttpRequest.create().apply {
                readData(ByteArrayInputStream(data, 0, length))
            }.let { SsdpRequest(it, SsdpMessageDelegate(it, address)) }
    }
}
