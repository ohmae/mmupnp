/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 *  http://opensource.org/licenses/MIT
 */

package net.mm2d.xml.parser

import net.mm2d.xml.builder.XmlAttributeBuilder
import net.mm2d.xml.builder.XmlElementBuilder
import net.mm2d.xml.node.XmlElement
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import javax.xml.parsers.SAXParserFactory

object XmlParser {
    fun parse(xml: String): XmlElement? {
        val documentBuilder = DocumentBuilder()
        val parser = SAXParserFactory.newInstance().newSAXParser()
        parser.parse(xml.byteInputStream(), documentBuilder)
        return documentBuilder.result?.build()
    }

    class DocumentBuilder : DefaultHandler() {
        private var root: XmlElementBuilder? = null
        private var work: XmlElementBuilder? = null
        var result: XmlElementBuilder? = null
            private set

        override fun startDocument() {
            root = null
            work = null
        }

        override fun endDocument() {
            result = root
            root = null
            work = null
        }

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            val element = XmlElementBuilder(qName)
            repeat(attributes.length) {
                element.appendAttribute(XmlAttributeBuilder(attributes.getQName(it), attributes.getValue(it)))
            }
            work?.appendChild(element)
            work = element
            if (root == null) {
                root = element
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            work = work?.parent
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            work?.appendTextNode(String(ch, start, length))
        }
    }
}
