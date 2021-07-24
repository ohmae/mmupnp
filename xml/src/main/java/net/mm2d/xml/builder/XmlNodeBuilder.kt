package net.mm2d.xml.builder

import net.mm2d.xml.node.XmlNode

interface XmlNodeBuilder {
    var parent: XmlElementBuilder?
    fun build(removeIgnorableWhitespace: Boolean = true): XmlNode
}

