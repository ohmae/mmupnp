/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import net.mm2d.upnp.common.Protocol
import net.mm2d.upnp.common.util.isAvailableInet4Interface
import net.mm2d.upnp.common.util.isAvailableInet6Interface
import java.net.NetworkInterface

internal fun <T : Any> Iterable<NetworkInterface>.createServerList(
    protocol: Protocol,
    createV4: (NetworkInterface) -> T?,
    createV6: (NetworkInterface) -> T?
): List<T> = when (protocol) {
    Protocol.IP_V4_ONLY -> filter { it.isAvailableInet4Interface() }.mapNotNull { createV4(it) }
    Protocol.IP_V6_ONLY -> filter { it.isAvailableInet6Interface() }.mapNotNull { createV6(it) }
    Protocol.DUAL_STACK -> mutableListOf<T>().also { list ->
        forEach { nif ->
            if (nif.isAvailableInet4Interface()) createV4(nif)?.let { list.add(it) }
            if (nif.isAvailableInet6Interface()) createV6(nif)?.let { list.add(it) }
        }
    }
}
