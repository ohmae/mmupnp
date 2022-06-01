/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.parser

import net.mm2d.upnp.Http
import net.mm2d.upnp.SingleHttpMessage
import net.mm2d.xml.parser.XmlParser
import java.util.*

internal fun String?.parseEventXml(): List<Pair<String, String>> {
    if (this.isNullOrEmpty()) {
        return emptyList()
    }
    try {
        val propertySetNode = XmlParser.parse(this)
        if (propertySetNode?.localName != "propertyset") {
            return emptyList()
        }
        return propertySetNode.childElements
            .filter { it.localName == "property" }
            .flatMap { it.childElements }
            .map { it.localName to it.value }
    } catch (ignored: Exception) {
    }
    return emptyList()
}

internal const val DEFAULT_MAX_AGE = 1800

internal fun SingleHttpMessage.parseCacheControl(): Int {
    val age = getHeader(Http.CACHE_CONTROL)?.lowercase(Locale.US)
    if (age?.startsWith("max-age") != true) {
        return DEFAULT_MAX_AGE
    }
    return age.substringAfter('=', "").toIntOrNull() ?: DEFAULT_MAX_AGE
}

internal fun SingleHttpMessage.parseUsn(): Pair<String, String> {
    val usn = getHeader(Http.USN)
    if (usn.isNullOrEmpty() || !usn.startsWith("uuid")) {
        return "" to ""
    }
    val pos = usn.indexOf("::")
    return if (pos < 0) usn to ""
    else usn.substring(0, pos) to usn.substring(pos + 2)
}
