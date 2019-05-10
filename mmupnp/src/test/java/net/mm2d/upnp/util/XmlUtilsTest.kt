/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.w3c.dom.Element

@RunWith(JUnit4::class)
class XmlUtilsTest {
    @Test
    fun getDocumentBuilder_NS_not_aware() {
        val xml = TestUtils.getResourceAsString("propchange.xml")
        val document = XmlUtils.newDocument(false, xml)
        val root = document.documentElement
        assertThat(root.tagName).isEqualTo("e:propertyset")
        assertThat(root.namespaceURI).isNull()
    }

    @Test
    fun getDocumentBuilder_NS_aware() {
        val xml = TestUtils.getResourceAsString("propchange.xml")
        val document = XmlUtils.newDocument(true, xml)
        val root = document.documentElement
        assertThat(root.tagName).isEqualTo("e:propertyset")
        assertThat(root.namespaceURI).isEqualTo("urn:schemas-upnp-org:event-1-0")
    }

    @Test
    fun findChildElementByLocalName() {
        val xml = TestUtils.getResourceAsString("propchange.xml")
        val document = XmlUtils.newDocument(true, xml)
        val root = document.documentElement
        val element = XmlUtils.findChildElementByLocalName(root, "property")
        assertThat(element!!.tagName).isEqualTo("e:property")
    }

    @Test
    fun findChildElementByLocalName_not_found() {
        val xml = TestUtils.getResourceAsString("propchange.xml")
        val document = XmlUtils.newDocument(true, xml)
        val root = document.documentElement
        val element = XmlUtils.findChildElementByLocalName(root, "properties")
        assertThat(element).isNull()
    }

    @Test
    fun iterator() {
        val xml = TestUtils.getResourceAsString("propchange.xml")
        val document = XmlUtils.newDocument(true, xml)
        val root = document.documentElement
        assertThat(root.firstChild.siblings().filter { it is Element })
            .isEqualTo(root.firstChild.siblingElements().toList())
    }
}
