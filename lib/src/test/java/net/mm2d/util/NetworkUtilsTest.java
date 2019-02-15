/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import net.mm2d.util.NetworkUtils.NetworkInterfaceEnumeration;
import net.mm2d.util.NetworkUtils.NetworkInterfaceWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.annotation.Nonnull;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(Enclosed.class)
public class NetworkUtilsTest {
    @RunWith(JUnit4.class)
    public static class NetworkInterfaceをデバイスから取得 {
        @Test
        public void getAvailableInet4Interfaces() throws Exception {
            final List<NetworkInterface> list = NetworkUtils.getAvailableInet4Interfaces();
            for (final NetworkInterface ni : list) {
                assertThat(ni.isLoopback(), is(false));
                assertThat(ni.isUp(), is(true));
                assertThat(ni.supportsMulticast(), is(true));
                assertThat(hasInet4Address(ni), is(true));
            }
        }

        private static boolean hasInet4Address(@Nonnull final NetworkInterface netIf) {
            final List<InterfaceAddress> addresses = netIf.getInterfaceAddresses();
            for (final InterfaceAddress address : addresses) {
                if (address.getAddress() instanceof Inet4Address) {
                    return true;
                }
            }
            return false;
        }

        @Test
        public void getNetworkInterfaceList_Exceptionが発生しないこと() throws Exception {
            assertThat(NetworkUtils.getNetworkInterfaceList(), is(notNullValue()));
        }
    }

    @RunWith(JUnit4.class)
    public static class NetworkInterfaceの取得 {
        private Field mNetworkInterfaceEnumeration;
        private NetworkInterfaceEnumeration mOriginalNetworkInterfaceEnumeration;

        @Before
        public void setUp() throws Exception {
            mNetworkInterfaceEnumeration = NetworkUtils.class.getDeclaredField("sNetworkInterfaceEnumeration");
            mNetworkInterfaceEnumeration.setAccessible(true);
            mOriginalNetworkInterfaceEnumeration = (NetworkInterfaceEnumeration) mNetworkInterfaceEnumeration.get(null);
        }

        @After
        public void tearDown() throws Exception {
            mNetworkInterfaceEnumeration.set(null, mOriginalNetworkInterfaceEnumeration);
        }

        @Test
        public void getNetworkInterfaceList_Exceptionが発生すればemptyListが返る() throws Exception {
            final NetworkInterfaceEnumeration networkInterfaceEnumeration = mock(NetworkInterfaceEnumeration.class);
            doThrow(new SocketException()).when(networkInterfaceEnumeration).get();
            mNetworkInterfaceEnumeration.set(null, networkInterfaceEnumeration);
            assertThat(NetworkUtils.getNetworkInterfaceList(), empty());
        }

        @Test
        public void getNetworkInterfaceList_空のEnumerationが戻ればemptyListが返る() throws Exception {
            final Enumeration<NetworkInterface> enumeration = mock(Enumeration.class);
            doReturn(false).when(enumeration).hasMoreElements();
            doReturn(null).when(enumeration).nextElement();

            final NetworkInterfaceEnumeration networkInterfaceEnumeration = mock(NetworkInterfaceEnumeration.class);
            doReturn(enumeration).when(networkInterfaceEnumeration).get();

            mNetworkInterfaceEnumeration.set(null, networkInterfaceEnumeration);
            assertThat(NetworkUtils.getNetworkInterfaceList(), empty());
        }
    }

    @RunWith(JUnit4.class)
    public static class アドレス判定のテスト {
        private InterfaceAddress mIpv4Address;
        private InterfaceAddress mIpv6Address;

        @Before
        public void setUp() throws Exception {
            mIpv4Address = TestUtils.createInterfaceAddress("192.168.0.1", "255.255.255.0", 24);
            mIpv6Address = TestUtils.createInterfaceAddress("fe80::a831:801b:8dc6:421f", "255.255.0.0", 16);
        }

        @Test
        public void isAvailableInet4Interface_multicast可かつupかつloopbackでなくIPv4アドレスを持っている場合true() throws Exception {
            final NetworkInterfaceWrapper wrapper = mock(NetworkInterfaceWrapper.class);
            doReturn(Arrays.asList(mIpv4Address, mIpv6Address)).when(wrapper).getInterfaceAddresses();
            doReturn(false).when(wrapper).isLoopback();
            doReturn(true).when(wrapper).isUp();
            doReturn(true).when(wrapper).supportsMulticast();

            assertThat(NetworkUtils.isAvailableInet4Interface(wrapper), is(true));
        }

