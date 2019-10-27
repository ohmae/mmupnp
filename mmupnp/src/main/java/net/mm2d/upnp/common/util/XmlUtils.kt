/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.util

import org.w3c.dom.*
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/**
 * Provide XML utility methods.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object XmlUtils {
    @Throws(ParserConfigurationException::class)
    private fun newDocumentBuilder(awareness: Boolean): DocumentBuilder =
        DocumentBuilderFactory.newInstance().also {
            it.isNamespaceAware = awareness
        }.newDocumentBuilder()

    /**
     * Create an empty Document.
     *
     * @param awareness if true, XML namespace aware
     * @return Document
     * @throws ParserConfigurationException If there is a problem with instantiation
     */
    @Throws(ParserConfigurationException::class)
    @JvmStatic
    fun newDocument(awareness: Boolean): Document = newDocumentBuilder(awareness).newDocument()

    /**
     * Create new Document that contains the argument string.
     *
     * @param awareness if true, XML namespace aware
     * @param xml XML string
     * @return Document
     * @throws SAXException if an parse error occurs.
     * @throws IOException if an I/O error occurs.
     * @throws ParserConfigurationException If there is a problem with instantiation
     */
    @Throws(SAXException::class, IOException::class, ParserConfigurationException::class)
    @JvmStatic
    fun newDocument(awareness: Boolean, xml: String): Document {
        val reader = StringReader(xml)
        return newDocumentBuilder(awareness).parse(InputSource(reader))
    }

    /**
     * Returns the first element node with a specific name below the parent node.
     *
     * @param parent parent node
     * @param localName local name to search
     * @return Element node found, null if not found
     */
    @JvmStatic
    fun findChildElementByLocalName(parent: Node, localName: String): Element? =
        parent.findChildElementByLocalName(localName)
}

/**
 * Iterate over all sibling nodes of the specified node.
 *
 * @receiver Node performing iteration
 * @param action Action to be performed on each element
 */
inline fun Node.forEachElement(action: (Element) -> Unit): Unit = siblingElements().asSequence().forEach(action)

/**
 * Returns the Iterable of Element
 *
 * @receiver Node performing iteration
 */
fun Node.siblingElements(): Iterable<Element> =
    object : Iterable<Element> {
        override fun iterator(): Iterator<Element> = ElementIterator(this@siblingElements)
    }

private class ElementIterator(
    node: Node
) : Iterator<Element> {
    private var node: Node? = node
    override fun next(): Element {
        skipUntilElement()
        return (node as? Element)
            ?.also { node = it.nextSibling }
            ?: throw NoSuchElementException()
    }

    override fun hasNext(): Boolean {
        skipUntilElement()
        return node is Element
    }

    private fun skipUntilElement() {
        var n: Node? = node
        while (n != null) {
            if (n is Element) {
                node = n
                return
            }
            n = n.nextSibling
        }
    }
}

/**
 * Returns the Iterable of Node
 *
 * @receiver Node performing iteration
 */
fun Node.siblings(): Iterable<Node> {
    return object : Iterable<Node> {
        override fun iterator(): Iterator<Node> = NodeIterator(this@siblings)
    }
}

private class NodeIterator(
    node: Node
) : Iterator<Node> {
    private var node: Node? = node
    override fun next(): Node = node?.also { node = it.nextSibling } ?: throw NoSuchElementException()
    override fun hasNext(): Boolean = node != null
}

/**
 * Search for a node with the specified local name among the child nodes of the receiver node.
 *
 * @receiver Search target node
 * @param localName local name
 */
fun Node.findChildElementByLocalName(localName: String): Element? =
    firstChild?.siblingElements()?.firstOrNull { it.localName == localName }

/**
 * Iterate NodeList.
 *
 * @receiver node list
 * @param action Action to be performed on each node
 */
inline fun NodeList.forEach(action: (Node) -> Unit): Unit = asIterable().forEach(action)

/**
 * Returns the Iterable of Node
 *
 * @receiver NodeList performing iteration
 */
fun NodeList.asIterable(): Iterable<Node> =
    object : Iterable<Node> {
        override fun iterator(): Iterator<Node> = NodeListIterator(this@asIterable)
    }

private class NodeListIterator(
    private val nodeList: NodeList
) : Iterator<Node> {
    private var index = 0
    override fun next(): Node = nodeList.item(index++)
    override fun hasNext(): Boolean = index < nodeList.length
}

/**
 * Iterate NamedNodeMap.
 *
 * @receiver node list
 * @param action Action to be performed on each node
 */
inline fun NamedNodeMap.forEach(action: (Node) -> Unit): Unit = asIterable().forEach(action)

/**
 * Returns the Iterable of Node
 *
 * @receiver NamedNodeMap performing iteration
 */
fun NamedNodeMap.asIterable(): Iterable<Node> =
    object : Iterable<Node> {
        override fun iterator(): Iterator<Node> = NamedNodeMapIterator(this@asIterable)
    }

private class NamedNodeMapIterator(
    private val map: NamedNodeMap
) : Iterator<Node> {
    private var index = 0
    override fun next(): Node = map.item(index++)
    override fun hasNext(): Boolean = index < map.length
}
