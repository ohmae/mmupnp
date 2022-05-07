/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 *  http://opensource.org/licenses/MIT
 */

package net.mm2d.xml.builder

import net.mm2d.xml.node.XmlElement
import net.mm2d.xml.node.XmlTextNode

class XmlElementBuilder(
    val qName: String = ""
) : XmlNodeBuilder {
    private var _prefixMap: Map<String, String>? = null
    private val _attributes: MutableList<XmlAttributeBuilder> = mutableListOf()
    private val _children: MutableList<XmlNodeBuilder> = mutableListOf()
    override var parent: XmlElementBuilder? = null
    val prefix: String
    val localName: String

    val attributes: List<XmlAttributeBuilder>
        get() = _attributes

    val children: List<XmlNodeBuilder>
        get() = _children

    val prefixMap: Map<String, String>
        get() = _prefixMap ?: makePrefixMap()

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

    private fun makePrefixMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        _attributes.forEach {
            if (it.qName == "xmlns") {
                map[""] = it.value
            }
            if (it.prefix == "xmlns") {
                map[it.localName] = it.value
            }
        }
        return map.toMap().also {
            _prefixMap = it
        }
    }

    fun appendChild(node: XmlNodeBuilder) {
        node.parent = this
        _children.add(node)
    }

    fun appendTextNode(text: String) {
        val lastChild = _children.lastOrNull()
        if (lastChild is XmlTextNodeBuilder) {
            lastChild.appendText(text)
        } else {
            appendChild(XmlTextNodeBuilder(text))
        }
    }

    fun appendAttribute(attribute: XmlAttributeBuilder) {
        attribute.parent = this
        _attributes.add(attribute)
        _prefixMap = null
    }

    fun setUri(uri: String) {
        val current = findUri()
        if (current == uri) return
        if (current.isEmpty()) {
            findRoot(this).appendNs(prefix, uri)
        } else {
            appendNs(prefix, uri)
        }
    }

    fun appendNs(prefix: String, uri: String) {
        val attribute = XmlAttributeBuilder.createNs(prefix, uri)
        _attributes.removeIf { it.qName == attribute.qName }
        appendAttribute(attribute)
    }

    fun findRoot(element: XmlElementBuilder): XmlElementBuilder =
        element.parent.let { if (it == null || it.qName.isEmpty()) element else findRoot(it) }

    private fun findUri(): String = findUri(prefix, this)

    override fun build(removeIgnorableWhitespace: Boolean): XmlElement {
        val predicate: (XmlNodeBuilder) -> Boolean =
            if (removeIgnorableWhitespace && children.size > 1) {
                { it !is XmlTextNodeBuilder || it.value.isNotBlank() }
            } else {
                { true }
            }
        val childList = children.filter(predicate).map { it.build(removeIgnorableWhitespace) }
        val value = if (childList.size == 1) {
            childList[0].let { if (it is XmlTextNode) it.value else "" }
        } else ""
        val attributeList = attributes.sortedBy { it.preferNsValue() }.map { it.build() }
        return XmlElement(
            findUri(),
            qName,
            value,
            childList,
            attributeList
        )
    }
}
