/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.parser

import net.mm2d.upnp.common.Http
import net.mm2d.upnp.common.HttpMessage
import net.mm2d.upnp.util.XmlUtils
import net.mm2d.upnp.util.siblingElements
import java.util.*

internal fun String?.parseEventXml(): List<Pair<String, String>> {
    if (this.isNullOrEmpty()) {
        return emptyList()
    }
    try {
        val propertySetNode = XmlUtils.newDocument(true, this).documentElement
        if (propertySetNode.localName != "propertyset") {
            return emptyList()
        }
        val list = mutableListOf<Pair<String, String>>()
        val firstChild = propertySetNode.firstChild ?: return list
        firstChild.siblingElements()
            .filter { it.localName == "property" }
            .flatMap { it.firstChild?.siblingElements() ?: emptyList() }
            .forEach {
                val name = it.localName
                if (!name.isNullOrEmpty()) {
                    list.add(name to it.textContent)
                }
            }
        return list
    } catch (ignored: Exception) {
    }
    return emptyList()
}

internal const val DEFAULT_MAX_AGE = 1800

internal fun HttpMessage.parseCacheControl(): Int {
    val age = getHeader(Http.CACHE_CONTROL)?.toLowerCase(Locale.US)
    if (age?.startsWith("max-age") != true) {
        return DEFAULT_MAX_AGE
    }
    return age.substringAfter('=', "").toIntOrNull() ?: DEFAULT_MAX_AGE
}

internal fun HttpMessage.parseUsn(): Pair<String, String> {
    val usn = getHeader(Http.USN)
    if (usn.isNullOrEmpty() || !usn.startsWith("uuid")) {
        return "" to ""
    }
    val pos = usn.indexOf("::")
    return if (pos < 0) usn to ""
    else usn.substring(0, pos) to usn.substring(pos + 2)
}
