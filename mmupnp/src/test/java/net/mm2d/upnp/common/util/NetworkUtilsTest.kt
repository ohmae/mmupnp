/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.util

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import net.mm2d.upnp.util.createInterfaceAddress
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.net.*
import java.util.*

@Suppress("TestFunctionName", "NonAsciiCharacters", "ClassName")
@RunWith(Enclosed::class)
class NetworkUtilsTest {
    @RunWith(JUnit4::class)
    class NetworkInterfaceをデバイスから取得 {
        @Test
        fun availableInterfaces() {
            NetworkUtils.getAvailableInterfaces().forEach { nif ->
                assertThat(nif.isLoopback).isFalse()
                assertThat(nif.isUp).isTrue()
                assertThat(nif.supportsMulticast()).isTrue()
            }
        }

        @Test
        fun availableInet4Interfaces() {
            NetworkUtils.getAvailableInet4Interfaces().forEach { nif ->
                assertThat(nif.isLoopback).isFalse()
                assertThat(nif.isUp).isTrue()
                assertThat(nif.supportsMulticast()).isTrue()
                assertThat(nif.interfaceAddresses.any { it.address is Inet4Address }).isTrue()
            }
        }

        @Test
        fun availableInet6Interfaces() {
            NetworkUtils.getAvailableInet6Interfaces().forEach { nif ->
                assertThat(nif.isLoopback).isFalse()
                assertThat(nif.isUp).isTrue()
                assertThat(nif.supportsMulticast()).isTrue()
                assertThat(nif.interfaceAddresses.any { it.address is Inet6Address }).isTrue()
            }
        }

        @Test
        fun findInet4Address() {
            val ipv4 = createInterfaceAddress("192.168.0.1", "255.255.255.0", 24)
            val ipv6 = createInterfaceAddress("fe80::a831:801b:8dc6:421f", "255.255.0.0", 16)
            val ni: NetworkInterface = mockk()
            every { ni.interfaceAddresses } returns listOf(ipv4, ipv6)
            assertThat(ni.findInet4Address()).isEqualTo(ipv4)
            every { ni.interfaceAddresses } returns listOf(ipv6, ipv4)
            assertThat(ni.findInet4Address()).isEqualTo(ipv4)
        }

        @Test(expected = IllegalArgumentException::class)
        fun findInet4Address_見つからなければException1() {
            val ipv6 = createInterfaceAddress("fe80::a831:801b:8dc6:421f", "255.255.0.0", 16)
            val ni: NetworkInterface = mockk()
            every { ni.interfaceAddresses } returns listOf(ipv6, ipv6)
            ni.findInet4Address()
        }

        @Test(expected = IllegalArgumentException::class)
        fun findInet4Address_見つからなければException2() {
            val ni: NetworkInterface = mockk()
            every { ni.interfaceAddresses } returns emptyList()
            ni.findInet4Address()
        }

        @Test
        fun findInet6Address() {
            val ipv4 = createInterfaceAddress("192.168.0.1", "255.255.255.0", 24)
            val ipv6 = createInterfaceAddress("fe80::a831:801b:8dc6:421f", "255.255.0.0", 16)
            val ni: NetworkInterface = mockk()
            every { ni.interfaceAddresses } returns listOf(ipv4, ipv6)
            assertThat(ni.findInet6Address()).isEqualTo(ipv6)
            every { ni.interfaceAddresses } returns listOf(ipv6, ipv4)
            assertThat(ni.findInet6Address()).isEqualTo(ipv6)
        }

        @Test(expected = IllegalArgumentException::class)
        fun findInet6Address_見つからなければException1() {
            val ipv4 = createInterfaceAddress("192.168.0.1", "255.255.255.0", 24)
            val ni: NetworkInterface = mockk()
            every { ni.interfaceAddresses } returns listOf(ipv4)
            ni.findInet6Address()
        }

        @Test(expected = IllegalArgumentException::class)
        fun findInet6Address_見つからなければException2() {
            val ni: NetworkInterface = mockk()
            every { ni.interfaceAddresses } returns emptyList()
            ni.findInet6Address()
        }
    }

