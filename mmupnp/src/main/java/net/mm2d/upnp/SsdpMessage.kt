/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import java.io.IOException
import java.io.OutputStream
import java.net.InetAddress

/**
 * Interface of SSDP(Simple Service Discovery Protocol) message.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface SsdpMessage {
    /**
     * Return UUID described in USN.
     *
     * @return UUID
     */
    val uuid: String

    /**
     * Return Type described in USN.
     *
     * @return Type
     */
    val type: String

    /**
     * Return the value of NTS field.
     *
     * @return value of NTF field
     */
    val nts: String?

    /**
     * Return the value of max-age.
     *
     * @return value of max-age
     */
    val maxAge: Int

    /**
     * Returns the time when the expiration limit will expire.
     *
     * The time when max-age is added to the reception time.
     *
     * @return time of expire
     */
    val expireTime: Long

    /**
     * Return the value of Location.
     *
     * @return Location
     */
    val location: String?

    /**
     * Returns the address of the interface that received this packet.
     *
     * @return the address of the interface that received this packet
     */
    val localAddress: InetAddress?

    /**
     * Returns the ScopeID of the interface that received this packet.
     *
     * @return ScopeID, 0 if not set (including IPv4)
     */
    val scopeId: Int

    /**
     * Returns whether this is a message for pinned.
     *
     * @return true this is a message for pinned.
     */
    val isPinned: Boolean

    /**
     * Returns the value of header
     *
     * @param name header name
     * @return value
     */
    fun getHeader(name: String): String?

    /**
     * Set the value of header
     *
     * @param name  header name
     * @param value value
     */
    fun setHeader(name: String, value: String)

    /**
     * Write the message to the OutputStream.
     *
     * @param os OutputStream
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    fun writeData(os: OutputStream)

    companion object {
        /**
         * Request method of M-SEARCH
         */
        const val M_SEARCH = "M-SEARCH"
        /**
         * Request method of NOTIFY
         */
        const val NOTIFY = "NOTIFY"
        /**
         * NTS: ssdp:alive
         */
        const val SSDP_ALIVE = "ssdp:alive"
        /**
         * NTS: ssdp:byebye
         */
        const val SSDP_BYEBYE = "ssdp:byebye"
        /**
         * NTS: ssdp:update
         */
        const val SSDP_UPDATE = "ssdp:update"
        /**
         * MAN: ssdp:discover
         */
        const val SSDP_DISCOVER = "\"ssdp:discover\""
    }
}
