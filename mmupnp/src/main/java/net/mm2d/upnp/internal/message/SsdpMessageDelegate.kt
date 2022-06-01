/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message

import net.mm2d.upnp.Http
import net.mm2d.upnp.SingleHttpMessage
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.internal.parser.parseCacheControl
import net.mm2d.upnp.internal.parser.parseUsn
import java.io.IOException
import java.io.OutputStream
import java.net.Inet6Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Common implementation of [SsdpMessage].
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SsdpMessageDelegate(
    val message: SingleHttpMessage,
    override val localAddress: InetAddress? = null
) : SsdpMessage {
    override val maxAge: Int
    override val expireTime: Long
    override val uuid: String
    override val type: String
    override val nts: String?
    override var location: String? = null
        private set
    override val scopeId: Int
        get() = (localAddress as? Inet6Address)?.scopeId ?: 0
    override val isPinned: Boolean = false

    init {
        if (localAddress == null) {
            maxAge = 0
            expireTime = 0
            uuid = ""
            type = ""
            nts = ""
            location = null
        } else {
            maxAge = message.parseCacheControl()
            expireTime = TimeUnit.SECONDS.toMillis(maxAge.toLong()) + System.currentTimeMillis()
            val (uuid, type) = message.parseUsn()
            this.uuid = uuid
            this.type = type
            nts = message.getHeader(Http.NTS)
            location = message.getHeader(Http.LOCATION)
        }
    }

    fun updateLocation() {
        location = message.getHeader(Http.LOCATION)
    }

    override fun getHeader(name: String): String? = message.getHeader(name)
    override fun setHeader(name: String, value: String): Unit = message.setHeader(name, value)

    @Throws(IOException::class)
    override fun writeData(os: OutputStream): Unit = message.writeData(os)

    override fun toString(): String = message.toString()
}
