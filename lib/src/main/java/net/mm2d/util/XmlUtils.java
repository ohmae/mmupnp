/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * XMLのユーティリティメソッドを提供する。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class XmlUtils {
    private static final DocumentBuilder[] sDocumentBuilders = new DocumentBuilder[2];

    @Nonnull
    private static DocumentBuilder newDocumentBuilder(final boolean awareness)
            throws ParserConfigurationException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(awareness);
        return factory.newDocumentBuilder();
    }

    @Nonnull
    private static synchronized DocumentBuilder getDocumentBuilder(final boolean awareness)
            throws ParserConfigurationException {
        final int index = awareness ? 1 : 0;
        if (sDocumentBuilders[index] == null) {
            sDocumentBuilders[index] = newDocumentBuilder(awareness);
        }
        return sDocumentBuilders[index];
    }

    /**
     * 空のDocumentを作成する。
     *
     * @param awareness trueのときXML namespaceに対応
     * @return Document
     * @throws ParserConfigurationException 実装が使用できないかインスタンス化できない
     */
    @Nonnull
    public static synchronized Document newDocument(final boolean awareness)
            throws ParserConfigurationException {
        return getDocumentBuilder(awareness).newDocument();
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
    @Nonnull
    public static synchronized Document newDocument(
            final boolean awareness,
            @Nonnull final String xml)
            throws SAXException, IOException, ParserConfigurationException {
        return getDocumentBuilder(awareness).parse(new InputSource(new StringReader(xml)));
    }

    /**
     * ノード以下にある特定の名前を持つ最初のエレメントノードを返す
     *
     * @param parent    親ノード
     * @param localName 検索するローカル名
     * @return 見つかったエレメントノード、見つからなければnull
     */
    @Nullable
    public static Element findChildElementByLocalName(
            @Nonnull final Node parent,
            @Nonnull final String localName) {
        Node child = parent.getFirstChild();
        for (; child != null; child = child.getNextSibling()) {
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (localName.equals(child.getLocalName())) {
                return (Element) child;
            }
        }
        return null;
    }
}
