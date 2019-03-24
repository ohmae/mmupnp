/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message

import net.mm2d.upnp.SsdpMessage

import java.io.OutputStream
import java.net.InetAddress

/**
 * 固定デバイス用の[SsdpMessage]
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class FakeSsdpMessage(
    override val location: String,
    private var _uuid: String = "",
    override val isPinned: Boolean = true
) : SsdpMessage {
    override var localAddress: InetAddress? = null
    override val scopeId: Int = 0
    override val type: String = ""
    override val nts: String? = SsdpMessage.SSDP_ALIVE
    override val maxAge: Int = Integer.MAX_VALUE
    override val expireTime: Long = java.lang.Long.MAX_VALUE
    override var uuid: String
        get() = _uuid
        internal set(value) {
            _uuid = value
        }

    override fun getHeader(name: String): String? = null
    override fun setHeader(name: String, value: String) {}
    override fun writeData(os: OutputStream) {}
}
