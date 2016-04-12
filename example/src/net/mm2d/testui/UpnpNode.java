/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.testui;

import net.mm2d.util.Log;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class UpnpNode extends DefaultMutableTreeNode {
    private static final String TAG = "UpnpNode";

    public UpnpNode() {
        super();
    }

    public UpnpNode(Object userObject) {
        super(userObject);
    }

    public UpnpNode(Object userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    public String getDetailText() {
        return "";
    }

    protected String formatXml(String xml) {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document doc = db.parse(new ByteArrayInputStream(xml.getBytes("utf-8")));
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            final StringWriter sw = new StringWriter();
            t.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (IllegalArgumentException | ParserConfigurationException | SAXException
                | IOException | TransformerFactoryConfigurationError | TransformerException e) {
            Log.w(TAG, e);
        }
        return "";
    }

    public String getDetailXml() {
        return "";
    }
}
