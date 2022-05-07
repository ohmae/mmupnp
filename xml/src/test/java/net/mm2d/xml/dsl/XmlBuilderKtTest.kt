/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 *  http://opensource.org/licenses/MIT
 */

package net.mm2d.xml.dsl

import com.google.common.truth.Truth.assertThat
import net.mm2d.xml.node.XmlElement
import net.mm2d.xml.node.XmlTextNode
import org.junit.Test

@Suppress("NonAsciiCharacters")
class XmlBuilderKtTest {
    @Test(expected = IllegalArgumentException::class)
    fun `buildXml 空`() {
        buildXml {}
    }

    @Test(expected = IllegalArgumentException::class)
    fun `buildXml root複数`() {
        buildXml {
            "a" {}
            "b" {}
        }
    }

    @Test
    fun `buildXml 単一タグ`() {
        val a = buildXml {
            "a"("b" eq "c")
        }
        assertThat(a.localName).isEqualTo("a")
        assertThat(a.qName).isEqualTo("a")
        assertThat(a.prefix).isEmpty()
        assertThat(a.uri).isEmpty()
        assertThat(a.attributes).hasSize(1)
        val b = a.attributes.find { it.qName == "b" }!!
        assertThat(b.localName).isEqualTo("b")
        assertThat(b.qName).isEqualTo("b")
        assertThat(b.prefix).isEmpty()
        assertThat(b.uri).isEmpty()
        assertThat(b.value).isEqualTo("c")
        assertThat(b.parent).isSameInstanceAs(a)
        assertThat(a.value).isEmpty()
        assertThat(a.children).isEmpty()
    }

    @Test
    fun `buildXml 単一タグ+テキスト`() {
        val a = buildXml {
            "a"("b" eq "c") { "d" }
        }
        assertThat(a.localName).isEqualTo("a")
        assertThat(a.qName).isEqualTo("a")
        assertThat(a.prefix).isEmpty()
        assertThat(a.uri).isEmpty()
        assertThat(a.attributes).hasSize(1)
        val b = a.attributes.find { it.qName == "b" }!!
        assertThat(b.localName).isEqualTo("b")
        assertThat(b.qName).isEqualTo("b")
        assertThat(b.prefix).isEmpty()
        assertThat(b.uri).isEmpty()
        assertThat(b.value).isEqualTo("c")
        assertThat(b.parent).isSameInstanceAs(a)
        assertThat(a.value).isEqualTo("d")
        assertThat(a.children).hasSize(1)
        val text = a.children[0] as XmlTextNode
        assertThat(text.value).isEqualTo("d")
        assertThat(text.parent).isSameInstanceAs(a)
    }

    @Test
    fun `buildXml 子ノード1つ`() {
        val a = buildXml {
            "a"("b" eq "c") { "d" {} }
        }
        assertThat(a.localName).isEqualTo("a")
        assertThat(a.qName).isEqualTo("a")
        assertThat(a.prefix).isEmpty()
        assertThat(a.uri).isEmpty()
        assertThat(a.attributes).hasSize(1)
        val b = a.attributes.find { it.qName == "b" }!!
        assertThat(b.localName).isEqualTo("b")
        assertThat(b.qName).isEqualTo("b")
        assertThat(b.prefix).isEmpty()
        assertThat(b.uri).isEmpty()
        assertThat(b.value).isEqualTo("c")
        assertThat(b.parent).isSameInstanceAs(a)
        assertThat(a.value).isEmpty()
        assertThat(a.children).hasSize(1)
        val d = a.children[0] as XmlElement
        assertThat(d.localName).isEqualTo("d")
        assertThat(d.qName).isEqualTo("d")
        assertThat(d.prefix).isEmpty()
        assertThat(d.uri).isEmpty()
        assertThat(d.value).isEmpty()
        assertThat(d.attributes).isEmpty()
        assertThat(d.children).isEmpty()
    }

