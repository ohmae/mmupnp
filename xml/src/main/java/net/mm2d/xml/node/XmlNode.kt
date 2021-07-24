package net.mm2d.xml.node

interface XmlNode {
    var parent: XmlElement?
    fun toXmlString(indent: Boolean, sb: StringBuilder, depth: Int)
}
