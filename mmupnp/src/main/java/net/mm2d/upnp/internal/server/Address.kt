/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import net.mm2d.upnp.common.util.toAddressString
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * Multicast address
 */
internal enum class Address(
    ssdpAddress: String,
    eventAddress: String
) {
    /**
     * Multicast address for IPv4
     */
    IP_V4(
        ServerConst.SSDP_ADDRESS_V4,
        ServerConst.EVENT_ADDRESS_V4
    ),
    /**
     * Multicast address for IPv6 (link local)
     */
    IP_V6(
        ServerConst.SSDP_ADDRESS_V6,
        ServerConst.EVENT_ADDRESS_V6
    ),
    ;

    val ssdpInetAddress: InetAddress = InetAddress.getByName(ssdpAddress)
    val ssdpSocketAddress: InetSocketAddress = InetSocketAddress(ssdpInetAddress, ServerConst.SSDP_PORT)
    val ssdpAddressString: String = ssdpSocketAddress.toAddressString()
    val eventInetAddress: InetAddress = InetAddress.getByName(eventAddress)
}
