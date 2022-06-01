/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message

import com.google.common.truth.Truth.assertThat
import net.mm2d.upnp.SingleHttpRequest
import net.mm2d.upnp.util.TestUtils
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream
import java.net.Inet6Address
import java.net.InetAddress

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class SsdpMessageDelegateTest {
    @Test
    fun getScopeId_インターフェース指定がなければ0() {
        val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")
        val request = SingleHttpRequest.create()
        request.readData(ByteArrayInputStream(data, 0, data.size))
        val delegate = SsdpMessageDelegate(request)

        assertThat(delegate.scopeId).isEqualTo(0)
    }

    @Test
    fun getScopeId_インターフェースIPv4なら0() {
        val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")
        val request = SingleHttpRequest.create()
        request.readData(ByteArrayInputStream(data, 0, data.size))
        val delegate = SsdpMessageDelegate(request, InetAddress.getByName("192.0.2.3"))

        assertThat(delegate.scopeId).isEqualTo(0)
    }

    @Test
    fun getScopeId_インターフェースに紐付かないIPv6なら0() {
        val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")
        val request = SingleHttpRequest.create()
        request.readData(ByteArrayInputStream(data, 0, data.size))
        val delegate = SsdpMessageDelegate(request, InetAddress.getByName("fe80::a831:801b:8dc6:421f"))

        assertThat(delegate.scopeId).isEqualTo(0)
    }

    @Test
    fun getScopeId_インターフェースに紐付くIPv6ならその値() {
        val scopeId = 1
        val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")
        val request = SingleHttpRequest.create()
        request.readData(ByteArrayInputStream(data, 0, data.size))
        val address =
            Inet6Address.getByAddress(null, InetAddress.getByName("fe80::a831:801b:8dc6:421f").address, scopeId)
        val delegate = SsdpMessageDelegate(request, address)

        assertThat(delegate.scopeId).isEqualTo(scopeId)
    }

    companion object {
        private const val DEFAULT_MAX_AGE = 1800
    }
}