    @Test
    fun `buildXml 子ノード2つ`() {
        val a = buildXml {
            "a"("b" eq "c") {
                "d" {}
                "d" {}
            }
        }
        assertThat(a.localName).isEqualTo("a")
        assertThat(a.qName).isEqualTo("a")
        assertThat(a.prefix).isEmpty()
        assertThat(a.uri).isEmpty()
        assertThat(a.attributes).hasSize(1)
        val b = a.attributes.find { it.qName == "b" }!!
        assertThat(b.localName).isEqualTo("b")
        assertThat(b.qName).isEqualTo("b")
        assertThat(b.prefix).isEmpty()
        assertThat(b.uri).isEmpty()
        assertThat(b.value).isEqualTo("c")
        assertThat(b.parent).isSameInstanceAs(a)
        assertThat(a.value).isEmpty()
        assertThat(a.children).hasSize(2)
        val d1 = a.children[0] as XmlElement
        assertThat(d1.localName).isEqualTo("d")
        assertThat(d1.qName).isEqualTo("d")
        assertThat(d1.prefix).isEmpty()
        assertThat(d1.uri).isEmpty()
        assertThat(d1.value).isEmpty()
        assertThat(d1.attributes).isEmpty()
        assertThat(d1.children).isEmpty()
        val d2 = a.children[1] as XmlElement
        assertThat(d2.localName).isEqualTo("d")
        assertThat(d2.qName).isEqualTo("d")
        assertThat(d2.prefix).isEmpty()
        assertThat(d2.uri).isEmpty()
        assertThat(d2.value).isEmpty()
        assertThat(d2.attributes).isEmpty()
        assertThat(d2.children).isEmpty()
        assertThat(d1).isNotSameInstanceAs(d2)
    }

    @Test
    fun `buildXml 単一タグ+ネームスペース`() {
        val a = buildXml {
            ("a:a" ns "uri")("a:b" ns "uri" eq "c")
        }
        assertThat(a.localName).isEqualTo("a")
        assertThat(a.qName).isEqualTo("a:a")
        assertThat(a.prefix).isEqualTo("a")
        assertThat(a.uri).isEqualTo("uri")
        assertThat(a.attributes).hasSize(2)
        assertThat(a.prefixMap).hasSize(1)
        assertThat(a.prefixMap["a"]).isEqualTo("uri")
        assertThat(a.children).isEmpty()
        val b = a.attributes.find { it.localName == "b" }!!
        assertThat(b.localName).isEqualTo("b")
        assertThat(b.qName).isEqualTo("a:b")
        assertThat(b.prefix).isEqualTo("a")
        assertThat(b.uri).isEqualTo("uri")
        assertThat(b.value).isEqualTo("c")
        assertThat(b.parent).isSameInstanceAs(a)
        assertThat(a.value).isEmpty()
        val ns = a.attributes.find { it.prefix == "xmlns" }!!
        assertThat(ns.qName).isEqualTo("xmlns:a")
        assertThat(ns.localName).isEqualTo("a")
        assertThat(ns.prefix).isEqualTo("xmlns")
        assertThat(ns.value).isEqualTo("uri")
        assertThat(b.parent).isSameInstanceAs(a)
    }

    @Test
    fun `buildXml 単一タグ+プレフィックスなしネームスペース`() {
        val a = buildXml {
            ("a" ns "uri")("a:b" ns "uri" eq "c")
        }
        assertThat(a.localName).isEqualTo("a")
        assertThat(a.qName).isEqualTo("a")
        assertThat(a.prefix).isEqualTo("")
        assertThat(a.uri).isEqualTo("uri")
        assertThat(a.attributes).hasSize(3)
        assertThat(a.prefixMap).hasSize(2)
        assertThat(a.prefixMap[""]).isEqualTo("uri")
        assertThat(a.prefixMap["a"]).isEqualTo("uri")
        assertThat(a.children).isEmpty()
        val b = a.attributes.find { it.localName == "b" }!!
        assertThat(b.localName).isEqualTo("b")
        assertThat(b.qName).isEqualTo("a:b")
        assertThat(b.prefix).isEqualTo("a")
        assertThat(b.uri).isEqualTo("uri")
        assertThat(b.value).isEqualTo("c")
        assertThat(b.parent).isSameInstanceAs(a)
        assertThat(a.value).isEmpty()
        val ns1 = a.attributes.find { it.prefix == "xmlns" }!!
        assertThat(ns1.qName).isEqualTo("xmlns:a")
        assertThat(ns1.localName).isEqualTo("a")
        assertThat(ns1.prefix).isEqualTo("xmlns")
        assertThat(ns1.value).isEqualTo("uri")
        assertThat(b.parent).isSameInstanceAs(a)
        val ns2 = a.attributes.find { it.localName == "xmlns" }!!
        assertThat(ns2.qName).isEqualTo("xmlns")
        assertThat(ns2.localName).isEqualTo("xmlns")
        assertThat(ns2.prefix).isEqualTo("")
        assertThat(ns2.value).isEqualTo("uri")
        assertThat(b.parent).isSameInstanceAs(a)
    }

