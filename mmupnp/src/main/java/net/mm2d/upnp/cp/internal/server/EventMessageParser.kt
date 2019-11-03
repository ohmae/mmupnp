/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.server

import net.mm2d.upnp.common.util.XmlUtils
import net.mm2d.upnp.common.util.siblingElements

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

