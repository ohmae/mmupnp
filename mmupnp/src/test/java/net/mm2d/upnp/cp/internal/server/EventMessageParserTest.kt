/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.server

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class EventMessageParserTest {
    @Test
    fun parsePropertyPairs_中身が空なら空のリスト() {
        val message: String? = ""
        assertThat(message.parseEventXml()).isEmpty()
    }

    @Test
    fun parsePropertyPairs_nullなら空のリスト() {
        val message: String? = null
        assertThat(message.parseEventXml()).isEmpty()
    }

    @Test
    fun parsePropertyPairs_rootがpropertysetでない場合リスト() {
        val message = "<e:property xmlns:e=\"urn:schemas-upnp-org:event-1-0\">\n" +
            "<e:property>\n" +
            "<SystemUpdateID>0</SystemUpdateID>\n" +
            "</e:property>\n" +
            "<e:property>\n" +
            "<ContainerUpdateIDs></ContainerUpdateIDs>\n" +
            "</e:property>\n" +
            "</e:property>"

        assertThat(message.parseEventXml()).isEmpty()
    }

    @Test
    fun parsePropertyPairs_property以外の要素は無視() {
        val message = "<e:propertyset xmlns:e=\"urn:schemas-upnp-org:event-1-0\">\n" +
            "<e:property>\n" +
            "<SystemUpdateID>0</SystemUpdateID>\n" +
            "</e:property>\n" +
            "<e:proper>\n" +
            "<ContainerUpdateIDs></ContainerUpdateIDs>\n" +
            "</e:proper>\n" +
            "</e:propertyset>"

        assertThat(message.parseEventXml()).hasSize(1)
    }

    @Test
    fun parsePropertyPairs_xml異常() {
        val message = "<e:propertyset xmlns:e=\"urn:schemas-upnp-org:event-1-0\">\n" +
            "<e:property>\n" +
            "<>0</>\n" +
            "</e:property>\n" +
            "<e:property>\n" +
            "<ContainerUpdateIDs></ContainerUpdateIDs>\n" +
            "</e:property>\n" +
            "</e:propertyset>"

        assertThat(message.parseEventXml()).isEmpty()
    }
}
