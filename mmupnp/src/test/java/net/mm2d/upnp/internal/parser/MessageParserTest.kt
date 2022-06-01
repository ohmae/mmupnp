/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.parser

import com.google.common.truth.Truth.assertThat
import net.mm2d.upnp.Http
import net.mm2d.upnp.SingleHttpResponse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class MessageParserTest {
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

    @Test
    fun parseCacheControl_正常() {
        val message = SingleHttpResponse.create()
        message.setHeader(Http.CACHE_CONTROL, "max-age=100")
        assertThat(message.parseCacheControl()).isEqualTo(100)
    }

    @Test
    fun parseCacheControl_空() {
        val message = SingleHttpResponse.create()
        assertThat(message.parseCacheControl()).isEqualTo(DEFAULT_MAX_AGE)
    }

    @Test
    fun parseCacheControl_max_ageから始まらない() {
        val message = SingleHttpResponse.create()
        message.setHeader(Http.CACHE_CONTROL, "age=100")
        assertThat(message.parseCacheControl()).isEqualTo(DEFAULT_MAX_AGE)
    }

    @Test
    fun parseCacheControl_デリミタが違う() {
        val message = SingleHttpResponse.create()
        message.setHeader(Http.CACHE_CONTROL, "max-age:100")
        assertThat(message.parseCacheControl()).isEqualTo(DEFAULT_MAX_AGE)
    }

    @Test
    fun parseCacheControl_数値がない() {
        val message = SingleHttpResponse.create()
        message.setHeader(Http.CACHE_CONTROL, "max-age=")

        assertThat(message.parseCacheControl()).isEqualTo(DEFAULT_MAX_AGE)
    }

    @Test
    fun parseCacheControl_10進数でない() {
        val message = SingleHttpResponse.create()
        message.setHeader(Http.CACHE_CONTROL, "max-age=ff")
        assertThat(message.parseCacheControl()).isEqualTo(DEFAULT_MAX_AGE)
    }

    @Test
    fun parseUsn_正常1() {
        val message = SingleHttpResponse.create()
        message.setHeader(Http.USN, "uuid:01234567-89ab-cdef-0123-456789abcdef::upnp:rootdevice")
        val (uuid, type) = message.parseUsn()

        assertThat(uuid).isEqualTo("uuid:01234567-89ab-cdef-0123-456789abcdef")
        assertThat(type).isEqualTo("upnp:rootdevice")
    }

    @Test
    fun parseUsn_正常2() {
        val message = SingleHttpResponse.create()
        message.setHeader(Http.USN, "uuid:01234567-89ab-cdef-0123-456789abcdef")
        val (uuid, type) = message.parseUsn()

        assertThat(uuid).isEqualTo("uuid:01234567-89ab-cdef-0123-456789abcdef")
        assertThat(type).isEqualTo("")
    }

    @Test
    fun parseUsn_空() {
        val message = SingleHttpResponse.create()
        val (uuid, type) = message.parseUsn()

        assertThat(uuid).isEqualTo("")
        assertThat(type).isEqualTo("")
    }

    @Test
    fun parseUsn_uuidでない() {
        val message = SingleHttpResponse.create()
        message.setHeader(Http.USN, "01234567-89ab-cdef-0123-456789abcdef")
        val (uuid, type) = message.parseUsn()

        assertThat(uuid).isEqualTo("")
        assertThat(type).isEqualTo("")
    }
}
