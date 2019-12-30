/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da.internal.impl

import net.mm2d.upnp.common.util.append
import org.w3c.dom.DOMException
import org.w3c.dom.Element
import org.w3c.dom.Node

interface XmlAppendable {
    fun appendTo(parent: Element)
}

@Throws(DOMException::class)
fun Node.append(list: List<XmlAppendable>, listTag: String) {
    if (list.isEmpty()) return
    append(listTag) { list.forEach { it.appendTo(this) } }
}

@Throws(DOMException::class)
fun Node.append(list: List<String>, listTag: String, tag: String) {
    if (list.isEmpty()) return
    append(listTag) { list.forEach { append(tag, it) } }
}
