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
            "a"("b" eq "c") { "d"{} }
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
                "d"{}
                "d"{}
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
        println(a.buildXml())
        assertThat(a.localName).isEqualTo("a")
        assertThat(a.qName).isEqualTo("a:a")
        assertThat(a.prefix).isEqualTo("a")
        assertThat(a.uri).isEqualTo("uri")
        assertThat(a.attributes).hasSize(2)
        assertThat(a.prefixMap).hasSize(1)
        val b = a.attributes.find { it.localName == "b" }!!
        assertThat(b.localName).isEqualTo("b")
        assertThat(b.qName).isEqualTo("a:b")
        assertThat(b.prefix).isEqualTo("a")
        assertThat(b.uri).isEqualTo("uri")
        assertThat(b.value).isEqualTo("c")
        assertThat(b.parent).isSameInstanceAs(a)
        assertThat(a.value).isEmpty()
        assertThat(a.children).isEmpty()
    }
}
