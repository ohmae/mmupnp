/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.mm2d.upnp.Http
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.internal.server.Address
import net.mm2d.upnp.internal.server.SsdpSearchServer
import net.mm2d.upnp.util.TestUtils
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@Suppress("TestFunctionName", "NonAsciiCharacters", "ClassName")
@RunWith(Enclosed::class)
internal object SsdpRequestTest {

    private fun makeFromResource(name: String): SsdpRequest {
        val data = TestUtils.getResourceAsByteArray(name)
        return SsdpRequest.create(mockk(relaxed = true), data, data.size)
    }

    @RunWith(JUnit4::class)
    class 作成 {
        @Test
        fun buildUp_所望のバイナリに変換できる() {
            val message = SsdpRequest.create()
            message.method = SsdpMessage.M_SEARCH
            message.uri = "*"
            message.setHeader(Http.HOST, Address.IP_V4.addressString)
            message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER)
            message.setHeader(Http.MX, "1")
            message.setHeader(Http.ST, SsdpSearchServer.ST_ROOTDEVICE)
            val baos = ByteArrayOutputStream()
            message.message.writeData(baos)
            val actual = baos.toByteArray()
            val expected = TestUtils.getResourceAsByteArray("ssdp-search-request.bin")

            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun buildUp_受信データから作成() {
            val message = makeFromResource("ssdp-notify-alive0.bin")
            val baos = ByteArrayOutputStream()
            message.message.writeData(baos)
            val actual = baos.toByteArray()
            val expected = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")

            assertThat(String(actual)).isEqualTo(String(expected))
        }

        @Test
        fun toString_messageのtoStringと等価() {
            val message = makeFromResource("ssdp-notify-alive0.bin")
            assertThat(message.toString()).isEqualTo(message.message.toString())
        }

        @Test
        fun isPinned() {
            val message = makeFromResource("ssdp-notify-alive0.bin")
            assertThat(message.isPinned).isFalse()
        }
    }

    @RunWith(JUnit4::class)
    class 個別パラメータ {
        @Test
        fun getType() {
            val message = makeFromResource("ssdp-notify-alive2.bin")
            assertThat(message.type).isEqualTo("urn:schemas-upnp-org:service:ContentDirectory:1")

        }

        @Test
        fun getExpireTime() {
            val beforeTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(300)
            val message = makeFromResource("ssdp-notify-alive2.bin")
            val afterTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(300)

            assertThat(message.expireTime).isAtLeast(beforeTime)
            assertThat(message.expireTime).isAtMost(afterTime)
        }
    }

    @RunWith(Theories::class)
    class Notifyメッセージ {
        @Theory
        fun getMethod_NOTIFYであること(message: SsdpRequest) {
            assertThat(message.method).isEqualTo(SsdpMessage.NOTIFY)
        }

        @Theory
        fun getUri_アスタリスクであること(message: SsdpRequest) {
            assertThat(message.uri).isEqualTo("*")
        }

        @Theory
        fun getUuid_記述の値であること(message: SsdpRequest) {
            assertThat(message.uuid).isEqualTo("uuid:01234567-89ab-cdef-0123-456789abcdef")
        }

        @Theory
        fun getHeader_HOST_SSDPのアドレスであること(message: SsdpRequest) {
            assertThat(message.getHeader(Http.HOST)).isEqualTo(Address.IP_V4.addressString)
        }

        companion object {
            @DataPoints
            @JvmField
            val message: Array<SsdpRequest> = arrayOf(
                makeFromResource("ssdp-notify-alive0.bin"),
                makeFromResource("ssdp-notify-alive1.bin"),
                makeFromResource("ssdp-notify-alive2.bin"),
                makeFromResource("ssdp-notify-byebye0.bin"),
                makeFromResource("ssdp-notify-byebye1.bin"),
                makeFromResource("ssdp-notify-byebye2.bin")
            )
        }
    }

    @RunWith(Theories::class)
    class Aliveメッセージ {
        @Theory
        fun getNts_NTSがAliveであること(message: SsdpRequest) {
            assertThat(message.nts).isEqualTo(SsdpMessage.SSDP_ALIVE)
        }

        @Theory
        fun getMaxAge_CACHE_CONTROLの値が取れること(message: SsdpRequest) {
            assertThat(message.maxAge).isEqualTo(300)
        }

        @Theory
        fun getLocation_Locationの値が取れること(message: SsdpRequest) {
            assertThat(message.location).isEqualTo("http://192.0.2.2:12345/device.xml")
        }

        companion object {
            @DataPoints
            @JvmField
            val message: Array<SsdpRequest> = arrayOf(
                makeFromResource("ssdp-notify-alive0.bin"),
                makeFromResource("ssdp-notify-alive1.bin"),
                makeFromResource("ssdp-notify-alive2.bin")
            )
        }
    }

    @RunWith(Theories::class)
    class ByeByeメッセージ {
        @Theory
        fun getNts_NTSがByebyeであること(message: SsdpRequest) {
            assertThat(message.nts).isEqualTo(SsdpMessage.SSDP_BYEBYE)
        }

        companion object {
            @DataPoints
            @JvmField
            val message: Array<SsdpRequest> = arrayOf(
                makeFromResource("ssdp-notify-byebye0.bin"),
                makeFromResource("ssdp-notify-byebye1.bin"),
                makeFromResource("ssdp-notify-byebye2.bin")
            )
        }
    }
}
