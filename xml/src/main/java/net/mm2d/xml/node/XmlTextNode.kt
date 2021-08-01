/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 *  http://opensource.org/licenses/MIT
 */

package net.mm2d.xml.node

class XmlTextNode(
    val value: String
) : XmlNode {
    override var parent: XmlElement? = null
    override fun toXmlString(indent: Boolean, sb: StringBuilder, depth: Int) {
        sb.append(escapeXmlString(value))
    }
}
