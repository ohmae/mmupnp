/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty

import net.mm2d.upnp.SsdpMessage
import java.io.IOException
import java.io.OutputStream
import java.net.InetAddress

/**
 * Empty implementation of [SsdpMessage].
 */
object EmptySsdpMessage : SsdpMessage {
    override val uuid: String = ""
    override val type: String = ""
    override val nts: String? = null
    override val maxAge: Int = 0
    override val expireTime: Long = 0L
    override val location: String? = null
    override val localAddress: InetAddress? = null
    override val scopeId: Int = 0
    override val isPinned: Boolean = false

    override fun getHeader(name: String): String? = null
    override fun setHeader(name: String, value: String) = Unit

    @Throws(IOException::class)
    override fun writeData(os: OutputStream) {
        throw IOException("empty object")
    }
}