        @Test
        public void isAvailableInet4Interface_Interfaceの状態が合致しなければfalse() throws Exception {
            final NetworkInterfaceWrapper wrapper = mock(NetworkInterfaceWrapper.class);
            doReturn(Collections.singletonList(mIpv4Address)).when(wrapper).getInterfaceAddresses();
            doReturn(false).when(wrapper).isLoopback();
            doReturn(true).when(wrapper).isUp();
            doReturn(false).when(wrapper).supportsMulticast();

            assertThat(NetworkUtils.isAvailableInet4Interface(wrapper), is(false));

            final NetworkInterfaceWrapper wrapper1 = mock(NetworkInterfaceWrapper.class);
            doReturn(Collections.singletonList(mIpv4Address)).when(wrapper1).getInterfaceAddresses();
            doReturn(false).when(wrapper1).isLoopback();
            doReturn(false).when(wrapper1).isUp();
            doReturn(true).when(wrapper1).supportsMulticast();

            assertThat(NetworkUtils.isAvailableInet4Interface(wrapper1), is(false));

            final NetworkInterfaceWrapper wrapper2 = mock(NetworkInterfaceWrapper.class);
            doReturn(Collections.singletonList(mIpv4Address)).when(wrapper2).getInterfaceAddresses();
            doReturn(true).when(wrapper2).isLoopback();
            doReturn(true).when(wrapper2).isUp();
            doReturn(true).when(wrapper2).supportsMulticast();

            assertThat(NetworkUtils.isAvailableInet4Interface(wrapper2), is(false));
        }

        @Test
        public void isAvailableInet4Interface_Exceptionが発生した場合false() throws Exception {
            final NetworkInterfaceWrapper wrapper = mock(NetworkInterfaceWrapper.class);
            doReturn(Collections.singletonList(mIpv4Address)).when(wrapper).getInterfaceAddresses();
            doReturn(false).when(wrapper).isLoopback();
            doThrow(new SocketException()).when(wrapper).isUp();
            doReturn(false).when(wrapper).supportsMulticast();

            assertThat(NetworkUtils.isAvailableInet4Interface(wrapper), is(false));

            final NetworkInterfaceWrapper wrapper1 = mock(NetworkInterfaceWrapper.class);
            doReturn(Collections.singletonList(mIpv4Address)).when(wrapper1).getInterfaceAddresses();
            doReturn(false).when(wrapper1).isLoopback();
            doThrow(new SocketException()).when(wrapper1).isUp();
            doReturn(true).when(wrapper1).supportsMulticast();

            assertThat(NetworkUtils.isAvailableInet4Interface(wrapper1), is(false));

            final NetworkInterfaceWrapper wrapper2 = mock(NetworkInterfaceWrapper.class);
            doReturn(Collections.singletonList(mIpv4Address)).when(wrapper2).getInterfaceAddresses();
            doReturn(false).when(wrapper2).isLoopback();
            doReturn(true).when(wrapper2).isUp();
            doThrow(new SocketException()).when(wrapper2).supportsMulticast();

            assertThat(NetworkUtils.isAvailableInet4Interface(wrapper2), is(false));
        }

        @Test
        public void isAvailableInet4Interface_IPv4アドレスを持っていない場合false() throws Exception {
            final NetworkInterfaceWrapper wrapper = mock(NetworkInterfaceWrapper.class);
            doReturn(Collections.singletonList(mIpv6Address)).when(wrapper).getInterfaceAddresses();
            doReturn(false).when(wrapper).isLoopback();
            doReturn(true).when(wrapper).isUp();
            doReturn(true).when(wrapper).supportsMulticast();

            assertThat(NetworkUtils.isAvailableInet4Interface(wrapper), is(false));
        }

        @Test
        public void isAvailableInet6Interface_multicast可かつupかつloopbackでなくIPv6アドレスを持っている場合true() throws Exception {
            final NetworkInterfaceWrapper wrapper = mock(NetworkInterfaceWrapper.class);
            doReturn(Arrays.asList(mIpv4Address, mIpv6Address)).when(wrapper).getInterfaceAddresses();
            doReturn(false).when(wrapper).isLoopback();
            doReturn(true).when(wrapper).isUp();
            doReturn(true).when(wrapper).supportsMulticast();

            assertThat(NetworkUtils.isAvailableInet6Interface(wrapper), is(true));
        }

        @Test
        public void isAvailableInet6Interface_Interfaceの状態が合致しなければfalse() throws Exception {
            final NetworkInterfaceWrapper wrapper = mock(NetworkInterfaceWrapper.class);
            doReturn(Collections.singletonList(mIpv6Address)).when(wrapper).getInterfaceAddresses();
            doReturn(false).when(wrapper).isLoopback();
            doReturn(true).when(wrapper).isUp();
            doReturn(false).when(wrapper).supportsMulticast();

            assertThat(NetworkUtils.isAvailableInet6Interface(wrapper), is(false));

            final NetworkInterfaceWrapper wrapper1 = mock(NetworkInterfaceWrapper.class);
            doReturn(Collections.singletonList(mIpv6Address)).when(wrapper1).getInterfaceAddresses();
            doReturn(false).when(wrapper1).isLoopback();
            doReturn(false).when(wrapper1).isUp();
            doReturn(true).when(wrapper1).supportsMulticast();

            assertThat(NetworkUtils.isAvailableInet6Interface(wrapper1), is(false));

            final NetworkInterfaceWrapper wrapper2 = mock(NetworkInterfaceWrapper.class);
            doReturn(Collections.singletonList(mIpv6Address)).when(wrapper2).getInterfaceAddresses();
            doReturn(true).when(wrapper2).isLoopback();
            doReturn(true).when(wrapper2).isUp();
            doReturn(true).when(wrapper2).supportsMulticast();

            assertThat(NetworkUtils.isAvailableInet6Interface(wrapper2), is(false));
        }

