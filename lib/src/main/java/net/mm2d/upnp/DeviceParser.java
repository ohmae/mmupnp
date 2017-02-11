/*
 * Copyright(c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.Log;
import net.mm2d.util.TextUtils;
import net.mm2d.util.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.parsers.ParserConfigurationException;

/**
 * デバイスをパースする。
 *
 * <p>Description XMLのダウンロード、パース、Builderへの値の設定をstatic methodで定義。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class DeviceParser {
    private static final String TAG = DeviceParser.class.getSimpleName();

    /**
     * インスタンス化禁止
     */
    private DeviceParser() {
        throw new AssertionError();
    }

    /**
     * DeviceDescriptionを読み込む。
     *
     * <p>Descriptionのパースを行い、Builderに登録する。
     * また、内部で記述されているicon/serviceのDescriptionの取得、パースも行い、
     * それぞれのBuilderも作成する。
     *
     * @param client        通信に使用するHttpClient
     * @param deviceBuilder DeviceのBuilder
     * @throws IOException                  通信上での何らかの問題
     * @throws SAXException                 XMLのパースに失敗
     * @throws ParserConfigurationException XMLパーサが利用できない場合
     */
    static void loadDescription(
            @Nonnull HttpClient client,
            @Nonnull Device.Builder deviceBuilder)
            throws IOException, SAXException, ParserConfigurationException {
        final String location = deviceBuilder.getLocation();
        parseDescription(deviceBuilder, downloadString(client, location));
        for (final Service.Builder serviceBuilder : deviceBuilder.getServiceBuilderList()) {
            ServiceParser.loadDescription(client, location, serviceBuilder);
        }
    }

    @Nonnull
    private static String downloadString(@Nonnull HttpClient client, @Nonnull String location) throws IOException {
        final URL url = new URL(location);
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.GET);
        request.setUrl(url, true);
        request.setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE);
        request.setHeader(Http.CONNECTION, Http.KEEP_ALIVE);
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK || TextUtils.isEmpty(response.getBody())) {
            Log.i(TAG, response.toString());
            throw new IOException(response.getStartLine());
        }
        return response.getBody();
    }

    private static void parseDescription(@Nonnull Device.Builder deviceBuilder, @Nonnull String description)
            throws IOException, SAXException, ParserConfigurationException {
        deviceBuilder.setDescription(description);
        final Document doc = XmlUtils.newDocument(true, description);
        final Node deviceNode = XmlUtils.findChildElementByLocalName(doc.getDocumentElement(), "device");
        if (deviceNode == null) {
            return;
        }
        Node node = deviceNode.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if (TextUtils.isEmpty(tag)) {
                continue;
            }
            switch (tag) {
                case "iconList":
                    deviceBuilder.setIconBuilderList(parseIconList(node));
                    break;
                case "serviceList":
                    deviceBuilder.setServiceBuilderList(parseServiceList(node));
                    break;
                default:
                    String namespace = node.getNamespaceURI();
                    namespace = namespace == null ? "" : namespace;
                    final String value = node.getTextContent();
                    deviceBuilder.putTag(namespace, tag, value);
                    setField(deviceBuilder, tag, value);
                    break;
            }
        }
    }

    @Nonnull
    private static List<Icon.Builder> parseIconList(@Nonnull Node listNode) {
        final List<Icon.Builder> builderList = new ArrayList<>();
        Node node = listNode.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (TextUtils.equals(node.getLocalName(), "icon")) {
                builderList.add(parseIcon((Element) node));
            }
        }
        return builderList;
    }

    @Nonnull
    private static Icon.Builder parseIcon(@Nonnull Element element) {
        final Icon.Builder builder = new Icon.Builder();
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
                case "mimetype":
                    builder.setMimeType(text);
                    break;
                case "height":
                    builder.setHeight(text);
                    break;
                case "width":
                    builder.setWidth(text);
                    break;
                case "depth":
                    builder.setDepth(text);
                    break;
                case "url":
                    builder.setUrl(text);
                    break;
                default:
                    break;
            }
        }
        return builder;
    }

    @Nonnull
    private static List<Service.Builder> parseServiceList(@Nonnull Node listNode) {
        final List<Service.Builder> builderList = new ArrayList<>();
        Node node = listNode.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (TextUtils.equals(node.getLocalName(), "service")) {
                builderList.add(parseService((Element) node));
            }
        }
        return builderList;
    }

    @Nonnull
    private static Service.Builder parseService(@Nonnull Element element) {
        final Service.Builder builder = new Service.Builder();
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
                case "serviceType":
                    builder.setServiceType(text);
                    break;
                case "serviceId":
                    builder.setServiceId(text);
                    break;
                case "SCPDURL":
                    builder.setScpdUrl(text);
                    break;
                case "eventSubURL":
                    builder.setEventSubUrl(text);
                    break;
                case "controlURL":
                    builder.setControlUrl(text);
                    break;
                default:
                    break;
            }
        }
        return builder;
    }

    private static void setField(@Nonnull Device.Builder builder, @Nonnull String tag, @Nonnull String value) {
        switch (tag) {
            case "UDN":
                builder.setUdn(value);
                break;
            case "deviceType":
                builder.setDeviceType(value);
                break;
            case "friendlyName":
                builder.setFriendlyName(value);
                break;
            case "manufacturer":
                builder.setManufacture(value);
                break;
            case "manufacturerURL":
                builder.setManufactureUrl(value);
                break;
            case "modelName":
                builder.setModelName(value);
                break;
            case "modelURL":
                builder.setModelUrl(value);
                break;
            case "modelDescription":
                builder.setModelDescription(value);
                break;
            case "modelNumber":
                builder.setModelNumber(value);
                break;
            case "serialNumber":
                builder.setSerialNumber(value);
                break;
            case "presentationURL":
                builder.setPresentationUrl(value);
                break;
            default:
                break;
        }
    }
}
