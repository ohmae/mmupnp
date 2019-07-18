/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 */

package net.mm2d.upnp

import net.mm2d.upnp.util.NetworkUtils
import java.net.NetworkInterface

/**
 * Enum to specify the protocol stack to use.
 *
 * @author [大前良介(OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
enum class Protocol {
    /**
     * Use only IPv4.
     */
    IP_V4_ONLY {
        override fun getAvailableInterfaces(): List<NetworkInterface> = NetworkUtils.getAvailableInet4Interfaces()
    },
    /**
     * Use only IPv6.
     *
     * Since UPnP support for IPv4 is a MUST, it is used only for verification.
     *
     * Only link local addresses are used as an IPv6 address system.
     * It does not support multicast on site local addresses.
     */
    IP_V6_ONLY {
        override fun getAvailableInterfaces(): List<NetworkInterface> = NetworkUtils.getAvailableInet6Interfaces()
    },
    /**
     * Use dual stack of IPv4 / IPv6.
     *
     * Prioritizes IPv4 over IPv6,
     * but prefers IPv6 if the IPv4 address is a link local address (assigned by APIPA).
     *
     * Only link local addresses are used as an IPv6 address system.
     * It does not support multicast on site local addresses.
     */
    DUAL_STACK {
        override fun getAvailableInterfaces(): List<NetworkInterface> = NetworkUtils.getAvailableInterfaces()
    };

    /**
     * Returns the NetworkInterface available for that protocol stack.
     */
    internal abstract fun getAvailableInterfaces(): List<NetworkInterface>

    companion object {
        /**
         * Default protocol stack.
         */
        @JvmField
        val DEFAULT = DUAL_STACK
    }
}
