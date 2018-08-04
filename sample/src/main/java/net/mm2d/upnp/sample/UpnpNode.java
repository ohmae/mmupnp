/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample;


import net.mm2d.log.Log;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.awt.Component;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

import javax.swing.JFrame;
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
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class UpnpNode extends DefaultMutableTreeNode {
    UpnpNode(final Object userObject) {
        super(userObject);
    }

    public UpnpNode(
            final Object userObject,
            final boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    public String getDetailText() {
        return "";
    }

    protected String formatXml(final String xml) {
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
        } catch (final IllegalArgumentException | ParserConfigurationException | SAXException
                | IOException | TransformerFactoryConfigurationError | TransformerException e) {
            Log.w(e);
        }
        return "";
    }

    public String getDetailXml() {
        return "";
    }

    public void showContextMenu(
            final JFrame frame,
            final Component invoker,
            final int x,
            final int y) {
    }
}