    @RunWith(JUnit4::class)
    class NetworkInterfaceの取得 {
        @Before
        fun setUp() {
            mockkStatic(NetworkInterface::class)
        }

        @After
        fun teardown() {
            unmockkStatic(NetworkInterface::class)
        }

        @Test
        fun `networkInterfaces nullになることはない`() {
            assertThat(NetworkUtils.getNetworkInterfaceList()).isNotNull()
        }

        @Test
        fun getNetworkInterfaceList_Exceptionが発生すればemptyListが返る() {
            every { NetworkInterface.getNetworkInterfaces() } throws SocketException()
            assertThat(NetworkUtils.getNetworkInterfaceList()).isEmpty()
        }

        @Test
        fun getNetworkInterfaceList_nullが戻ればemptyListが返る() {
            every { NetworkInterface.getNetworkInterfaces() } returns null
            assertThat(NetworkUtils.getNetworkInterfaceList()).isEmpty()
        }

        @Test
        fun getNetworkInterfaceList_空のEnumerationが戻ればemptyListが返る() {
            val enumeration: Enumeration<NetworkInterface> = mockk()
            every { enumeration.hasMoreElements() } returns false
            every { enumeration.nextElement() } returns null
            every { NetworkInterface.getNetworkInterfaces() } returns enumeration
            assertThat(NetworkUtils.getNetworkInterfaceList()).isEmpty()
        }
    }

    @RunWith(JUnit4::class)
    class アドレス判定のテスト {
        private val ipv4Address = createInterfaceAddress("192.168.0.1", "255.255.255.0", 24)
        private val ipv6Address = createInterfaceAddress("fe80::a831:801b:8dc6:421f", "255.255.0.0", 16)

        @Test
        fun isAvailableInet4Interface_multicast可かつupかつloopbackでなくIPv4アドレスを持っている場合true() {
            val nif = mockk<NetworkInterface>()
            every { nif.interfaceAddresses } returns listOf(ipv4Address, ipv6Address)
            every { nif.isLoopback } returns false
            every { nif.isUp } returns true
            every { nif.supportsMulticast() } returns true
            assertThat(nif.isAvailableInet4Interface()).isTrue()
        }

        @Test
        fun isAvailableInet4Interface_Interfaceの状態が合致しなければfalse() {
            val nif = mockk<NetworkInterface>()
            every { nif.interfaceAddresses } returns listOf(ipv4Address)
            every { nif.isLoopback } returns false
            every { nif.isUp } returns true
            every { nif.supportsMulticast() } returns false
            assertThat(nif.isAvailableInet4Interface()).isFalse()

            val nif1 = mockk<NetworkInterface>()
            every { nif1.interfaceAddresses } returns listOf(ipv4Address)
            every { nif1.isLoopback } returns false
            every { nif1.isUp } returns false
            every { nif1.supportsMulticast() } returns true
            assertThat(nif1.isAvailableInet4Interface()).isFalse()

            val nif2 = mockk<NetworkInterface>()
            every { nif2.interfaceAddresses } returns listOf(ipv4Address)
            every { nif2.isLoopback } returns true
            every { nif2.isUp } returns true
            every { nif2.supportsMulticast() } returns true
            assertThat(nif2.isAvailableInet4Interface()).isFalse()
        }

        @Test
        fun isAvailableInet4Interface_Exceptionが発生した場合false() {
            val nif = mockk<NetworkInterface>()
            every { nif.interfaceAddresses } returns listOf(ipv4Address)
            every { nif.isLoopback } returns false
            every { nif.isUp } throws SocketException()
            every { nif.supportsMulticast() } returns false
            assertThat(nif.isAvailableInet4Interface()).isFalse()

            val nif1 = mockk<NetworkInterface>()
            every { nif1.interfaceAddresses } returns listOf(ipv4Address)
            every { nif1.isLoopback } returns false
            every { nif1.isUp } throws SocketException()
            every { nif1.supportsMulticast() } returns true
            assertThat(nif1.isAvailableInet4Interface()).isFalse()

            val nif2 = mockk<NetworkInterface>()
            every { nif2.interfaceAddresses } returns listOf(ipv4Address)
            every { nif2.isLoopback } returns false
            every { nif2.isUp } returns true
            every { nif2.supportsMulticast() } throws SocketException()
            assertThat(nif2.isAvailableInet4Interface()).isFalse()
        }

