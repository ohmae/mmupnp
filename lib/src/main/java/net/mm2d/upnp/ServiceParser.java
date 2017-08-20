/*
 * Copyright(c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.TextUtils;
import net.mm2d.util.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;

import javax.annotation.Nonnull;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Serviceのパース処理。
 *
 * <p>Description XMLのダウンロード、パース、Builderへの値の設定をstatic methodで定義。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class ServiceParser {
    /**
     * SCPDURLからDescriptionを取得し、パースする。
     *
     * <p>可能であればKeepAliveを行う。
     *
     * @param client  通信に使用するHttpClient
     * @param baseUrl URLのベースとして使用する値
     * @param builder ServiceのBuilder
     * @throws IOException                  通信エラー
     * @throws SAXException                 XMLパースエラー
     * @throws ParserConfigurationException 実装が使用できないかインスタンス化できない
     */
    static void loadDescription(@Nonnull final HttpClient client,
                                @Nonnull final String baseUrl,
                                @Nonnull final Service.Builder builder)
            throws IOException, SAXException, ParserConfigurationException {
        final String scpdUrl = builder.getScpdUrl();
        if (scpdUrl == null) {
            throw new IOException();
        }
        final URL url = Device.getAbsoluteUrl(baseUrl, scpdUrl);
        final String description = client.downloadString(url);
        builder.setDescription(description);
        if (TextUtils.isEmpty(description)) {
            // 空であっても必須パラメータはそろっているため正常として扱う。
            return;
        }
        final Document doc = XmlUtils.newDocument(true, description);
        parseActionList(builder, doc.getElementsByTagName("action"));
        parseStateVariableList(builder, doc.getElementsByTagName("stateVariable"));
    }

    private static void parseActionList(@Nonnull final Service.Builder builder, @Nonnull final NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            builder.addActionBuilder(parseAction((Element) nodeList.item(i)));
        }
    }

    private static void parseStateVariableList(@Nonnull final Service.Builder builder, @Nonnull final NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            builder.addVariableBuilder(parseStateVariable((Element) nodeList.item(i)));
        }
    }

    @Nonnull
    private static Action.Builder parseAction(@Nonnull final Element element) {
        final Action.Builder builder = new Action.Builder();
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if (TextUtils.equals(tag, "name")) {
                builder.setName(node.getTextContent());
            } else if (TextUtils.equals(tag, "argumentList")) {
                for (Node c = node.getFirstChild(); c != null; c = c.getNextSibling()) {
                    if (c.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    if (TextUtils.equals(c.getLocalName(), "argument")) {
                        builder.addArgumentBuilder(parseArgument((Element) c));
                    }
                }
            }
        }
        return builder;
    }

    @Nonnull
    private static Argument.Builder parseArgument(@Nonnull final Element element) {
        final Argument.Builder builder = new Argument.Builder();
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if (TextUtils.isEmpty(tag)) {
                continue;
            }
            final String value = node.getTextContent();
            setField(builder, tag, value);
        }
        return builder;
    }

    private static void setField(@Nonnull final Argument.Builder builder,
                                 @Nonnull final String tag, @Nonnull final String value) {
        switch (tag) {
            case "name":
                builder.setName(value);
                break;
            case "direction":
                builder.setDirection(value);
                break;
            case "relatedStateVariable":
                builder.setRelatedStateVariableName(value);
                break;
            default:
                break;
        }
    }

    @Nonnull
    private static StateVariable.Builder parseStateVariable(@Nonnull final Element element) {
        final StateVariable.Builder builder = new StateVariable.Builder()
                .setSendEvents(element.getAttribute("sendEvents"))
                .setMulticast(element.getAttribute("multicast"));
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if (TextUtils.isEmpty(tag)) {
                continue;
            }
            switch (tag) {
                case "name":
                    builder.setName(node.getTextContent());
                    break;
                case "dataType":
                    builder.setDataType(node.getTextContent());
                    break;
                case "defaultValue":
                    builder.setDefaultValue(node.getTextContent());
                    break;
                case "allowedValueList":
                    parseAllowedValueList(builder, (Element) node);
                    break;
                case "allowedValueRange":
                    parseAllowedValueRange(builder, (Element) node);
                    break;
                default:
                    break;
            }
        }
        return builder;
    }

    private static void parseAllowedValueList(
            @Nonnull final StateVariable.Builder builder, @Nonnull final Element element) {
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if ("allowedValue".equals(node.getLocalName())) {
                builder.addAllowedValue(node.getTextContent());
            }
        }
    }

    private static void parseAllowedValueRange(
            @Nonnull final StateVariable.Builder builder, @Nonnull final Element element) {
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if (TextUtils.isEmpty(tag)) {
                continue;
            }
            final String value = node.getTextContent();
            setField(builder, tag, value);
        }
    }

    private static void setField(@Nonnull final StateVariable.Builder builder,
                                 @Nonnull final String tag, @Nonnull final String value) {
        switch (tag) {
            case "step":
                builder.setStep(value);
                break;
            case "minimum":
                builder.setMinimum(value);
                break;
            case "maximum":
                builder.setMaximum(value);
                break;
            default:
                break;
        }
    }
}
