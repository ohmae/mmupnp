/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message

import net.mm2d.upnp.Http
import net.mm2d.upnp.HttpMessage
import net.mm2d.upnp.SsdpMessage
import java.io.IOException
import java.io.OutputStream
import java.net.Inet6Address
import java.net.InetAddress
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Common implementation of [SsdpMessage].
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SsdpMessageDelegate(
    val message: HttpMessage,
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
            maxAge = parseCacheControl(message)
            expireTime = TimeUnit.SECONDS.toMillis(maxAge.toLong()) + System.currentTimeMillis()
            val (uuid, type) = parseUsn(message)
            this.uuid = uuid
            this.type = type
            nts = message.getHeader(Http.NTS)
            location = message.getHeader(Http.LOCATION)
        }
    }

    fun updateLocation() {
        location = message.getHeader(Http.LOCATION)
    }

    override fun getHeader(name: String): String? {
        return message.getHeader(name)
    }

    override fun setHeader(name: String, value: String) {
        message.setHeader(name, value)
    }

    @Throws(IOException::class)
    override fun writeData(os: OutputStream) {
        message.writeData(os)
    }

    override fun toString(): String {
        return message.toString()
    }

    companion object {
        private const val DEFAULT_MAX_AGE = 1800

        // VisibleForTesting
        internal fun parseCacheControl(message: HttpMessage): Int {
            val age = message.getHeader(Http.CACHE_CONTROL)?.toLowerCase(Locale.US)
            if (age == null || !age.startsWith("max-age")) {
                return DEFAULT_MAX_AGE
            }
            return age.substringAfter('=', "").toIntOrNull() ?: DEFAULT_MAX_AGE
        }

        // VisibleForTesting
        internal fun parseUsn(message: HttpMessage): Pair<String, String> {
            val usn = message.getHeader(Http.USN)
            if (usn.isNullOrEmpty() || !usn.startsWith("uuid")) {
                return "" to ""
            }
            val pos = usn.indexOf("::")
            return if (pos < 0) usn to ""
            else usn.substring(0, pos) to usn.substring(pos + 2)
        }
    }
}
