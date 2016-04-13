/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import net.mm2d.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.net.InterfaceAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class Service {
    public static class Builder {
        private Device mDevice;
        private String mServiceType;
        private String mServiceId;
        private String mScpdUrl;
        private String mControlUrl;
        private String mEventSubUrl;

        public Builder() {
        }

        public void setDevice(Device device) {
            mDevice = device;
        }

        public void setServiceType(String serviceType) {
            mServiceType = serviceType;
        }

        public void setServiceId(String serviceId) {
            mServiceId = serviceId;
        }

        public void setScpdUrl(String scpdUrl) {
            mScpdUrl = scpdUrl;
        }

        public void setControlUrl(String controlUrl) {
            mControlUrl = controlUrl;
        }

        public void setEventSubUrl(String eventSubUrl) {
            mEventSubUrl = eventSubUrl;
        }

        public Service build() {
            return new Service(this);
        }
    }

    private static final String TAG = "Service";
    private final ControlPoint mControlPoint;
    private final Device mDevice;
    private String mDescription;
    private final String mServiceType;
    private final String mServiceId;
    private final String mScpdUrl;
    private final String mControlUrl;
    private final String mEventSubUrl;
    private List<Action> mActionList;
    private final Map<String, Action> mActionMap;
    private List<StateVariable> mStateVariableList;
    private final Map<String, StateVariable> mStateVariableMap;
    private long mSubscriptionStart;
    private long mSubscriptionTimeout;
    private String mSubscriptionId;

    private Service(Builder builder) {
        mDevice = builder.mDevice;
        mControlPoint = mDevice.getControlPoint();
        mServiceType = builder.mServiceType;
        mServiceId = builder.mServiceId;
        mScpdUrl = builder.mScpdUrl;
        mControlUrl = builder.mControlUrl;
        mEventSubUrl = builder.mEventSubUrl;
        mActionMap = new LinkedHashMap<>();
        mStateVariableMap = new LinkedHashMap<>();
    }

    public Device getDevice() {
        return mDevice;
    }

    URL getAbsoluteUrl(String url) throws MalformedURLException {
        return mDevice.getAbsoluteUrl(url);
    }

    public String getServiceType() {
        return mServiceType;
    }

    public String getServiceId() {
        return mServiceId;
    }

    public String getScpdUrl() {
        return mScpdUrl;
    }

    public String getControlUrl() {
        return mControlUrl;
    }

    public String getEventSubUrl() {
        return mEventSubUrl;
    }

    public String getDescription() {
        return mDescription;
    }

    public List<Action> getActionList() {
        if (mActionList == null) {
            final List<Action> list = new ArrayList<>(mActionMap.values());
            mActionList = Collections.unmodifiableList(list);
        }
        return mActionList;
    }

    public Action findAction(String name) {
        return mActionMap.get(name);
    }

    public List<StateVariable> getStateVariableList() {
        if (mStateVariableList == null) {
            final List<StateVariable> list = new ArrayList<>(mStateVariableMap.values());
            mStateVariableList = Collections.unmodifiableList(list);
        }
        return mStateVariableList;
    }

    public StateVariable findStateVariable(String name) {
        return mStateVariableMap.get(name);
    }

    void loadDescription(HttpClient client)
            throws IOException, SAXException, ParserConfigurationException {
        final URL url = getAbsoluteUrl(mScpdUrl);
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.GET);
        request.setUrl(url, true);
        request.setHeader(Http.USER_AGENT, Http.USER_AGENT_VALUE);
        request.setHeader(Http.CONNECTION, Http.KEEP_ALIVE);
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK) {
            Log.i(TAG, response.toString());
            throw new IOException();
        }
        mDescription = response.getBody();
        parseDescription(mDescription);
    }

    void parseDescription(String xml)
            throws IOException, SAXException, ParserConfigurationException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        final DocumentBuilder db = dbf.newDocumentBuilder();
        final Document doc = db.parse(new InputSource(new StringReader(xml)));
        final List<Action.Builder> alist = parseActionList(doc.getElementsByTagName("action"));
        parseStateVariableList(doc.getElementsByTagName("stateVariable"));
        for (final Action.Builder builder : alist) {
            for (final Argument.Builder b : builder.getArgumentBuilderList()) {
                final String name = b.getRelatedStateVariableName();
                final StateVariable v = mStateVariableMap.get(name);
                b.setRelatedStateVariable(v);
            }
            final Action a = builder.build();
            mActionMap.put(a.getName(), a);
        }
    }

    private List<Action.Builder> parseActionList(NodeList nodeList) {
        final List<Action.Builder> list = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            list.add(parseAction((Element) nodeList.item(i)));
        }
        return list;
    }

    private void parseStateVariableList(NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            final StateVariable.Builder builder = parseStateVariable((Element) nodeList.item(i));
            final StateVariable variable = builder.build();
            mStateVariableMap.put(variable.getName(), variable);
        }
    }

    private Action.Builder parseAction(Element element) {
        final Action.Builder builder = new Action.Builder();
        builder.serService(this);
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if ("name".equals(tag)) {
                builder.setName(node.getTextContent());
            } else if ("argumentList".equals(tag)) {
                for (Node c = node.getFirstChild(); c != null; c = c.getNextSibling()) {
                    if (c.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    if ("argument".equals(c.getLocalName())) {
                        builder.addArugmentBuilder(parseArgument((Element) c));
                    }
                }
            }
        }
        return builder;
    }

    private Argument.Builder parseArgument(Element element) {
        final Argument.Builder builder = new Argument.Builder();
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if ("name".equals(tag)) {
                builder.setName(node.getTextContent());
            } else if ("direction".equals(tag)) {
                builder.setDirection(node.getTextContent());
            } else if ("relatedStateVariable".equals(tag)) {
                final String text = node.getTextContent();
                builder.setRelatedStateVariableName(text);
            }
        }
        return builder;
    }

    private StateVariable.Builder parseStateVariable(Element element) {
        final StateVariable.Builder builder = new StateVariable.Builder();
        builder.setService(this);
        builder.setSendEvents(element.getAttribute("sendEvents"));
        builder.setMulticast(element.getAttribute("multicast"));
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if ("name".equals(tag)) {
                builder.setName(node.getTextContent());
            } else if ("dataType".equals(tag)) {
                builder.setDataType(node.getTextContent());
            } else if ("defaultValue".equals(tag)) {
                builder.setDefaultValue(node.getTextContent());
            } else if ("allowedValueList".equals(tag)) {
                Node child = node.getFirstChild();
                for (; child != null; child = child.getNextSibling()) {
                    if (child.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    if ("allowedValue".equals(child.getLocalName())) {
                        builder.addAllowedValue(child.getTextContent());
                    }
                }
            } else if ("allowedValueRange".equals(tag)) {
                Node child = node.getFirstChild();
                for (; child != null; child = child.getNextSibling()) {
                    if (child.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    final String ctag = child.getLocalName();
                    if ("step".equals(ctag)) {
                        builder.setStep(child.getTextContent());
                    } else if ("minimum".equals(ctag)) {
                        builder.setMinimum(child.getTextContent());
                    } else if ("maximum".equals(ctag)) {
                        builder.setMaximun(child.getTextContent());
                    }
                }
            }
        }
        return builder;
    }

    private String getCallback() {
        final StringBuilder sb = new StringBuilder();
        sb.append('<');
        sb.append("http://");
        final SsdpMessage ssdp = mDevice.getSsdpMessage();
        final InterfaceAddress ifa = ssdp.getInterfaceAddress();
        sb.append(ifa.getAddress().getHostAddress());
        sb.append(':');
        final int port = mControlPoint.getEventPort();
        sb.append(String.valueOf(port));
        sb.append('/');
        sb.append(mDevice.getUdn());
        sb.append('/');
        sb.append(mServiceId);
        sb.append('>');
        return sb.toString();
    }

    private long getTimeout(HttpResponse response) {
        final String timeout = response.getHeader(Http.TIMEOUT).toLowerCase();
        if (timeout.contains("infinite")) {
            return -1;
        }
        final String prefix = "second-";
        final int pos = timeout.indexOf(prefix);
        if (pos < 0) {
            return 0;
        }
        final String secondSection = timeout.substring(pos + prefix.length());
        try {
            final int second = Integer.parseInt(secondSection);
            return second * 1000L;
        } catch (final NumberFormatException e) {
            Log.w(TAG, e);
        }
        return 0;
    }

    public boolean subscribe() throws IOException {
        return subscribe(false);
    }

    public boolean subscribe(boolean keep) throws IOException {
        if (mEventSubUrl == null) {
            return false;
        }
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.SUBSCRIBE);
        final URL url = getAbsoluteUrl(mEventSubUrl);
        request.setUrl(url, true);
        request.setHeader(Http.NT, "upnp:event");
        request.setHeader(Http.CALLBACK, getCallback());
        request.setHeader(Http.TIMEOUT, "Second-300");
        request.setHeader(Http.CONTENT_LENGTH, "0");
        final HttpClient client = new HttpClient(false);
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK) {
            System.out.println(response.toString());
            return false;
        }
        final String sid = response.getHeader(Http.SID);
        final long timeout = getTimeout(response);
        if (sid == null || sid.isEmpty() || timeout == 0) {
            System.out.println(response.toString());
            return false;
        }
        mSubscriptionId = sid;
        mSubscriptionStart = System.currentTimeMillis();
        mSubscriptionTimeout = timeout;
        mControlPoint.registerSubscribeService(this);
        if (keep) {
            mControlPoint.addSubscribeKeeper(this);
        }
        return true;
    }

    public boolean renewSubscribe() throws IOException {
        return renewSubscribe(true);
    }

    boolean renewSubscribe(boolean notify) throws IOException {
        if (mEventSubUrl == null || mSubscriptionId == null) {
            return false;
        }
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.SUBSCRIBE);
        final URL url = getAbsoluteUrl(mEventSubUrl);
        request.setUrl(url, true);
        request.setHeader(Http.SID, mSubscriptionId);
        request.setHeader(Http.TIMEOUT, "Second-300");
        request.setHeader(Http.CONTENT_LENGTH, "0");
        final HttpClient client = new HttpClient(false);
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK) {
            System.out.println(response.toString());
            return false;
        }
        final String sid = response.getHeader(Http.SID);
        final long timeout = getTimeout(response);
        if (sid == null || sid.isEmpty()
                || !sid.equals(mSubscriptionId) || timeout == 0) {
            System.out.println(response.toString());
            return false;
        }
        mSubscriptionStart = System.currentTimeMillis();
        mSubscriptionTimeout = timeout;
        if (notify) {
            mControlPoint.renewSubscribeService();
        }
        return true;
    }

    public boolean unsubscribe() throws IOException {
        if (mEventSubUrl == null || mSubscriptionId == null) {
            return false;
        }
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.UNSUBSCRIBE);
        final URL url = getAbsoluteUrl(mEventSubUrl);
        request.setUrl(url, true);
        request.setHeader(Http.SID, mSubscriptionId);
        request.setHeader(Http.CONTENT_LENGTH, "0");
        final HttpClient client = new HttpClient(false);
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK) {
            System.out.println(response.toString());
            return false;
        }
        mControlPoint.unregisterSubscribeService(this);
        mSubscriptionId = null;
        mSubscriptionStart = 0;
        mSubscriptionTimeout = 0;
        return true;
    }

    public String getSubscriptionId() {
        return mSubscriptionId;
    }

    public long getSubscriptionStart() {
        return mSubscriptionStart;
    }

    public long getSubscriptionTimeout() {
        return mSubscriptionTimeout;
    }
}
