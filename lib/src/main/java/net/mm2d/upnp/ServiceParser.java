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
import java.util.ArrayList;
import java.util.List;

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
    private static final String TAG = ServiceParser.class.getSimpleName();

    /**
     * インスタンス化禁止
     */
    private ServiceParser() {
        throw new AssertionError();
    }

    /**
     * SCPDURLからDescriptionを取得し、パースする。
     *
     * <p>可能であればKeepAliveを行う。
     *
     * @param client   通信に使用するHttpClient
     * @param location SSDPパケットに記述されたlocation
     * @param builder  ServiceのBuilder
     * @throws IOException                  通信エラー
     * @throws SAXException                 XMLパースエラー
     * @throws ParserConfigurationException 実装が使用できないかインスタンス化できない
     */
    static void loadDescription(@Nonnull HttpClient client,
                                @Nonnull String location,
                                @Nonnull Service.Builder builder)
            throws IOException, SAXException, ParserConfigurationException {
        final URL url = Device.getAbsoluteUrl(location, builder.getScpdUrl());
        final String description = client.downloadString(url);
        builder.setDescription(description);
        final Document doc = XmlUtils.newDocument(true, description);
        builder.setActionBuilderList(parseActionList(doc.getElementsByTagName("action")));
        builder.setVariableBuilderList(parseStateVariableList(doc.getElementsByTagName("stateVariable")));
    }

    @Nonnull
    private static List<Action.Builder> parseActionList(@Nonnull NodeList nodeList) {
        final List<Action.Builder> builderList = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            builderList.add(parseAction((Element) nodeList.item(i)));
        }
        return builderList;
    }

    @Nonnull
    private static List<StateVariable.Builder> parseStateVariableList(@Nonnull NodeList nodeList) {
        final List<StateVariable.Builder> builderList = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            builderList.add(parseStateVariable((Element) nodeList.item(i)));
        }
        return builderList;
    }

    @Nonnull
    private static Action.Builder parseAction(@Nonnull Element element) {
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
    private static Argument.Builder parseArgument(@Nonnull Element element) {
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
            final String text = node.getTextContent();
            switch (tag) {
                case "name":
                    builder.setName(text);
                    break;
                case "direction":
                    builder.setDirection(text);
                    break;
                case "relatedStateVariable":
                    builder.setRelatedStateVariableName(text);
                    break;
                default:
                    break;
            }
        }
        return builder;
    }

    @Nonnull
    private static StateVariable.Builder parseStateVariable(@Nonnull Element element) {
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
            @Nonnull StateVariable.Builder builder, @Nonnull Element element) {
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
            @Nonnull StateVariable.Builder builder, @Nonnull Element element) {
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if (TextUtils.isEmpty(tag)) {
                continue;
            }
            final String text = node.getTextContent();
            switch (tag) {
                case "step":
                    builder.setStep(text);
                    break;
                case "minimum":
                    builder.setMinimum(text);
                    break;
                case "maximum":
                    builder.setMaximum(text);
                    break;
                default:
                    break;
            }
        }
    }
}
