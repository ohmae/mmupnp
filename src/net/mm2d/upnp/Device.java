/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import net.mm2d.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class Device {
    private static final String TAG = "Device";
    private final ControlPoint mControlPoint;
    private SsdpMessage mSsdp;
    private String mDescription;
    private String mUdn;
    private String mDeviceType;
    private String mFriendlyName;
    private String mManufacture;
    private String mManufactureUrl;
    private String mModelName;
    private String mModelUrl;
    private String mModelDescription;
    private String mModelNumber;
    private String mSerialNumber;
    private String mPresentationUrl;
    private final Map<String, Map<String, String>> mTagMap;
    private final List<Icon> mIconList;
    private final List<Service> mServiceList;

    public Device(ControlPoint controlPoint) {
        mControlPoint = controlPoint;
        mIconList = new ArrayList<>();
        mServiceList = new ArrayList<>();
        mTagMap = new LinkedHashMap<>();
        mTagMap.put("", new HashMap<String, String>());
    }

    public ControlPoint getControlPoint() {
        return mControlPoint;
    }

    void setSsdpMessage(SsdpMessage message) {
        mSsdp = message;
    }

    SsdpMessage getSsdpMessage() {
        return mSsdp;
    }

    public String getUuid() {
        return mSsdp.getUuid();
    }

    public long getExpireTime() {
        return mSsdp.getExpireTime();
    }

    public String getDescription() {
        return mDescription;
    }

    URL getAbsoluteUrl(String url) throws MalformedURLException {
        if (url.startsWith("http://")) {
            return new URL(url);
        }
        String baseUrl = getLocation();
        if (url.startsWith("/")) {
            int pos = baseUrl.indexOf("://");
            pos = baseUrl.indexOf("/", pos + 3);
            return new URL(baseUrl.substring(0, pos) + url);
        }
        int pos = baseUrl.indexOf("?");
        if (pos > 0) {
            baseUrl = baseUrl.substring(0, pos);
        }
        if (baseUrl.endsWith("/")) {
            return new URL(baseUrl + url);
        }
        pos = baseUrl.lastIndexOf("/");
        baseUrl = baseUrl.substring(0, pos + 1);
        return new URL(baseUrl + url);
    }

    void loadDescription() throws IOException, SAXException, ParserConfigurationException {
        final HttpClient client = new HttpClient(true);
        final URL url = new URL(getLocation());
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.GET);
        request.setUrl(url, true);
        request.setHeader(Http.USER_AGENT, Http.USER_AGENT_VALUE);
        request.setHeader(Http.CONNECTION, Http.KEEP_ALIVE);
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK) {
            Log.i(TAG, response.toString());
            client.close();
            throw new IOException(response.getStartLine());
        }
        mDescription = response.getBody();
        parseDescription(mDescription);
        for (final Icon icon : mIconList) {
            icon.loadBinary(client);
        }
        for (final Service service : mServiceList) {
            service.loadDescription(client);
        }
        client.close();
    }

    private void parseIconList(Node listNode) {
        Node node = listNode.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if ("icon".equals(node.getLocalName())) {
                mIconList.add(parseIcon((Element) node));
            }
        }
    }

    private Icon parseIcon(Element element) {
        final Icon.Builder icon = new Icon.Builder();
        icon.setDevice(this);
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if ("mimetype".equals(tag)) {
                icon.setMimeType(node.getTextContent());
            } else if ("height".equals(tag)) {
                icon.setHeight(node.getTextContent());
            } else if ("width".equals(tag)) {
                icon.setWidth(node.getTextContent());
            } else if ("depth".equals(tag)) {
                icon.setDepth(node.getTextContent());
            } else if ("url".equals(tag)) {
                icon.setUrl(node.getTextContent());
            }
        }
        return icon.build();
    }

    private void parseServiceList(Node listNode) {
        Node node = listNode.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if ("service".equals(node.getLocalName())) {
                mServiceList.add(parseService((Element) node));
            }
        }
    }

    private Service parseService(Element element) {
        final Service.Builder service = new Service.Builder();
        service.setDevice(this);
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if ("serviceType".equals(tag)) {
                service.setServiceType(node.getTextContent());
            } else if ("serviceId".equals(tag)) {
                service.setServiceId(node.getTextContent());
            } else if ("SCPDURL".equals(tag)) {
                service.setScpdUrl(node.getTextContent());
            } else if ("eventSubURL".equals(tag)) {
                service.setEventSubUrl(node.getTextContent());
            } else if ("controlURL".equals(tag)) {
                service.setControlUrl(node.getTextContent());
            }
        }
        return service.build();
    }

    private void parseDescription(String xml)
            throws IOException, SAXException, ParserConfigurationException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        final DocumentBuilder db = dbf.newDocumentBuilder();
        final Document doc = db.parse(new InputSource(new StringReader(xml)));
        Node n = doc.getDocumentElement().getFirstChild();
        for (; n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if ("device".equals(n.getLocalName())) {
                break;
            }
        }
        if (n == null) {
            return;
        }
        Node node = n.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if ("iconList".equals(tag)) {
                parseIconList(node);
            } else if ("serviceList".equals(tag)) {
                parseServiceList(node);
            } else {
                String ns = node.getNamespaceURI();
                ns = ns == null ? "" : ns;
                final String text = node.getTextContent();
                Map<String, String> nsmap = mTagMap.get(ns);
                if (nsmap == null) {
                    nsmap = new HashMap<>();
                    mTagMap.put(ns, nsmap);
                }
                nsmap.put(tag, node.getTextContent());
                if ("UDN".equals(tag)) {
                    mUdn = text;
                } else if ("deviceType".equals(tag)) {
                    mDeviceType = text;
                } else if ("friendlyName".equals(tag)) {
                    mFriendlyName = text;
                } else if ("manufacturer".equals(tag)) {
                    mManufacture = text;
                } else if ("manufacturerURL".equals(tag)) {
                    mManufactureUrl = text;
                } else if ("modelName".equals(tag)) {
                    mModelName = text;
                } else if ("modelURL".equals(tag)) {
                    mModelUrl = text;
                } else if ("modelDescription".equals(tag)) {
                    mModelDescription = text;
                } else if ("modelNumber".equals(tag)) {
                    mModelNumber = text;
                } else if ("serialNumber".equals(tag)) {
                    mSerialNumber = text;
                } else if ("presentationURL".equals(tag)) {
                    mPresentationUrl = text;
                }
            }
        }
    }

    public String getValue(String name) {
        for (final Entry<String, Map<String, String>> entry : mTagMap.entrySet()) {
            final String value = entry.getValue().get(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public String getValue(String name, String namespace) {
        final Map<String, String> nsmap = mTagMap.get(namespace);
        if (nsmap == null) {
            return null;
        }
        return nsmap.get(name);
    }

    public String getLocation() {
        return mSsdp.getLocation();
    }

    public String getIpAddress() {
        try {
            URL url = new URL(getLocation());
            return url.getHost();
        } catch (MalformedURLException e) {
            return "";
        }
    }

    public String getUdn() {
        return mUdn;
    }

    public String getDeviceType() {
        return mDeviceType;
    }

    public String getFriendlyName() {
        return mFriendlyName;
    }

    public String getManufacture() {
        return mManufacture;
    }

    public String getManufactureUrl() {
        return mManufactureUrl;
    }

    public String getModelName() {
        return mModelName;
    }

    public String getModelUrl() {
        return mModelUrl;
    }

    public String getModelDescription() {
        return mModelDescription;
    }

    public String getModelNumber() {
        return mModelNumber;
    }

    public String getSerialNumber() {
        return mSerialNumber;
    }

    public String getPresentationUrl() {
        return mPresentationUrl;
    }

    public List<Icon> getIconList() {
        return Collections.unmodifiableList(mIconList);
    }

    public List<Service> getServiceList() {
        return Collections.unmodifiableList(mServiceList);
    }

    public Service findServiceById(String id) {
        for (final Service service : mServiceList) {
            if (service.getServiceId().equals(id)) {
                return service;
            }
        }
        return null;
    }

    public Service findServiceByType(String type) {
        for (final Service service : mServiceList) {
            if (service.getServiceType().equals(type)) {
                return service;
            }
        }
        return null;
    }

    public Action findAction(String name) {
        for (final Service service : mServiceList) {
            final Action action = service.findAction(name);
            if (action != null) {
                return action;
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        return mUdn.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Device)) {
            return false;
        }
        final Device d = (Device) obj;
        return mUdn.equals(d.getUdn());
    }

    @Override
    public String toString() {
        return mFriendlyName;
    }
}
