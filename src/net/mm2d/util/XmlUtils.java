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
 * XMLの定形処理を実装する
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class XmlUtils {
    private static DocumentBuilder sDocumentBuilder;

    private static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder();
    }

    private static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        if (sDocumentBuilder == null) {
            sDocumentBuilder = newDocumentBuilder();
        }
        return sDocumentBuilder;
    }

    /**
     * 空のDocumentを作成する。
     *
     * @return Document
     * @throws ParserConfigurationException 実装が使用できないかインスタンス化できない
     */
    @Nonnull
    public static synchronized Document newDocument() throws ParserConfigurationException {
        return getDocumentBuilder().newDocument();
    }

    /**
     * 引数のStringをもとにしたDocumentを作成する。
     *
     * @param xml XML文字列
     * @return Document
     * @throws SAXException 構文解析エラーが発生した
     * @throws IOException 入出力エラーが発生した
     * @throws ParserConfigurationException 実装が使用できないかインスタンス化できない
     */
    @Nonnull
    public static synchronized Document newDocument(@Nonnull String xml)
            throws SAXException, IOException, ParserConfigurationException {
        return getDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    /**
     * ノード以下にある特定の名前を持つ最初のエレメントノードを返す
     *
     * @param parent 親ノード
     * @param localName 検索するローカル名
     * @return 見つかったエレメントノード、見つからなければnull
     */
    @Nullable
    public static Element findChildElementByLocalName(
            @Nonnull Node parent, @Nonnull String localName) {
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