    @Test
    fun `buildXml 子タグにネームスペース`() {
        val a = buildXml {
            "a" { ("a:b" ns "uri") {} }
        }
        assertThat(a.localName).isEqualTo("a")
        assertThat(a.qName).isEqualTo("a")
        assertThat(a.prefix).isEqualTo("")
        assertThat(a.uri).isEqualTo("")
        assertThat(a.attributes).hasSize(0)
        assertThat(a.prefixMap).hasSize(0)
        val b = a.children.first() as XmlElement
        assertThat(b.localName).isEqualTo("b")
        assertThat(b.qName).isEqualTo("a:b")
        assertThat(b.prefix).isEqualTo("a")
        assertThat(b.uri).isEqualTo("uri")
        assertThat(b.value).isEqualTo("")
        assertThat(b.parent).isSameInstanceAs(a)
        assertThat(b.attributes).hasSize(1)
        assertThat(b.prefixMap).hasSize(1)
        assertThat(b.prefixMap["a"]).isEqualTo("uri")
        assertThat(a.value).isEmpty()
        val ns = b.attributes.find { it.prefix == "xmlns" }!!
        assertThat(ns.qName).isEqualTo("xmlns:a")
        assertThat(ns.localName).isEqualTo("a")
        assertThat(ns.prefix).isEqualTo("xmlns")
        assertThat(ns.value).isEqualTo("uri")
        assertThat(b.parent).isSameInstanceAs(a)
    }

    @Test
    fun `buildXml toXmlString`() {
        assertThat(
            buildXml { "a" {} }.buildString()
        ).isEqualTo(
            """<a/>"""
        )
        assertThat(
            buildXml { "a" { "b" } }.buildString()
        ).isEqualTo(
            """<a>b</a>"""
        )
        assertThat(
            buildXml { "a" { "b" {} } }.buildString()
        ).isEqualTo(
            """<a><b/></a>"""
        )
        assertThat(
            buildXml { "a"("b" eq "c") {} }.buildString()
        ).isEqualTo(
            """<a b="c"/>"""
        )
        assertThat(
            buildXml { ("a" ns "uri")("a:b" ns "uri" eq "c") {} }.buildString()
        ).isEqualTo(
            """<a xmlns="uri" xmlns:a="uri" a:b="c"/>"""
        )
        assertThat(
            buildXml { "a" { ("a:b" ns "uri") {} } }.buildString()
        ).isEqualTo(
            """<a><a:b xmlns:a="uri"/></a>"""
        )
    }

    @Test
    fun `buildXml toXmlString format`() {
        val element = buildXml {
            "a"("x" eq "y") {
                "b" {
                    "c"
                }
                "b" {
                    "c"
                }
            }
        }
        assertThat(
            element.buildString()
        ).isEqualTo(
            """<a x="y"><b>c</b><b>c</b></a>"""
        )
        assertThat(
            element.buildString(indent = false, withDeclaration = true)
        ).isEqualTo(
            """<?xml version="1.0" encoding="utf-8"?><a x="y"><b>c</b><b>c</b></a>"""
        )
        assertThat(
            element.buildString(indent = true, withDeclaration = false)
        ).isEqualTo(
            """
            <a x="y">
                <b>c</b>
                <b>c</b>
            </a>

            """.trimIndent()
        )
        assertThat(
            element.buildString(indent = true, withDeclaration = true)
        ).isEqualTo(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <a x="y">
                <b>c</b>
                <b>c</b>
            </a>

            """.trimIndent()
        )
    }

    @Test
    fun `buildXml toXmlString escape`() {
        assertThat(
            buildXml { "a"("x" eq "<>&'\"") { "<>&'\"" } }.buildString()
        ).isEqualTo(
            """<a x="&lt;&gt;&amp;&apos;&quot;">&lt;&gt;&amp;'"</a>"""
        )
        assertThat(
            buildXml { "a" { "\uD840\uDC0B" } }.buildString()
        ).isEqualTo(
            """<a>&#x2000b;</a>"""
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `buildXml toXmlString escape Invalid UTF-16 surrogate1`() {
        buildXml { "a" { "\uD840" } }.buildString()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `buildXml toXmlString escape Invalid UTF-16 surrogate2`() {
        buildXml { "a" { "\uD840a" } }.buildString()
    }
}
