/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.util

import com.google.common.truth.Truth.assertThat
import net.mm2d.upnp.util.TestUtils
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
        val element = root.findChildElementByLocalName("property")
        assertThat(element!!.tagName).isEqualTo("e:property")
    }

    @Test
    fun findChildElementByLocalName_not_found() {
        val xml = TestUtils.getResourceAsString("propchange.xml")
        val document = XmlUtils.newDocument(true, xml)
        val root = document.documentElement
        val element = root.findChildElementByLocalName("properties")
        assertThat(element).isNull()
    }

    @Test
    fun iterate_elements() {
        val xml = TestUtils.getResourceAsString("propchange.xml")
        val document = XmlUtils.newDocument(true, xml)
        val root = document.documentElement
        assertThat(root.firstChild.siblings().filterIsInstance<Element>())
            .isEqualTo(root.firstChild.siblingElements().toList())
    }

    @Test
    fun iterate_attributes() {
        val xml = """<item a="a" b="b" c="c" />"""
        val document = XmlUtils.newDocument(true, xml)
        val root = document.documentElement
        val map = root.attributes.asIterable()
            .map { it.nodeName to it.nodeValue }
            .toMap()
        assertThat(map).hasSize(3)
        assertThat(map["a"]).isEqualTo("a")
        assertThat(map["b"]).isEqualTo("b")
        assertThat(map["c"]).isEqualTo("c")
        root.attributes.forEach {
            assertThat(it.nodeName).isEqualTo(it.nodeValue)
        }
    }
}
