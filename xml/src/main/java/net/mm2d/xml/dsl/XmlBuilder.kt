/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 *  http://opensource.org/licenses/MIT
 */

package net.mm2d.xml.dsl

import net.mm2d.xml.builder.XmlAttributeBuilder
import net.mm2d.xml.builder.XmlElementBuilder
import net.mm2d.xml.node.XmlElement

fun buildXml(block: XmlBuilder.() -> Unit): XmlElement {
    val document = XmlElementBuilder()
    XmlBuilder(document).also { it.block() }
    val children = document.children
    if (children.size != 1) {
        throw IllegalArgumentException(
            if (children.isEmpty()) "There is no root element" else "There are multiple root elements"
        )
    }
    return (children[0] as XmlElementBuilder).build()
}

class XmlBuilder(
    private val node: XmlElementBuilder
) {
    operator fun String.invoke(vararg attr: NamedValue, block: XmlBuilder.() -> Any? = { }) {
        just().invoke(*attr, block = block)
    }

    operator fun Name.invoke(vararg attr: NamedValue, block: XmlBuilder.() -> Any? = { }) {
        val child = createElement(node).also { node.appendChild(it) }
        attr.forEach { child.setAttribute(it) }
        XmlBuilder(child).block().let { if (it is String) child.appendTextNode(it) }
    }

    private fun XmlElementBuilder.setAttribute(attr: NamedValue) {
        attr.name.setAttribute(this, attr.value)
    }

    interface Name {
        fun createElement(parent: XmlElementBuilder): XmlElementBuilder
        fun setAttribute(element: XmlElementBuilder, value: String)
    }

    class NameWithNamespace(
        private val qualifiedName: String,
        private val namespaceUri: String
    ) : Name {
        override fun createElement(parent: XmlElementBuilder): XmlElementBuilder =
            XmlElementBuilder(qualifiedName).also {
                it.parent = parent
                it.setUri(namespaceUri)
            }

        override fun setAttribute(element: XmlElementBuilder, value: String) {
            val attr = XmlAttributeBuilder(qualifiedName, value)
            element.appendAttribute(attr)
            attr.setUri(namespaceUri)
        }
    }

    class JustName(
        private val name: String
    ) : Name {
        override fun createElement(parent: XmlElementBuilder): XmlElementBuilder =
            XmlElementBuilder(name).also { it.parent = parent }

        override fun setAttribute(element: XmlElementBuilder, value: String): Unit =
            element.appendAttribute(XmlAttributeBuilder(name, value))
    }

    private fun String.just(): Name = JustName(this)

    data class NamedValue(
        val name: Name,
        val value: String
    )

    infix fun String.ns(namespaceUri: String): NameWithNamespace = NameWithNamespace(this, namespaceUri)
    infix fun String.eq(that: String): NamedValue = NamedValue(just(), that)
    infix fun Name.eq(that: String): NamedValue = NamedValue(this, that)
}
