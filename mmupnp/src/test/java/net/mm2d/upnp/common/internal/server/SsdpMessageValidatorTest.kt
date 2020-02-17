/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.server

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.mm2d.upnp.common.internal.message.SsdpResponse
import net.mm2d.upnp.util.TestUtils
import net.mm2d.upnp.util.createInterfaceAddress
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

    @Test
    fun invalidAddress_IPv4() {
        var interfaceAddress = createInterfaceAddress("192.168.0.1", "255.255.255.0", 24)

        assertThat(InetAddress.getByName("192.168.0.255").isInvalidAddress(Address.IP_V4, interfaceAddress, true)).isFalse()
        assertThat(InetAddress.getByName("192.168.0.255").isInvalidAddress(Address.IP_V4, interfaceAddress, false)).isFalse()

        interfaceAddress = createInterfaceAddress("192.168.0.1", "255.255.255.128", 25)

        assertThat(InetAddress.getByName("192.168.0.255").isInvalidAddress(Address.IP_V4, interfaceAddress, true)).isTrue()
        assertThat(InetAddress.getByName("192.168.0.255").isInvalidAddress(Address.IP_V4, interfaceAddress, false)).isFalse()

        interfaceAddress = createInterfaceAddress("192.168.0.1", "255.255.255.0", 24)

        assertThat(InetAddress.getByName("192.168.1.255").isInvalidAddress(Address.IP_V4, interfaceAddress, true)).isTrue()

        interfaceAddress = createInterfaceAddress("192.168.0.1", "255.255.254.0", 23)

        assertThat(InetAddress.getByName("192.168.1.255").isInvalidAddress(Address.IP_V4, interfaceAddress, true)).isFalse()

        assertThat(InetAddress.getByName("fe80::a831:801b:8dc6:421f").isInvalidAddress(Address.IP_V4, interfaceAddress, true)).isTrue()
        assertThat(InetAddress.getByName("fe80::a831:801b:8dc6:421f").isInvalidAddress(Address.IP_V4, interfaceAddress, false)).isTrue()
    }

    @Test
    fun invalidAddress_IPv6() {
        var interfaceAddress = createInterfaceAddress("fe80::a831:801b:8dc6:421f", "255.255.0.0", 16)
        assertThat(InetAddress.getByName("2001:db8::1").isInvalidAddress(Address.IP_V6, interfaceAddress, true)).isFalse()
        assertThat(InetAddress.getByName("2001:db8::1").isInvalidAddress(Address.IP_V6, interfaceAddress, false)).isFalse()

        assertThat(InetAddress.getByName("192.168.0.255").isInvalidAddress(Address.IP_V6, interfaceAddress, true)).isTrue()
        assertThat(InetAddress.getByName("192.168.0.255").isInvalidAddress(Address.IP_V6, interfaceAddress, false)).isTrue()
    }
}
