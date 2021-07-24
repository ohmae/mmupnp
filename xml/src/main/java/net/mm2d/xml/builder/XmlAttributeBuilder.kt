package net.mm2d.xml.builder

import net.mm2d.xml.node.XmlAttribute

class XmlAttributeBuilder(
    val qName: String,
    val value: String
) : XmlNodeBuilder {
    override var parent: XmlElementBuilder? = null
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

    override fun build(removeIgnorableWhitespace: Boolean): XmlAttribute =
        XmlAttribute(
            findUri(),
            qName,
            value
        )

    fun setUri(uri: String) {
        val existingUri = findUri()
        if (existingUri == uri) return
        val parent = parent ?: throw IllegalStateException()
        if (existingUri.isEmpty()) {
            parent.findRoot(parent).appendNs(prefix, uri)
        } else {
            parent.appendNs(prefix, uri)
        }
    }

    private fun findUri(): String = parent?.let { findUri(prefix, it) } ?: ""
}
