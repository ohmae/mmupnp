/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.server

import net.mm2d.log.Logger
import net.mm2d.upnp.common.SsdpMessage
import net.mm2d.upnp.common.isHttpUrl
import java.io.IOException
import java.net.*

internal val DEFAULT_SSDP_MESSAGE_FILTER: (SsdpMessage) -> Boolean = { true }
/**
 * A normal URL is described in the Location of SsdpMessage,
 * and it is checked whether there is a mismatch between the description address and the packet source address.
 *
 * @receiver SsdpMessage to check
 * @param sourceAddress source address
 * @return true: if there is an invalid Location, such as a mismatch with the sender. false: otherwise
 */
internal fun SsdpMessage.hasInvalidLocation(sourceAddress: InetAddress): Boolean =
    (!hasValidLocation(sourceAddress)).also {
        if (it) Logger.w { "Location: $location is invalid from $sourceAddress" }
    }

private fun SsdpMessage.hasValidLocation(sourceAddress: InetAddress): Boolean {
    val location = location ?: return false
    if (!location.isHttpUrl()) return false
    try {
        return sourceAddress == InetAddress.getByName(URL(location).host)
    } catch (ignored: IOException) {
    }
    return false
}

internal fun SsdpMessage.isNotUpnp(): Boolean {
    if (getHeader("X-TelepathyAddress.sony.com") != null) {
        // Sony telepathy service(urn:schemas-sony-com:service:X_Telepathy:1) send ssdp packet,
        // but it's location address refuses connection. Since this is not correct as UPnP, ignore it.
        Logger.v("ignore sony telepathy service")
        return true
    }
    return false
}

internal fun InetAddress.isInvalidAddress(address: Address, interfaceAddress: InterfaceAddress, checkSegment: Boolean): Boolean {
    if (isInvalidVersion(address)) {
        Logger.w { "IP version mismatch:$this $interfaceAddress" }
        return true
    }
    // Even if the address setting is incorrect, multicast packets can be sent.
    // Since the segment information is incorrect and packets from parties
    // that can not be exchanged except for multicast are useless even if received, they are discarded.
    if (checkSegment &&
        address == Address.IP_V4 &&
        isInvalidSegment(interfaceAddress)
    ) {
        Logger.w { "Invalid segment:$this $interfaceAddress" }
        return true
    }
    return false
}

private fun InetAddress.isInvalidVersion(address: Address): Boolean {
    return if (address == Address.IP_V4)
        this is Inet6Address
    else
        this is Inet4Address
}

private fun InetAddress.isInvalidSegment(interfaceAddress: InterfaceAddress): Boolean {
    val a = interfaceAddress.address.address
    val b = address
    val pref = interfaceAddress.networkPrefixLength.toInt()
    val bytes = pref / 8
    for (i in 0 until bytes) {
        if (a[i] != b[i]) {
            return true
        }
    }
    val bits = pref % 8
    if (bits != 0) {
        val mask = (0xff shl 8 - bits) and 0xff
        return (a[bytes].toInt() and mask) != (b[bytes].toInt() and mask)
    }
    return false
}
