/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
@RunWith(JUnit4.class)
public class XmlUtilsTest {
    @Test
    public void constuctor() {
        new XmlUtils();
    }

    @Test
    public void getDocumentBuilder_NS_not_aware() throws Exception {
        final String xml = TestUtils.getResourceAsString("propchange.xml");
        final Document document = XmlUtils.newDocument(false, xml);
        final Element root = document.getDocumentElement();
        assertThat(root.getTagName(), is("e:propertyset"));
        assertThat(root.getNamespaceURI(), is(nullValue()));
    }

    @Test
    public void getDocumentBuilder_NS_aware() throws Exception {
        final String xml = TestUtils.getResourceAsString("propchange.xml");
        final Document document = XmlUtils.newDocument(true, xml);
        final Element root = document.getDocumentElement();
        assertThat(root.getTagName(), is("e:propertyset"));
        assertThat(root.getNamespaceURI(), is("urn:schemas-upnp-org:event-1-0"));
    }

    @Test
    public void findChildElementByLocalName() throws Exception {
        final String xml = TestUtils.getResourceAsString("propchange.xml");
        final Document document = XmlUtils.newDocument(true, xml);
        final Element root = document.getDocumentElement();
        final Element element = XmlUtils.findChildElementByLocalName(root, "property");
        assertThat(element.getTagName(), is("e:property"));
    }

    @Test
    public void findChildElementByLocalName_not_found() throws Exception {
        final String xml = TestUtils.getResourceAsString("propchange.xml");
        final Document document = XmlUtils.newDocument(true, xml);
        final Element root = document.getDocumentElement();
        final Element element = XmlUtils.findChildElementByLocalName(root, "propertys");
        assertThat(element, is(nullValue()));
    }
}