        @Test
        fun isAvailableInet4Interface_IPv4アドレスを持っていない場合false() {
            val nif = mockk<NetworkInterface>()
            every { nif.interfaceAddresses } returns listOf(ipv6Address)
            every { nif.isLoopback } returns false
            every { nif.isUp } returns true
            every { nif.supportsMulticast() } returns true
            assertThat(nif.isAvailableInet4Interface()).isFalse()
        }

        @Test
        fun isAvailableInet6Interface_multicast可かつupかつloopbackでなくIPv6アドレスを持っている場合true() {
            val nif = mockk<NetworkInterface>()
            every { nif.interfaceAddresses } returns listOf(ipv4Address, ipv6Address)
            every { nif.isLoopback } returns false
            every { nif.isUp } returns true
            every { nif.supportsMulticast() } returns true
            assertThat(nif.isAvailableInet6Interface()).isTrue()
        }

        @Test
        fun isAvailableInet6Interface_Interfaceの状態が合致しなければfalse() {
            val nif = mockk<NetworkInterface>()
            every { nif.interfaceAddresses } returns listOf(ipv6Address)
            every { nif.isLoopback } returns false
            every { nif.isUp } returns true
            every { nif.supportsMulticast() } returns false
            assertThat(nif.isAvailableInet6Interface()).isFalse()

            val nif1 = mockk<NetworkInterface>()
            every { nif1.interfaceAddresses } returns listOf(ipv6Address)
            every { nif1.isLoopback } returns false
            every { nif1.isUp } returns false
            every { nif1.supportsMulticast() } returns true
            assertThat(nif1.isAvailableInet6Interface()).isFalse()

            val nif2 = mockk<NetworkInterface>()
            every { nif2.interfaceAddresses } returns listOf(ipv6Address)
            every { nif2.isLoopback } returns true
            every { nif2.isUp } returns true
            every { nif2.supportsMulticast() } returns true
            assertThat(nif2.isAvailableInet6Interface()).isFalse()
        }

        @Test
        fun isAvailableInet6Interface_Exceptionが発生した場合false() {
            val nif = mockk<NetworkInterface>()
            every { nif.interfaceAddresses } returns listOf(ipv6Address)
            every { nif.isLoopback } returns false
            every { nif.isUp } throws SocketException()
            every { nif.supportsMulticast() } returns false
            assertThat(nif.isAvailableInet6Interface()).isFalse()

            val nif1 = mockk<NetworkInterface>()
            every { nif1.interfaceAddresses } returns listOf(ipv6Address)
            every { nif1.isLoopback } returns false
            every { nif1.isUp } throws SocketException()
            every { nif1.supportsMulticast() } returns true
            assertThat(nif1.isAvailableInet6Interface()).isFalse()

            val nif2 = mockk<NetworkInterface>()
            every { nif2.interfaceAddresses } returns listOf(ipv6Address)
            every { nif2.isLoopback } returns false
            every { nif2.isUp } returns true
            every { nif2.supportsMulticast() } throws SocketException()
            assertThat(nif2.isAvailableInet6Interface()).isFalse()
        }

        @Test
        fun isAvailableInet6Interface_IPv6アドレスを持っていない場合false() {
            val nif = mockk<NetworkInterface>()
            every { nif.interfaceAddresses } returns listOf(ipv4Address)
            every { nif.isLoopback } returns false
            every { nif.isUp } returns true
            every { nif.supportsMulticast() } returns true
            assertThat(nif.isAvailableInet6Interface()).isFalse()
        }

