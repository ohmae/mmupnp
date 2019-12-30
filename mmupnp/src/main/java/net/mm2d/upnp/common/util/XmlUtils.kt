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
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

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
}

/**
 * Iterator for Node.
 *
 * @receiver Node
 */
operator fun Node.iterator(): Iterator<Node> = object : Iterator<Node> {
    private var node: Node? = this@iterator
    override fun next(): Node = node?.also { node = it.nextSibling }
        ?: throw NoSuchElementException()

    override fun hasNext(): Boolean = node != null
}

/**
 * Returns the Iterable of Sibling Nodes
 *
 * @receiver Node performing iteration
 */
fun Node.siblings(): Iterable<Node> = Iterable { iterator() }

/**
 * Returns the Iterable of Sibling Elements
 *
 * @receiver Node performing iteration
 */
fun Node.siblingElements(): Iterable<Element> = siblings().mapNotNull { it as? Element }

/**
 * Returns the Iterable of Child Nodes
 *
 * @receiver Node performing iteration
 */
fun Node.children(): Iterable<Node> = childNodes.asIterable()

/**
 * Returns the Iterable of Child Elements
 *
 * @receiver Node performing iteration
 */
fun Node.childElements(): Iterable<Element> = childNodes.asElementIterable()

/**
 * Search for a node with the specified local name among the child nodes of the receiver node.
 *
 * @receiver Search target node
 * @param localName local name
 */
fun Node.findChildElementByLocalName(localName: String): Element? =
    childElements().firstOrNull { it.localName == localName }

/**
 * Iterator for NodeList.
 *
 * @receiver NodeList
 */
operator fun NodeList.iterator(): Iterator<Node> = object : Iterator<Node> {
    private var index = 0
    override fun next(): Node = item(index++)
    override fun hasNext(): Boolean = index < length
}

/**
 * Returns the Iterable of Node
 *
 * @receiver NodeList performing iteration
 */
fun NodeList.asIterable(): Iterable<Node> = Iterable { iterator() }

/**
 * Returns the Iterable of Node
 *
 * @receiver NodeList performing iteration
 */
fun NodeList.asElementIterable(): Iterable<Element> = asIterable().mapNotNull { it as? Element }

/**
 * Iterator for NamedNodeMap.
 */
operator fun NamedNodeMap.iterator() = object : Iterator<Node> {
    private var index = 0
    override fun next(): Node = item(index++)
    override fun hasNext(): Boolean = index < length
}

/**
 * Iterate NamedNodeMap.
 *
 * @receiver node list
 * @param action Action to be performed on each node
 */
inline fun NamedNodeMap.forEach(action: (Node) -> Unit): Unit = iterator().forEach(action)

/**
 * Returns the Iterable of Node
 *
 * @receiver NamedNodeMap performing iteration
 */
fun NamedNodeMap.asIterable(): Iterable<Node> = Iterable { iterator() }

/**
 * Create new element and add to receiver document.
 *
 * @receiver Document
 * @param namespaceUri The namespace URI of the element to create.
 * @param qualifiedName The qualified name of the element to create.
 * @return New element
 */
@Throws(DOMException::class)
fun Document.appendWithNs(namespaceUri: String?, qualifiedName: String): Element =
    createElementNS(namespaceUri, qualifiedName).also { appendChild(it) }

/**
 * Create new element with namespace and add to receiver node.
 *
 * @receiver Parent node
 * @param namespaceUri The namespace URI of the element to create.
 * @param qualifiedName The qualified name of the element to create.
 * @return New element
 */
@Throws(DOMException::class)
fun Node.appendWithNs(namespaceUri: String?, qualifiedName: String): Element =
    ownerDocument.createElementNS(namespaceUri, qualifiedName).also { appendChild(it) }

/**
 * Create new element with namespace and add to receiver node.
 *
 * @receiver Parent node
 * @param namespaceUri The namespace URI of the element to create.
 * @param qualifiedName The qualified name of the element to create.
 * @param textContent Text content the element to create.
 * @return New element
 */
@Throws(DOMException::class)
fun Node.appendWithNs(namespaceUri: String?, qualifiedName: String, textContent: String?): Element =
    appendWithNs(namespaceUri, qualifiedName).also { it.textContent = textContent }

/**
 * Create new element and add to receiver node.
 *
 * @receiver Document
 * @param tagName The name of the element to create.
 * @return New element
 */
@Throws(DOMException::class)
fun Document.append(tagName: String): Element =
    createElement(tagName).also { appendChild(it) }

/**
 * Create new element and add to receiver node.
 *
 * @receiver Parent node
 * @param tagName The name of the element to create.
 * @return New element
 */
@Throws(DOMException::class)
fun Node.append(tagName: String): Element =
    ownerDocument.createElement(tagName).also { appendChild(it) }

/**
 * Create new element with text content and add to receiver node.
 *
 * @receiver Parent node
 * @param tagName The name of the element to create.
 * @param textContent Text content the element to create.
 * @return New element
 */
@Throws(DOMException::class)
fun Node.append(tagName: String, textContent: String?): Element =
    append(tagName).also { it.textContent = textContent }

/**
 * Convert XML Document to String.
 *
 * @receiver document XML Document to convert
 * @return Converted string
 * @throws TransformerException If a conversion error occurs
 */
// VisibleForTesting
@Throws(TransformerException::class)
fun Document.toXml(): String {
    val sw = StringWriter()
    TransformerFactory.newInstance().newTransformer().also {
        //it.setOutputProperty(OutputKeys.METHOD, "xml")
        it.setOutputProperty(OutputKeys.INDENT, "yes")
        //it.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        it.transform(DOMSource(this), StreamResult(sw))
    }
    return sw.toString()
}
