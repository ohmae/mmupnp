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

    fun buildXml(indent: Boolean = false): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        toXmlString(indent, sb, 0)
        return sb.toString()
    }

    override fun toXmlString(indent: Boolean, sb: StringBuilder, depth: Int) {
        if (indent) sb.indent(depth)
        sb.append("<")
        sb.append(qName)
        attributes.forEach { it.toXmlString(indent, sb, depth) }
        if (children.isEmpty()) {
            sb.append("/>")
        } else {
            sb.append(">")
            children.forEach { it.toXmlString(indent, sb, depth + 1) }
            if (indent && (children.size != 1 || children[0] !is XmlTextNode)) sb.indent(depth)
            sb.append("</")
            sb.append(qName)
            sb.append(">")
        }
    }

    private fun StringBuilder.indent(depth: Int) {
        append("\n")
        repeat(depth) { append("\t") }
    }
}