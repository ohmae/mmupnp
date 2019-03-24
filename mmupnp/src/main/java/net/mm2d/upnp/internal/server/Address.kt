/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import net.mm2d.upnp.util.toAddressString
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * マルチキャストアドレス。
 */
internal enum class Address(address: String) {
    /**
     * IPv4用マルチキャストアドレス
     */
    IP_V4("239.255.255.250"),
    /**
     * IPv6用リンクローカルマルチキャストアドレス
     */
    IP_V6_LINK_LOCAL("FF02::C"),
    /**
     * IPv6用サイトローカルマルチキャストアドレス
     *
     * 未使用
     */
    IP_V6_SITE_LOCAL("FF05::C");

    val inetAddress: InetAddress = InetAddress.getByName(address)
    val socketAddress: InetSocketAddress = InetSocketAddress(inetAddress, SsdpServer.SSDP_PORT)
    val addressString: String = socketAddress.toAddressString()
}