        @Test
        fun isAvailableInterface() {
            val nif = mockk<NetworkInterface>()
            every { nif.interfaceAddresses } returns listOf()
            every { nif.isLoopback } returns false
            every { nif.isUp } returns true
            every { nif.supportsMulticast() } returns true
            assertThat(nif.isAvailableInterface()).isFalse()
            every { nif.interfaceAddresses } returns listOf(ipv4Address)
            assertThat(nif.isAvailableInterface()).isTrue()
            every { nif.interfaceAddresses } returns listOf(ipv6Address)
            assertThat(nif.isAvailableInterface()).isTrue()
        }

        @Test
        fun isAvailableInet6Address() {
            val address: Inet6Address = mockk(relaxed = true)
            every { address.isLinkLocalAddress } returns false
            assertThat(address.isAvailableInet6Address()).isFalse()
            every { address.isLinkLocalAddress } returns true
            assertThat(address.isAvailableInet6Address()).isTrue()
            assertThat(mockk<Inet4Address>().isAvailableInet6Address()).isFalse()
        }
    }

    @RunWith(JUnit4::class)
    class アドレス変換のテスト {
        @Test
        fun toNormalizedString_manual() {
            assertThat("0:0:0:0:0:0:0:0".toInet6Address().toNormalizedString()).isEqualTo("::")
            assertThat("1:0:0:0:0:0:0:0".toInet6Address().toNormalizedString()).isEqualTo("1::")
            assertThat("0:0:0:0:0:0:0:1".toInet6Address().toNormalizedString()).isEqualTo("::1")
            assertThat("1:0:1:0:0:0:0:0".toInet6Address().toNormalizedString()).isEqualTo("1:0:1::")
            assertThat("0:0:0:0:0:1:0:1".toInet6Address().toNormalizedString()).isEqualTo("::1:0:1")
            assertThat("0:1:0:0:0:0:0:0".toInet6Address().toNormalizedString()).isEqualTo("0:1::")
            assertThat("0:0:0:0:0:0:1:0".toInet6Address().toNormalizedString()).isEqualTo("::1:0")
            assertThat("0:1:0:0:0:0:1:0".toInet6Address().toNormalizedString()).isEqualTo("0:1::1:0")
            assertThat("1:1:1:1:1:1:1:1".toInet6Address().toNormalizedString()).isEqualTo("1:1:1:1:1:1:1:1")
            assertThat("fe80::ffff:ffff".toInet6Address().toNormalizedString()).isEqualTo("fe80::ffff:ffff")
        }

        private fun String.toInet6Address(): Inet6Address {
            return InetAddress.getByName(this) as Inet6Address
        }

        @Test
        fun toNormalizedString_all() {
            all(ByteArray(16), 0)
        }

        private fun all(bin: ByteArray, pos: Int) {
            if (pos >= 16) {
                val address = InetAddress.getByAddress(bin) as Inet6Address
                assertThat(InetAddress.getByName(address.toNormalizedString()))
                    .isEqualTo(address)
                return
            }
            val sections = intArrayOf(0, 0x1, 0x11, 0x111, 0x1111)
            for (section in sections) {
                bin[pos] = (section shr 8 and 0xff).toByte()
                bin[pos + 1] = (section and 0xff).toByte()
                all(bin, pos + 2)
            }
        }

        @Test
        fun toAddressString() {
            assertThat(InetSocketAddress("192.168.0.1", 80).toAddressString())
                .isEqualTo("192.168.0.1")
            assertThat(InetSocketAddress("192.168.0.1", 0).toAddressString())
                .isEqualTo("192.168.0.1")
            assertThat(InetSocketAddress("192.168.0.1", 8080).toAddressString())
                .isEqualTo("192.168.0.1:8080")
            assertThat(InetSocketAddress("2001:db8:1::1", 80).toAddressString())
                .isEqualTo("[2001:db8:1::1]")
            assertThat(InetSocketAddress("2001:db8:1::1", 0).toAddressString())
                .isEqualTo("[2001:db8:1::1]")
            assertThat(InetSocketAddress("2001:db8:1::1", 8080).toAddressString())
                .isEqualTo("[2001:db8:1::1]:8080")
        }
    }
}
