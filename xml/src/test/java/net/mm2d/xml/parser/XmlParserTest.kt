/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 *  http://opensource.org/licenses/MIT
 */

package net.mm2d.xml.parser

import com.google.common.truth.Truth.assertThat
import net.mm2d.xml.node.XmlTextNode
import org.junit.Test

class XmlParserTest {
    @Test
    fun parse() {
        val element = XmlParser.parse("""<a x="y"><b>c</b></a>""")!!
        assertThat(element.qName).isEqualTo("a")
        assertThat(element.attributes[0].qName).isEqualTo("x")
        assertThat(element.attributes[0].value).isEqualTo("y")
        assertThat(element.childElements[0].qName).isEqualTo("b")
        assertThat(element.childElements[0].value).isEqualTo("c")
        assertThat((element.childElements[0].children[0] as XmlTextNode).value).isEqualTo("c")
    }
}
