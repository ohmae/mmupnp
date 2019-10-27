/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.message

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.mm2d.upnp.common.Http
import net.mm2d.upnp.util.TestUtils
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayOutputStream

@Suppress("TestFunctionName", "NonAsciiCharacters", "ClassName")
@RunWith(Enclosed::class)
internal object SsdpResponseTest {

    private fun makeFromResource(name: String): SsdpResponse {
        val data = TestUtils.getResourceAsByteArray(name)
        return SsdpResponse.create(mockk(), data, data.size)
    }

    @RunWith(JUnit4::class)
    class 作成 {
        @Test
        fun buildUp_受信データから作成() {
            val message =
                makeFromResource("ssdp-search-response0.bin")

            val baos = ByteArrayOutputStream()
            message.message.writeData(baos)
            val actual = baos.toByteArray()

            val expected = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin")

            assertThat(String(actual)).isEqualTo(String(expected))
        }

        @Test
        fun setStatusCode() {
            val message =
                makeFromResource("ssdp-search-response0.bin")
            message.setStatusCode(404)
            assertThat(message.getStatusCode()).isEqualTo(404)
        }

        @Test
        fun setReasonPhrase() {
            val message =
                makeFromResource("ssdp-search-response0.bin")
            message.setReasonPhrase("Not Found")
            assertThat(message.getReasonPhrase()).isEqualTo("Not Found")
        }

        @Test
        fun setStatus() {
            val message =
                makeFromResource("ssdp-search-response0.bin")
            message.setStatus(Http.Status.HTTP_NOT_FOUND)
            assertThat(message.getStatus()).isEqualTo(Http.Status.HTTP_NOT_FOUND)
        }

        @Test
        fun isPinned() {
            val message =
                makeFromResource("ssdp-search-response0.bin")
            assertThat(message.isPinned).isEqualTo(false)
        }
    }

    @RunWith(Theories::class)
    class Responseメッセージ {
        @Theory
        fun getStatus(message: SsdpResponse) {
            assertThat(message.getStatus()).isEqualTo(Http.Status.HTTP_OK)
        }

        @Theory
        fun getStatusCode(message: SsdpResponse) {
            assertThat(message.getStatusCode()).isEqualTo(200)
        }

        @Theory
        fun getReasonPhrase(message: SsdpResponse) {
            assertThat(message.getReasonPhrase()).isEqualTo("OK")
        }

        @Theory
        fun getUuid_記述の値であること(message: SsdpResponse) {
            assertThat(message.uuid).isEqualTo("uuid:01234567-89ab-cdef-0123-456789abcdef")
        }

        @Theory
        fun getMaxAge_CACHE_CONTROLの値が取れること(message: SsdpResponse) {
            assertThat(message.maxAge).isEqualTo(300)
        }

        @Theory
        fun getLocation_Locationの値が取れること(message: SsdpResponse) {
            assertThat(message.location).isEqualTo("http://192.0.2.2:12345/device.xml")
        }

        companion object {
            @DataPoints
            @JvmField
            val messages: Array<SsdpResponse> = arrayOf(
                makeFromResource("ssdp-search-response0.bin"),
                makeFromResource("ssdp-search-response1.bin")
            )
        }
    }
}
