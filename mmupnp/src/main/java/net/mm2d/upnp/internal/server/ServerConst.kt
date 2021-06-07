/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

internal object ServerConst {
    /**
     * IPv4 Address for SSDP
     */
    const val SSDP_ADDRESS_V4 = "239.255.255.250"

    /**
     * IPv6 Address for SSDP
     */
    const val SSDP_ADDRESS_V6 = "FF02::C"

    /**
     * Port number for SSDP.
     */
    const val SSDP_PORT = 1900

    /**
     * IPv4 Address for Multicast Eventing
     */
    const val EVENT_ADDRESS_V4 = "239.255.255.246"

    /**
     * IPv6 Address for Multicast Eventing
     */
    const val EVENT_ADDRESS_V6 = "FF02::130"

    /**
     * Port number for Multicast Eventing
     */
    const val EVENT_PORT = 7900
}
