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
    /**
     * DeviceDescriptionを読み込む。
     *
     * <p>Descriptionのパースを行い、Builderに登録する。
     * また、内部で記述されているicon/serviceのDescriptionの取得、パースも行い、
     * それぞれのBuilderも作成する。
     *
     * @param client  通信に使用するHttpClient
     * @param builder DeviceのBuilder
     * @throws IOException                  通信上での何らかの問題
     * @throws SAXException                 XMLのパースに失敗
     * @throws ParserConfigurationException XMLパーサが利用できない場合
     */
    static void loadDescription(
            @Nonnull final HttpClient client,
            @Nonnull final Device.Builder builder)
            throws IOException, SAXException, ParserConfigurationException {
        parseDescription(builder, client.downloadString(new URL(builder.getLocation())));
        loadServices(client, builder);
    }

    private static void loadServices(
            @Nonnull final HttpClient client,
            @Nonnull final Device.Builder builder)
            throws IOException, SAXException, ParserConfigurationException {
        for (final Service.Builder serviceBuilder : builder.getServiceBuilderList()) {
            ServiceParser.loadDescription(client, builder.getBaseUrl(), serviceBuilder);
        }
        for (final Device.Builder deviceBuilder : builder.getEmbeddedDeviceBuilderList()) {
            loadServices(client, deviceBuilder);
        }
    }

    private static void parseDescription(
            @Nonnull final Device.Builder builder,
            @Nonnull final String description)
            throws IOException, SAXException, ParserConfigurationException {
        builder.setDescription(description);
        final Document doc = XmlUtils.newDocument(true, description);
        final Node deviceNode = XmlUtils.findChildElementByLocalName(doc.getDocumentElement(), "device");
        if (deviceNode == null) {
            throw new IllegalStateException();
        }
        parseDevice(builder, deviceNode);
    }

    private static void parseDevice(
            @Nonnull final Device.Builder builder,
            @Nonnull final Node deviceNode) {
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
                    parseIconList(builder, node);
                    break;
                case "serviceList":
                    parseServiceList(builder, node);
                    break;
                case "deviceList":
                    parseDeviceList(builder, node);
                    break;
                default:
                    String namespace = node.getNamespaceURI();
                    namespace = namespace == null ? "" : namespace;
                    final String value = node.getTextContent();
                    builder.putTag(namespace, tag, value);
                    setField(builder, tag, value);
                    break;
            }
        }
    }

    private static void setField(
            @Nonnull final Device.Builder builder,
            @Nonnull final String tag,
            @Nonnull final String value) {
        switch (tag) {
            case "UDN":
                builder.setUdn(value);
                break;
            case "UPC":
                builder.setUpc(value);
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
            case "URLBase":
                builder.setUrlBase(value);
                break;
            default:
                break;
        }
    }

    private static void parseIconList(
            @Nonnull final Device.Builder builder,
            @Nonnull final Node listNode) {
        Node node = listNode.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (TextUtils.equals(node.getLocalName(), "icon")) {
                builder.addIconBuilder(parseIcon(node));
            }
        }
    }

    private static Icon.Builder parseIcon(@Nonnull final Node iconNode) {
        final Icon.Builder builder = new Icon.Builder();
        Node node = iconNode.getFirstChild();
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

    private static void setField(
            @Nonnull final Icon.Builder builder,
            @Nonnull final String tag,
            @Nonnull final String value) {
        switch (tag) {
            case "mimetype":
                builder.setMimeType(value);
                break;
            case "height":
                builder.setHeight(value);
                break;
            case "width":
                builder.setWidth(value);
                break;
            case "depth":
                builder.setDepth(value);
                break;
            case "url":
                builder.setUrl(value);
                break;
            default:
                break;
        }
    }

    private static void parseServiceList(
            @Nonnull final Device.Builder builder,
            @Nonnull final Node listNode) {
        Node node = listNode.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (TextUtils.equals(node.getLocalName(), "service")) {
                builder.addServiceBuilder(parseService(node));
            }
        }
    }

    @Nonnull
    private static Service.Builder parseService(@Nonnull final Node serviceNode) {
        final Service.Builder builder = new Service.Builder();
        Node node = serviceNode.getFirstChild();
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

    private static void setField(
            @Nonnull final Service.Builder builder,
            @Nonnull final String tag,
            @Nonnull final String value) {
        switch (tag) {
            case "serviceType":
                builder.setServiceType(value);
                break;
            case "serviceId":
                builder.setServiceId(value);
                break;
            case "SCPDURL":
                builder.setScpdUrl(value);
                break;
            case "eventSubURL":
                builder.setEventSubUrl(value);
                break;
            case "controlURL":
                builder.setControlUrl(value);
                break;
            default:
                break;
        }
    }

    private static void parseDeviceList(
            @Nonnull final Device.Builder builder,
            @Nonnull final Node listNode) {
        final List<Device.Builder> builderList = new ArrayList<>();
        Node node = listNode.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (TextUtils.equals(node.getLocalName(), "device")) {
                final Device.Builder embeddedBuilder = builder.createEmbeddedDeviceBuilder();
                parseDevice(embeddedBuilder, node);
                builderList.add(embeddedBuilder);
            }
        }
        builder.setEmbeddedDeviceBuilderList(builderList);
    }
}
