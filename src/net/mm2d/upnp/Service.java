/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

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
    private final Device mDevice;
    private String mServiceType;
    private String mServiceId;
    private String mScpdUrl;
    private String mControlUrl;
    private String mEventSubUrl;
    private String mServiceXml;
    private String mSubscriptionId;
    private List<Action> mActionList;
    private final Map<String, Action> mActionMap;
    private List<StateVariable> mStateVariableList;
    private final Map<String, StateVariable> mStateVariableMap;

    public Service(Device device) {
        mDevice = device;
        mActionMap = new LinkedHashMap<>();
        mStateVariableMap = new LinkedHashMap<>();
    }

    void getXml(HttpClient client) throws IOException {
        final URL url = getAbsoluteUrl(mScpdUrl);
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.GET);
        request.setUrl(url, true);
        request.setHeader(Http.USER_AGENT, Http.USER_AGENT_VALUE);
        request.setHeader(Http.CONNECTION, Http.KEEP_ALIVE);
        final HttpResponse response = client.post(request);
        mServiceXml = response.getBody();
        try {
            parseServiceXml(mServiceXml);
        } catch (final SAXException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void parseServiceXml(String xml)
            throws IOException, SAXException, ParserConfigurationException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        final DocumentBuilder db = dbf.newDocumentBuilder();
        final Document doc = db.parse(new InputSource(new StringReader(xml)));
        parseStateVariableList(doc.getElementsByTagName("stateVariable"));
        parseActionList(doc.getElementsByTagName("action"));
    }

    private void parseActionList(NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Action a = parseAction((Element) nodeList.item(i));
            mActionMap.put(a.getName(), a);
        }
    }

    private void parseStateVariableList(NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            final StateVariable v = parseStateVariable((Element) nodeList.item(i));
            mStateVariableMap.put(v.getName(), v);
        }
    }

    private Action parseAction(Element element) {
        final Action action = new Action(this);
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if ("name".equals(tag)) {
                action.setName(node.getTextContent());
            } else if ("argumentList".equals(tag)) {
                for (Node c = node.getFirstChild(); c != null; c = c.getNextSibling()) {
                    if (c.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    if ("argument".equals(c.getLocalName())) {
                        action.addArugment(parseArgument(action, (Element) c));
                    }
                }
            }
        }
        return action;
    }

    private Argument parseArgument(Action action, Element element) {
        final Argument argument = new Argument(action);
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if ("name".equals(tag)) {
                argument.setName(node.getTextContent());
            } else if ("direction".equals(tag)) {
                argument.setDirection(node.getTextContent());
            } else if ("relatedStateVariable".equals(tag)) {
                final String text = node.getTextContent();
                argument.setRelatedStateVariableName(text);
                final StateVariable v = mStateVariableMap.get(text);
                argument.setRelatedStateVariable(v);
            }
        }
        return argument;
    }

    private StateVariable parseStateVariable(Element element) {
        final StateVariable variable = new StateVariable(this);
        variable.setSendEvents(element.getAttribute("sendEvents"));
        variable.setMulticast(element.getAttribute("multicast"));
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if ("name".equals(tag)) {
                variable.setName(node.getTextContent());
            } else if ("dataType".equals(tag)) {
                variable.setDataType(node.getTextContent());
            } else if ("defaultValue".equals(tag)) {
                variable.setDefaultValue(node.getTextContent());
            } else if ("allowedValueList".equals(tag)) {
                Node child = node.getFirstChild();
                for (; child != null; child = child.getNextSibling()) {
                    if (child.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    if ("allowedValue".equals(child.getLocalName())) {
                        variable.addAllowedValue(child.getTextContent());
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
                        variable.setStep(child.getTextContent());
                    } else if ("minimum".equals(ctag)) {
                        variable.setMinimum(child.getTextContent());
                    } else if ("maximum".equals(ctag)) {
                        variable.setMaximun(child.getTextContent());
                    }
                }
            }
        }
        return variable;
    }

    URL getAbsoluteUrl(String url) throws MalformedURLException {
        return mDevice.getAbsoluteUrl(url);
    }

    public String getServiceType() {
        return mServiceType;
    }

    public void setServiceType(String serviceType) {
        mServiceType = serviceType;
    }

    public String getServiceId() {
        return mServiceId;
    }

    public void setServiceId(String serviceId) {
        mServiceId = serviceId;
    }

    public String getScpdUrl() {
        return mScpdUrl;
    }

    public void setScpdUrl(String scpdUrl) {
        mScpdUrl = scpdUrl;
    }

    public String getControlUrl() {
        return mControlUrl;
    }

    public void setControlUrl(String controlUrl) {
        mControlUrl = controlUrl;
    }

    public String getEventSubUrl() {
        return mEventSubUrl;
    }

    public void setEventSubUrl(String eventSubUrl) {
        mEventSubUrl = eventSubUrl;
    }

    public String getServiceXml() {
        return mServiceXml;
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

    private String getCallback() {
        final StringBuilder sb = new StringBuilder();
        sb.append('<');
        sb.append("http://");
        final InterfaceAddress ifa = mDevice.getSsdpPacket().getInterfaceAddress();
        sb.append(ifa.getAddress().getHostAddress());
        sb.append(':');
        final int port = mDevice.getControlPoint().getEventPort();
        sb.append(String.valueOf(port));
        sb.append('/');
        sb.append(mDevice.getUdn());
        sb.append('/');
        sb.append(mServiceId);
        sb.append('>');
        return sb.toString();
    }

    public boolean subscribe() throws IOException {
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
            return false;
        }
        mSubscriptionId = response.getHeader(Http.SID);
        mDevice.getControlPoint().registerSubscribeService(this);
        return true;
    }

    public boolean renewSubscribe() throws IOException {
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
        return response.getStatus() == Http.Status.HTTP_OK;
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
            return false;
        }
        mDevice.getControlPoint().unregisterSubscribeService(this);
        mSubscriptionId = null;
        return true;
    }

    public String getSubscriptionId() {
        return mSubscriptionId;
    }
}
