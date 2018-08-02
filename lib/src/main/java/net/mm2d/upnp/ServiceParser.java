/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
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
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Serviceのパース処理。
 *
 * <p>Description XMLのダウンロード、パース、Builderへの値の設定をstatic methodで定義。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
final class ServiceParser {
    /**
     * SCPDURLからDescriptionを取得し、パースする。
     *
     * <p>可能であればKeepAliveを行う。
     *
     * @param client        通信に使用するHttpClient
     * @param deviceBuilder DeviceのBuilder
     * @param builder       ServiceのBuilder
     * @throws IOException                  通信エラー
     * @throws SAXException                 XMLパースエラー
     * @throws ParserConfigurationException 実装が使用できないかインスタンス化できない
     */
    static void loadDescription(
            @Nonnull final HttpClient client,
            @Nonnull final DeviceImpl.Builder deviceBuilder,
            @Nonnull final ServiceImpl.Builder builder)
            throws IOException, SAXException, ParserConfigurationException {
        final String scpdUrl = builder.getScpdUrl();
        if (scpdUrl == null) {
            throw new IOException();
        }
        final String baseUrl = deviceBuilder.getBaseUrl();
        final int scopeId = deviceBuilder.getSsdpMessage().getScopeId();
        final URL url = Http.makeAbsoluteUrl(baseUrl, scpdUrl, scopeId);
        final String description = client.downloadString(url);
        if (TextUtils.isEmpty(description)) {
            // 空であっても必須パラメータはそろっているため正常として扱う。
            return;
        }
        builder.setDescription(description);
        final Document doc = XmlUtils.newDocument(true, description);
        parseActionList(builder, doc.getElementsByTagName("action"));
        parseStateVariableList(builder, doc.getElementsByTagName("stateVariable"));
    }

    private static void parseActionList(
            @Nonnull final ServiceImpl.Builder builder,
            @Nonnull final NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            builder.addActionBuilder(parseAction((Element) nodeList.item(i)));
        }
    }

    private static void parseStateVariableList(
            @Nonnull final ServiceImpl.Builder builder,
            @Nonnull final NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            builder.addStateVariable(parseStateVariable((Element) nodeList.item(i)));
        }
    }

    @Nullable
    private static String getTagName(@Nonnull final Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }
        return node.getLocalName();
    }

    @Nonnull
    private static ActionImpl.Builder parseAction(@Nonnull final Element element) {
        final ActionImpl.Builder builder = new ActionImpl.Builder();
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            final String tag = getTagName(node);
            if (TextUtils.equals(tag, "name")) {
                builder.setName(node.getTextContent());
            } else if (TextUtils.equals(tag, "argumentList")) {
                for (Node c = node.getFirstChild(); c != null; c = c.getNextSibling()) {
                    if (TextUtils.equals(getTagName(c), "argument")) {
                        builder.addArgumentBuilder(parseArgument((Element) c));
                    }
                }
            }
        }
        return builder;
    }

    @Nonnull
    private static ArgumentImpl.Builder parseArgument(@Nonnull final Element element) {
        final ArgumentImpl.Builder builder = new ArgumentImpl.Builder();
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            final String tag = getTagName(node);
            if (TextUtils.isEmpty(tag)) {
                continue;
            }
            final String value = node.getTextContent();
            setField(builder, tag, value);
        }
        return builder;
    }

    @SuppressWarnings("IfCanBeSwitch")
    private static void setField(
            @Nonnull final ArgumentImpl.Builder builder,
            @Nonnull final String tag,
            @Nonnull final String value) {
        if ("name".equals(tag)) {
            builder.setName(value);
        } else if ("direction".equals(tag)) {
            builder.setDirection(value);
        } else if ("relatedStateVariable".equals(tag)) {
            builder.setRelatedStateVariableName(value);
        }
    }

    @Nonnull
    private static StateVariable parseStateVariable(@Nonnull final Element element) {
        final StateVariableImpl.Builder builder = new StateVariableImpl.Builder()
                .setSendEvents(element.getAttribute("sendEvents"))
                .setMulticast(element.getAttribute("multicast"));
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            final String tag = getTagName(node);
            if (TextUtils.isEmpty(tag)) {
                continue;
            }
            if ("name".equals(tag)) {
                builder.setName(node.getTextContent());
            } else if ("dataType".equals(tag)) {
                builder.setDataType(node.getTextContent());
            } else if ("defaultValue".equals(tag)) {
                builder.setDefaultValue(node.getTextContent());
            } else if ("allowedValueList".equals(tag)) {
                parseAllowedValueList(builder, (Element) node);
            } else if ("allowedValueRange".equals(tag)) {
                parseAllowedValueRange(builder, (Element) node);
            }
        }
        return builder.build();
    }

    private static void parseAllowedValueList(
            @Nonnull final StateVariableImpl.Builder builder,
            @Nonnull final Element element) {
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            final String tag = getTagName(node);
            if ("allowedValue".equals(tag)) {
                builder.addAllowedValue(node.getTextContent());
            }
        }
    }

    private static void parseAllowedValueRange(
            @Nonnull final StateVariableImpl.Builder builder,
            @Nonnull final Element element) {
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            final String tag = getTagName(node);
            if (TextUtils.isEmpty(tag)) {
                continue;
            }
            final String value = node.getTextContent();
            setField(builder, tag, value);
        }
    }

    @SuppressWarnings("IfCanBeSwitch")
    private static void setField(
            @Nonnull final StateVariableImpl.Builder builder,
            @Nonnull final String tag,
            @Nonnull final String value) {
        if ("step".equals(tag)) {
            builder.setStep(value);
        } else if ("minimum".equals(tag)) {
            builder.setMinimum(value);
        } else if ("maximum".equals(tag)) {
            builder.setMaximum(value);
        }
    }

    // インスタンス化禁止
    private ServiceParser() {
    }
}
