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
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

/**
 * デバイスをパースする。
 *
 * <p>Description XMLのダウンロード、パース、Builderへの値の設定をstatic methodで定義。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
final class DeviceParser {
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
            @Nonnull final DeviceImpl.Builder builder)
            throws IOException, SAXException, ParserConfigurationException {
        final URL url = Http.makeUrlWithScopeId(builder.getLocation(), builder.getSsdpMessage().getScopeId());
        final String description = client.downloadString(url);
        if (TextUtils.isEmpty(description)) {
            throw new IOException("download error");
        }
        builder.onDownloadDescription(client);
        parseDescription(builder, description);
        loadServices(client, builder);
    }

    private static void loadServices(
            @Nonnull final HttpClient client,
            @Nonnull final DeviceImpl.Builder builder)
            throws IOException, SAXException, ParserConfigurationException {
        for (final ServiceImpl.Builder serviceBuilder : builder.getServiceBuilderList()) {
            ServiceParser.loadDescription(client, builder, serviceBuilder);
        }
        for (final DeviceImpl.Builder deviceBuilder : builder.getEmbeddedDeviceBuilderList()) {
            loadServices(client, deviceBuilder);
        }
    }

    // VisibleForTesting
    static void parseDescription(
            @Nonnull final DeviceImpl.Builder builder,
            @Nonnull final String description)
            throws IOException, SAXException, ParserConfigurationException {
        builder.setDescription(description);
        final Document doc = XmlUtils.newDocument(true, description);
        final Node deviceNode = XmlUtils.findChildElementByLocalName(doc.getDocumentElement(), "device");
        if (deviceNode == null) {
            throw new IOException();
        }
        parseDevice(builder, deviceNode);
    }

    @Nullable
    private static String getTagName(@Nonnull final Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }
        return node.getLocalName();
    }

    private static void parseDevice(
            @Nonnull final DeviceImpl.Builder builder,
            @Nonnull final Node deviceNode) {
        Node node = deviceNode.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            final String tag = getTagName(node);
            if (TextUtils.isEmpty(tag)) {
                continue;
            }
            if ("iconList".equals(tag)) {
                parseIconList(builder, node);
            } else if ("serviceList".equals(tag)) {
                parseServiceList(builder, node);
            } else if ("deviceList".equals(tag)) {
                parseDeviceList(builder, node);
            } else {
                final String namespace = node.getNamespaceURI();
                final String value = node.getTextContent();
                builder.putTag(namespace, tag, value);
                setField(builder, tag, value);
            }
        }
    }

    @SuppressWarnings("IfCanBeSwitch")
    private static void setField(
            @Nonnull final DeviceImpl.Builder builder,
            @Nonnull final String tag,
            @Nonnull final String value) {
        if ("UDN".equals(tag)) {
            builder.setUdn(value);
        } else if ("UPC".equals(tag)) {
            builder.setUpc(value);
        } else if ("deviceType".equals(tag)) {
            builder.setDeviceType(value);
        } else if ("friendlyName".equals(tag)) {
            builder.setFriendlyName(value);
        } else if ("manufacturer".equals(tag)) {
            builder.setManufacture(value);
        } else if ("manufacturerURL".equals(tag)) {
            builder.setManufactureUrl(value);
        } else if ("modelName".equals(tag)) {
            builder.setModelName(value);
        } else if ("modelURL".equals(tag)) {
            builder.setModelUrl(value);
        } else if ("modelDescription".equals(tag)) {
            builder.setModelDescription(value);
        } else if ("modelNumber".equals(tag)) {
            builder.setModelNumber(value);
        } else if ("serialNumber".equals(tag)) {
            builder.setSerialNumber(value);
        } else if ("presentationURL".equals(tag)) {
            builder.setPresentationUrl(value);
        } else if ("URLBase".equals(tag)) {
            builder.setUrlBase(value);
        }
    }

    private static void parseIconList(
            @Nonnull final DeviceImpl.Builder builder,
            @Nonnull final Node listNode) {
        Node node = listNode.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (TextUtils.equals(getTagName(node), "icon")) {
                builder.addIconBuilder(parseIcon(node));
            }
        }
    }

    @Nonnull
    private static IconImpl.Builder parseIcon(@Nonnull final Node iconNode) {
        final IconImpl.Builder builder = new IconImpl.Builder();
        Node node = iconNode.getFirstChild();
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
            @Nonnull final IconImpl.Builder builder,
            @Nonnull final String tag,
            @Nonnull final String value) {
        if ("mimetype".equals(tag)) {
            builder.setMimeType(value);
        } else if ("height".equals(tag)) {
            builder.setHeight(value);
        } else if ("width".equals(tag)) {
            builder.setWidth(value);
        } else if ("depth".equals(tag)) {
            builder.setDepth(value);
        } else if ("url".equals(tag)) {
            builder.setUrl(value);
        }
    }

    private static void parseServiceList(
            @Nonnull final DeviceImpl.Builder builder,
            @Nonnull final Node listNode) {
        Node node = listNode.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (TextUtils.equals(getTagName(node), "service")) {
                builder.addServiceBuilder(parseService(node));
            }
        }
    }

    @Nonnull
    private static ServiceImpl.Builder parseService(@Nonnull final Node serviceNode) {
        final ServiceImpl.Builder serviceBuilder = new ServiceImpl.Builder();
        Node node = serviceNode.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            final String tag = getTagName(node);
            if (TextUtils.isEmpty(tag)) {
                continue;
            }
            final String value = node.getTextContent();
            setField(serviceBuilder, tag, value);
        }
        return serviceBuilder;
    }

    @SuppressWarnings("IfCanBeSwitch")
    private static void setField(
            @Nonnull final ServiceImpl.Builder builder,
            @Nonnull final String tag,
            @Nonnull final String value) {
        if ("serviceType".equals(tag)) {
            builder.setServiceType(value);
        } else if ("serviceId".equals(tag)) {
            builder.setServiceId(value);
        } else if ("SCPDURL".equals(tag)) {
            builder.setScpdUrl(value);
        } else if ("eventSubURL".equals(tag)) {
            builder.setEventSubUrl(value);
        } else if ("controlURL".equals(tag)) {
            builder.setControlUrl(value);
        }
    }

    private static void parseDeviceList(
            @Nonnull final DeviceImpl.Builder builder,
            @Nonnull final Node listNode) {
        final List<DeviceImpl.Builder> builderList = new ArrayList<>();
        Node node = listNode.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (TextUtils.equals(getTagName(node), "device")) {
                final DeviceImpl.Builder embeddedBuilder = builder.createEmbeddedDeviceBuilder();
                parseDevice(embeddedBuilder, node);
                builderList.add(embeddedBuilder);
            }
        }
        builder.setEmbeddedDeviceBuilderList(builderList);
    }

    // インスタンス化禁止
    private DeviceParser() {
        throw new AssertionError();
    }
}
