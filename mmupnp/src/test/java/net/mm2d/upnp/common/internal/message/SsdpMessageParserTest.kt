/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.message

import com.google.common.truth.Truth.assertThat
import net.mm2d.upnp.common.Http
import net.mm2d.upnp.common.HttpResponse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class SsdpMessageParserTest {
    @Test
    fun parseCacheControl_正常() {
        val message = HttpResponse.create()
        message.setHeader(Http.CACHE_CONTROL, "max-age=100")
        assertThat(message.parseCacheControl()).isEqualTo(100)
    }

    @Test
    fun parseCacheControl_空() {
        val message = HttpResponse.create()
        assertThat(message.parseCacheControl()).isEqualTo(DEFAULT_MAX_AGE)
    }

    @Test
    fun parseCacheControl_max_ageから始まらない() {
        val message = HttpResponse.create()
        message.setHeader(Http.CACHE_CONTROL, "age=100")
        assertThat(message.parseCacheControl()).isEqualTo(DEFAULT_MAX_AGE)
    }

    @Test
    fun parseCacheControl_デリミタが違う() {
        val message = HttpResponse.create()
        message.setHeader(Http.CACHE_CONTROL, "max-age:100")
        assertThat(message.parseCacheControl()).isEqualTo(DEFAULT_MAX_AGE)
    }

    @Test
    fun parseCacheControl_数値がない() {
        val message = HttpResponse.create()
        message.setHeader(Http.CACHE_CONTROL, "max-age=")

        assertThat(message.parseCacheControl()).isEqualTo(DEFAULT_MAX_AGE)
    }

    @Test
    fun parseCacheControl_10進数でない() {
        val message = HttpResponse.create()
        message.setHeader(Http.CACHE_CONTROL, "max-age=ff")
        assertThat(message.parseCacheControl()).isEqualTo(DEFAULT_MAX_AGE)
    }

    @Test
    fun parseUsn_正常1() {
        val message = HttpResponse.create()
        message.setHeader(Http.USN, "uuid:01234567-89ab-cdef-0123-456789abcdef::upnp:rootdevice")
        val (uuid, type) = message.parseUsn()

        assertThat(uuid).isEqualTo("uuid:01234567-89ab-cdef-0123-456789abcdef")
        assertThat(type).isEqualTo("upnp:rootdevice")
    }

    @Test
    fun parseUsn_正常2() {
        val message = HttpResponse.create()
        message.setHeader(Http.USN, "uuid:01234567-89ab-cdef-0123-456789abcdef")
        val (uuid, type) = message.parseUsn()

        assertThat(uuid).isEqualTo("uuid:01234567-89ab-cdef-0123-456789abcdef")
        assertThat(type).isEqualTo("")
    }

    @Test
    fun parseUsn_空() {
        val message = HttpResponse.create()
        val (uuid, type) = message.parseUsn()

        assertThat(uuid).isEqualTo("")
        assertThat(type).isEqualTo("")
    }

    @Test
    fun parseUsn_uuidでない() {
        val message = HttpResponse.create()
        message.setHeader(Http.USN, "01234567-89ab-cdef-0123-456789abcdef")
        val (uuid, type) = message.parseUsn()

        assertThat(uuid).isEqualTo("")
        assertThat(type).isEqualTo("")
    }
}
