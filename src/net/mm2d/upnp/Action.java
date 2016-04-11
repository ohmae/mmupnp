/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class Action {
    private final Service mService;
    private final String mName;
    private List<Argument> mArgumentList;
    private final Map<String, Argument> mArgumentMap;
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String SOAP_STYLE = "http://schemas.xmlsoap.org/soap/encoding/";

    public static class Builder {
        private Service mService;
        private String mName;
        private final List<Argument.Builder> mArgumentList;

        public Builder() {
            mArgumentList = new ArrayList<>();
        }

        public void serService(Service service) {
            mService = service;
        }

        public void setName(String name) {
            mName = name;
        }

        public void addArugmentBuilder(Argument.Builder argument) {
            mArgumentList.add(argument);
        }

        public List<Argument.Builder> getArgumentBuilderList() {
            return mArgumentList;
        }

        public Action build() {
            return new Action(this);
        }
    }

    private Action(Builder builder) {
        mService = builder.mService;
        mName = builder.mName;
        mArgumentMap = new LinkedHashMap<>();
        for (final Argument.Builder b : builder.mArgumentList) {
            b.setAction(this);
            final Argument argument = b.build();
            mArgumentMap.put(argument.getName(), argument);
        }
    }

    public Service getService() {
        return mService;
    }

    public String getName() {
        return mName;
    }

    public List<Argument> getArgumentList() {
        if (mArgumentList == null) {
            mArgumentList = new ArrayList<>(mArgumentMap.values());
        }
        return Collections.unmodifiableList(mArgumentList);
    }

    private String getSoapActionName() {
        return '"' + mService.getServiceType()
                + '#' + mName + '"';
    }

    public Map<String, String> invoke(Map<String, String> arguments)
            throws IOException {
        final String soap = makeSoap(arguments);
        if (soap == null) {
            return null;
        }
        final URL url = mService.getAbsoluteUrl(mService.getControlUrl());
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.POST);
        request.setUrl(url, true);
        request.setHeader(Http.SOAPACTION, getSoapActionName());
        request.setHeader(Http.USER_AGENT, Http.USER_AGENT_VALUE);
        request.setHeader(Http.CONNECTION, Http.CLOSE);
        request.setHeader(Http.CONTENT_TYPE, Http.CONTENT_TYPE_DEFAULT);
        request.setBody(soap, true);
        final HttpClient client = new HttpClient(false);
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK) {
            System.out.println(response.toString());
            throw new IOException();
        }
        try {
            return parseResponse(response.getBody());
        } catch (SAXException | ParserConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String makeSoap(Map<String, String> arguments) {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document doc = db.newDocument();
            final Element e = doc.createElementNS(SOAP_NS, "s:Envelope");
            doc.appendChild(e);
            final Attr a = doc.createAttributeNS(SOAP_NS, "s:encodingStyle");
            a.setNodeValue(SOAP_STYLE);
            e.setAttributeNode(a);
            final Element b = doc.createElementNS(SOAP_NS, "s:Body");
            e.appendChild(b);
            final Element action = doc.createElementNS(mService.getServiceType(), "u:" + mName);
            b.appendChild(action);
            for (final Entry<String, Argument> entry : mArgumentMap.entrySet()) {
                final Argument arg = entry.getValue();
                if (arg.isInputDirection()) {
                    final Element p = doc.createElement(arg.getName());
                    String value = arguments.get(arg.getName());
                    if (value == null) {
                        value = arg.getRelatedStateVariable().getDefaultValue();
                    }
                    if (value != null) {
                        p.setTextContent(value);
                    }
                    action.appendChild(p);
                }
            }
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer t = tf.newTransformer();
            final StringWriter sw = new StringWriter();
            t.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (DOMException
                | ParserConfigurationException
                | TransformerFactoryConfigurationError
                | TransformerException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Element findChildElementByName(Node node, String name) {
        Node child = node.getFirstChild();
        for (; child != null; child = child.getNextSibling()) {
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (name.equals(child.getLocalName())) {
                return (Element) child;
            }
        }
        return null;
    }

    private Map<String, String> parseResponse(String xml)
            throws IOException, SAXException, ParserConfigurationException {
        final String responseTag = mName + "Response";
        final Map<String, String> result = new HashMap<>();
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        final DocumentBuilder db = dbf.newDocumentBuilder();
        final Document doc = db.parse(new InputSource(new StringReader(xml)));
        final Element envelope = doc.getDocumentElement();
        final Element body = findChildElementByName(envelope, "Body");
        if (body == null) {
            System.out.println("body null");
            return result;
        }
        final Element response = findChildElementByName(body, responseTag);
        if (response == null) {
            System.out.println("response null");
            return result;
        }
        Node node = response.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            final String text = node.getTextContent();
            final Argument arg = mArgumentMap.get(tag);
            if (arg == null) {
                continue;
            }
            result.put(tag, text);
        }
        return result;
    }
}
