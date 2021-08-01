/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 *  http://opensource.org/licenses/MIT
 */

package net.mm2d.xml.node

class XmlAttribute(
    val uri: String,
    val qName: String,
    val value: String
) : XmlNode {
    override var parent: XmlElement? = null
    val prefix: String
    val localName: String

    init {
        val index = qName.indexOf(":")
        if (index == -1) {
            prefix = ""
            localName = qName
        } else {
            prefix = qName.substring(0, index)
            localName = qName.substring(index + 1, qName.length)
        }
    }

    override fun toXmlString(indent: Boolean, sb: StringBuilder, depth: Int) {
        sb.append(" ")
        sb.append(qName)
        sb.append("=\"")
        sb.append(escapeXmlString(value, true))
        sb.append("\"")
    }
}
