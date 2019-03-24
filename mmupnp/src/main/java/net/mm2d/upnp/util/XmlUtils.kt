/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.util

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.xml.sax.SAXException

import java.io.IOException
import java.io.StringReader
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/**
 * XMLのユーティリティメソッドを提供する。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object XmlUtils {
    private val documentBuilder: DocumentBuilder by lazy {
        newDocumentBuilder(false)
    }
    private val documentBuilderNs: DocumentBuilder by lazy {
        newDocumentBuilder(true)
    }

    @Throws(ParserConfigurationException::class)
    private fun newDocumentBuilder(awareness: Boolean): DocumentBuilder {
        return DocumentBuilderFactory.newInstance().also {
            it.isNamespaceAware = awareness
        }.newDocumentBuilder()
    }

    @Synchronized
    @Throws(ParserConfigurationException::class)
    private fun getDocumentBuilder(awareness: Boolean): DocumentBuilder {
        return if (awareness) documentBuilderNs else documentBuilder
    }

    /**
     * 空のDocumentを作成する。
     *
     * @param awareness trueのときXML namespaceに対応
     * @return Document
     * @throws ParserConfigurationException 実装が使用できないかインスタンス化できない
     */
    @Synchronized
    @Throws(ParserConfigurationException::class)
    @JvmStatic
    fun newDocument(awareness: Boolean): Document {
        return getDocumentBuilder(awareness).newDocument()
    }

    /**
     * 引数のStringをもとにしたDocumentを作成する。
     *
     * @param awareness trueのときXML namespaceに対応
     * @param xml       XML文字列
     * @return Document
     * @throws SAXException                 構文解析エラーが発生した
     * @throws IOException                  入出力エラーが発生した
     * @throws ParserConfigurationException 実装が使用できないかインスタンス化できない
     */
    @Synchronized
    @Throws(SAXException::class, IOException::class, ParserConfigurationException::class)
    @JvmStatic
    fun newDocument(awareness: Boolean, xml: String): Document {
        val reader = StringReader(xml)
        return getDocumentBuilder(awareness).parse(InputSource(reader))
    }

    /**
     * ノード以下にある特定の名前を持つ最初のエレメントノードを返す
     *
     * @param parent    親ノード
     * @param localName 検索するローカル名
     * @return 見つかったエレメントノード、見つからなければnull
     */
    @JvmStatic
    fun findChildElementByLocalName(parent: Node, localName: String): Element? {
        parent.firstChild?.forEachElement {
            if (localName == it.localName) {
                return it
            }
        }
        return null
    }
}

/**
 * 指定ノードの兄弟ノード全体をイテレートする。
 *
 * @receiver イテレーションを行う親ノード
 * @param action 各要素について実行されるaction
 */
inline fun Node.forEachElement(action: (Element) -> Unit) {
    var node: Node? = this
    while (node != null) {
        if (node is Element) {
            action(node)
        }
        node = node.nextSibling
    }
}

/**
 * 指定ノードの子ノードの中から指定ローカル名のノードを探す。
 *
 * @receiver 探索対象ノード
 * @param localName ローカル名
 */
fun Node.findChildElementByLocalName(localName: String): Element? {
    firstChild?.forEachElement {
        if (localName == it.localName) {
            return it
        }
    }
    return null
}

/**
 * ノードリストをイテレートする。
 *
 * @receiver NodeList
 * @param action 各要素について実行されるaction
 */
inline fun NodeList.forEach(action: (Node) -> Unit) {
    for (i in 0 until length) action(item(i))
}