        @Test
        public void isAvailableInet6Interface_Exceptionが発生した場合false() throws Exception {
            final NetworkInterfaceWrapper wrapper = mock(NetworkInterfaceWrapper.class);
            doReturn(Collections.singletonList(mIpv6Address)).when(wrapper).getInterfaceAddresses();
            doReturn(false).when(wrapper).isLoopback();
            doThrow(new SocketException()).when(wrapper).isUp();
            doReturn(false).when(wrapper).supportsMulticast();

            assertThat(NetworkUtils.isAvailableInet6Interface(wrapper), is(false));

            final NetworkInterfaceWrapper wrapper1 = mock(NetworkInterfaceWrapper.class);
            doReturn(Collections.singletonList(mIpv6Address)).when(wrapper1).getInterfaceAddresses();
            doReturn(false).when(wrapper1).isLoopback();
            doThrow(new SocketException()).when(wrapper1).isUp();
            doReturn(true).when(wrapper1).supportsMulticast();

            assertThat(NetworkUtils.isAvailableInet6Interface(wrapper1), is(false));

            final NetworkInterfaceWrapper wrapper2 = mock(NetworkInterfaceWrapper.class);
            doReturn(Collections.singletonList(mIpv6Address)).when(wrapper2).getInterfaceAddresses();
            doReturn(false).when(wrapper2).isLoopback();
            doReturn(true).when(wrapper2).isUp();
            doThrow(new SocketException()).when(wrapper2).supportsMulticast();

            assertThat(NetworkUtils.isAvailableInet6Interface(wrapper2), is(false));
        }

        @Test
        public void isAvailableInet6Interface_IPv6アドレスを持っていない場合false() throws Exception {
            final NetworkInterfaceWrapper wrapper = mock(NetworkInterfaceWrapper.class);
            doReturn(Collections.singletonList(mIpv4Address)).when(wrapper).getInterfaceAddresses();
            doReturn(false).when(wrapper).isLoopback();
            doReturn(true).when(wrapper).isUp();
            doReturn(true).when(wrapper).supportsMulticast();

            assertThat(NetworkUtils.isAvailableInet6Interface(wrapper), is(false));
        }
    }

    @RunWith(JUnit4.class)
    public static class アドレス変換のテスト {
        @Test
        public void toNormalizedString_manual() throws Exception {
            assertThat(NetworkUtils.toNormalizedString((Inet6Address) InetAddress.getByName("0:0:0:0:0:0:0:0")), is("::"));
            assertThat(NetworkUtils.toNormalizedString((Inet6Address) InetAddress.getByName("1:0:0:0:0:0:0:0")), is("1::"));
            assertThat(NetworkUtils.toNormalizedString((Inet6Address) InetAddress.getByName("0:0:0:0:0:0:0:1")), is("::1"));
            assertThat(NetworkUtils.toNormalizedString((Inet6Address) InetAddress.getByName("1:0:1:0:0:0:0:0")), is("1:0:1::"));
            assertThat(NetworkUtils.toNormalizedString((Inet6Address) InetAddress.getByName("0:0:0:0:0:1:0:1")), is("::1:0:1"));
            assertThat(NetworkUtils.toNormalizedString((Inet6Address) InetAddress.getByName("0:1:0:0:0:0:0:0")), is("0:1::"));
            assertThat(NetworkUtils.toNormalizedString((Inet6Address) InetAddress.getByName("0:0:0:0:0:0:1:0")), is("::1:0"));
            assertThat(NetworkUtils.toNormalizedString((Inet6Address) InetAddress.getByName("0:1:0:0:0:0:1:0")), is("0:1::1:0"));
            assertThat(NetworkUtils.toNormalizedString((Inet6Address) InetAddress.getByName("1:1:0:1:1:1:1:1")), is("1:1:0:1:1:1:1:1"));
            assertThat(NetworkUtils.toNormalizedString((Inet6Address) InetAddress.getByName("1:1:1:1:1:1:1:1")), is("1:1:1:1:1:1:1:1"));
        }

        @Test
        public void toNormalizedString_all() throws Exception {
            final byte[] bin = new byte[16];
            all(bin, 0);
        }

        private static void all(
                @Nonnull final byte[] bin,
                final int pos) throws Exception {
            if (pos >= 16) {
                final InetAddress addressBefore = InetAddress.getByAddress(bin);
                final InetAddress addressAfter = InetAddress.getByName(NetworkUtils.toNormalizedString((Inet6Address) addressBefore));
                assertThat(addressAfter, is(addressBefore));
                return;
            }
            final int[] sections = new int[]{0, 0x1, 0x11, 0x111, 0x1111};
            for (final int section : sections) {
                bin[pos] = (byte) ((section >> 8) & 0xff);
                bin[pos + 1] = (byte) (section & 0xff);
                all(bin, pos + 2);
            }
        }
    }
}