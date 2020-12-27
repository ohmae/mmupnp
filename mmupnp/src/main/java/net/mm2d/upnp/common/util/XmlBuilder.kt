package net.mm2d.upnp.common.util

import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

fun buildXml(block: XmlNode.() -> Unit): XmlNode = XmlNode(newDocument()).also { it.block() }

private fun newDocument(): Document =
    DocumentBuilderFactory.newInstance().also {
        it.isNamespaceAware = true
    }.newDocumentBuilder().newDocument()

class XmlNode(
    private val node: Node
) {
    private val document: Document = if (node is Document) node else node.ownerDocument

    operator fun String.invoke(vararg attr: Pair<String, String>, block: XmlNode.() -> Any = { }) {
        val child = document.createElement(this).also { node.appendChild(it) }
        attr.forEach { child.setAttribute(it.first, it.second) }
        XmlNode(child).block().let { if (it is String) child.textContent = it }
    }

    override fun toString(): String {
        val sw = StringWriter()
        TransformerFactory.newInstance().newTransformer().also {
            it.setOutputProperty(OutputKeys.INDENT, "yes")
            it.transform(DOMSource(document), StreamResult(sw))
        }
        return sw.toString()
    }
}
