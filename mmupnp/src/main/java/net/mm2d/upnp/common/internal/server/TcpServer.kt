/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.server

import java.net.InetAddress

internal interface TcpServer : Server {
    fun getLocalPort(): Int
    fun getInetAddress(): InetAddress?
}
