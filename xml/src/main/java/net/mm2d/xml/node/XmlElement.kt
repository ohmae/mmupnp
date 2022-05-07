/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 *  http://opensource.org/licenses/MIT
 */

package net.mm2d.xml.node

class XmlElement(
    val uri: String,
    val qName: String,
    val value: String,
    val children: List<XmlNode> = emptyList(),
    val attributes: List<XmlAttribute> = emptyList(),
) : XmlNode {
    override var parent: XmlElement? = null
    val prefix: String
    val localName: String
    val prefixMap: Map<String, String>
    val childElements: List<XmlElement> =
        children.mapNotNull { it as? XmlElement }

    init {
        val index = qName.indexOf(":")
        if (index == -1) {
            prefix = ""
            localName = qName
        } else {
            prefix = qName.substring(0, index)
            localName = qName.substring(index + 1, qName.length)
        }
        val map = mutableMapOf<String, String>()
        attributes.forEach {
            if (it.qName == "xmlns") {
                map[""] = it.value
            }
            if (it.prefix == "xmlns") {
                map[it.localName] = it.value
            }
        }
        prefixMap = map.toMap()
        children.forEach { it.parent = this }
        attributes.forEach { it.parent = this }
    }

    fun getAttribute(qName: String): XmlAttribute? =
        attributes.find { it.qName == qName }

    fun getAttributeValue(qName: String): String =
        getAttribute(qName)?.value ?: ""

    fun getAttributeNS(uri: String, localName: String): XmlAttribute? =
        attributes.find { it.uri == uri && it.localName == localName }

    fun getAttributeNSValue(uri: String, localName: String): String =
        getAttributeNS(uri, localName)?.value ?: ""

    fun buildString(indent: Boolean = false, withDeclaration: Boolean = false): String =
        buildString {
            if (withDeclaration) {
                append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                if (indent) newLine()
            }
            toXmlString(indent, this, 0)
        }

    override fun toXmlString(indent: Boolean, sb: StringBuilder, depth: Int) {
        sb.apply {
            if (indent) indent(depth)
            append("<")
            append(qName)
            attributes.forEach { it.toXmlString(indent, this, depth) }
            if (children.isEmpty()) {
                append("/>")
                if (indent) newLine()
            } else {
                append(">")
                if (indent && (children.size != 1 || children[0] !is XmlTextNode)) {
                    newLine()
                }
                children.forEach { it.toXmlString(indent, this, depth + 1) }
                if (indent && (children.size != 1 || children[0] !is XmlTextNode)) {
                    indent(depth)
                }
                append("</")
                append(qName)
                append(">")
                if (indent) newLine()
            }
        }
    }

    private fun StringBuilder.indent(depth: Int) {
        repeat(depth) { append("    ") }
    }

    private fun StringBuilder.newLine() {
        append("\n")
    }
}
