/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

internal object ServerConst {
    /**
     * SSDP Multicast Address IPv4
     */
    const val SSDP_ADDRESS_V4 = "239.255.255.250"
    /**
     * SSDP Multicast Address IPv6
     */
    const val SSDP_ADDRESS_V6 = "FF02::C"
    /**
     * Port number used for SSDP.
     */
    const val SSDP_PORT = 1900
}
