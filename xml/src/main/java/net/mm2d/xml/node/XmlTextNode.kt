package net.mm2d.xml.node

class XmlTextNode(
    val value: String
) : XmlNode {
    override var parent: XmlElement? = null
    override fun toXmlString(indent: Boolean, sb: StringBuilder, depth: Int) {
        sb.append(escapeXmlString(value))
    }
}