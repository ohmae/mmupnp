/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.server

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.mm2d.upnp.common.internal.message.SsdpResponse
import net.mm2d.upnp.util.TestUtils
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException
import java.net.InetAddress

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class SsdpMessageValidatorTest {
    @Throws(IOException::class)
    private fun makeFromResource(name: String): SsdpResponse {
        val data = TestUtils.getResourceAsByteArray(name)
        return SsdpResponse.create(mockk(relaxed = true), data, data.size)
    }

    @Test
    fun isInvalidLocation_アドレス一致() {
        val message = makeFromResource("ssdp-search-response0.bin")
        assertThat(message.hasInvalidLocation(InetAddress.getByName("192.0.2.2"))).isFalse()
    }

    @Test
    fun isInvalidLocation_http以外() {
        val message = makeFromResource("ssdp-search-response-invalid-location0.bin")
        assertThat(message.hasInvalidLocation(InetAddress.getByName("192.0.2.2"))).isTrue()
    }

    @Test
    fun isInvalidLocation_表記に問題() {
        val message = makeFromResource("ssdp-search-response-invalid-location1.bin")
        assertThat(message.hasInvalidLocation(InetAddress.getByName("192.0.2.2"))).isTrue()
    }

    @Test
    fun isInvalidLocation_locationなし() {
        val message = makeFromResource("ssdp-search-response-no-location.bin")
        assertThat(message.hasInvalidLocation(InetAddress.getByName("192.0.2.2"))).isTrue()
    }
}
