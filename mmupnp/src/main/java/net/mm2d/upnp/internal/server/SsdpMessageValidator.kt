/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import net.mm2d.log.Logger
import net.mm2d.upnp.common.Http
import net.mm2d.upnp.common.SsdpMessage
import java.io.IOException
import java.net.InetAddress
import java.net.URL

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
    if (!Http.isHttpUrl(location)) return false
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
