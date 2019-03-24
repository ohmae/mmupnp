/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample

import org.xml.sax.SAXException
import java.awt.Component
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.StringWriter
import javax.swing.JFrame
import javax.swing.tree.DefaultMutableTreeNode
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.TransformerFactoryConfigurationError
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
open class UpnpNode(
    userObject: Any,
    allowsChildren: Boolean = true
) : DefaultMutableTreeNode(userObject, allowsChildren) {
    open fun getDetailXml(): String {
        return ""
    }

    open fun formatDescription(): String {
        return ""
    }

    protected fun formatXml(xml: String): String {
        try {
            val dbf = DocumentBuilderFactory.newInstance().also {
                it.isNamespaceAware = true
            }
            val doc = dbf.newDocumentBuilder()
                .parse(ByteArrayInputStream(xml.toByteArray(charset("utf-8"))))
            val sw = StringWriter()
            TransformerFactory.newInstance().newTransformer().also {
                it.setOutputProperty(OutputKeys.INDENT, "yes")
                it.setOutputProperty(OutputKeys.METHOD, "xml")
            }.transform(DOMSource(doc), StreamResult(sw))
            return sw.toString()
        } catch (e: IllegalArgumentException) {
        } catch (e: ParserConfigurationException) {
        } catch (e: SAXException) {
        } catch (e: IOException) {
        } catch (e: TransformerFactoryConfigurationError) {
        } catch (e: TransformerException) {
        }
        return ""
    }

    open fun showContextMenu(frame: JFrame, invoker: Component, x: Int, y: Int) {
    }
}
