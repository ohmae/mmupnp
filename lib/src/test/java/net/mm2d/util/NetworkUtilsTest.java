/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import net.mm2d.upnp.Http;
import net.mm2d.util.NetworkUtils.NetworkInterfaceEnumeration;
import net.mm2d.util.NetworkUtils.NetworkInterfaceWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.annotation.Nonnull;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class NetworkUtilsTest {
    @RunWith(JUnit4.class)
    public static class NetworkInterfaceをデバイスから取得 {
        @Test(expected = InvocationTargetException.class)
        public void constructor() throws Exception {
            final Constructor<NetworkUtils> constructor = NetworkUtils.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        }

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
            mNetworkInterfaceEnumeration.set(null, new NetworkInterfaceEnumeration() {
                @Override
                Enumeration<NetworkInterface> get() throws SocketException {
                    throw new SocketException();
                }
            });
            assertThat(NetworkUtils.getNetworkInterfaceList(), empty());
        }

        @Test
        public void getNetworkInterfaceList_空のEnumerationが戻ればemptyListが返る() throws Exception {
            mNetworkInterfaceEnumeration.set(null, new NetworkInterfaceEnumeration() {
                @Override
                Enumeration<NetworkInterface> get() throws SocketException {
                    return new Enumeration<NetworkInterface>() {
                        @Override
                        public boolean hasMoreElements() {
                            return false;
                        }

                        @Override
                        public NetworkInterface nextElement() {
                            return null;
                        }
                    };
                }
            });
            assertThat(NetworkUtils.getNetworkInterfaceList(), empty());
        }
    }

    @RunWith(JUnit4.class)
    public static class アドレス判定のテスト {
        private InterfaceAddress mIpv4Address;
        private InterfaceAddress mIpv6Address;

        @Before
        public void setUp() throws Exception {
            mIpv4Address = createInterfaceAddress("192.168.0.1", "255.255.255.0", (short) 24);
            mIpv6Address = createInterfaceAddress("fe80::a831:801b:8dc6:421f", "255.255.0.0", (short) 16);
        }

        @Test
        public void isAvailableInet4Interface_multicast可かつupかつloopbackでなくIPv4アドレスを持っている場合true() {
            final NetworkInterfaceWrapper wrapper = new NetworkInterfaceWrapper() {
                @Override
                public List<InterfaceAddress> getInterfaceAddresses() {
                    return Arrays.asList(mIpv4Address, mIpv6Address);
                }

                @Override
                public boolean isLoopback() throws SocketException {
                    return false;
                }

                @Override
                public boolean isUp() throws SocketException {
                    return true;
                }

                @Override
                public boolean supportsMulticast() throws SocketException {
                    return true;
                }
            };

            assertThat(NetworkUtils.isAvailableInet4Interface(wrapper), is(true));
        }

        @Test
        public void isAvailableInet4Interface_Interfaceの状態が合致しなければfalse() {
            assertThat(NetworkUtils.isAvailableInet4Interface(new NetworkInterfaceWrapper() {
                @Override
                public List<InterfaceAddress> getInterfaceAddresses() {
                    return Arrays.asList(mIpv4Address);
                }

                @Override
                public boolean isLoopback() throws SocketException {
                    return false;
                }

                @Override
                public boolean isUp() throws SocketException {
                    return true;
                }

                @Override
                public boolean supportsMulticast() throws SocketException {
                    return false;
                }
            }), is(false));

            assertThat(NetworkUtils.isAvailableInet4Interface(new NetworkInterfaceWrapper() {
                @Override
                public List<InterfaceAddress> getInterfaceAddresses() {
                    return Arrays.asList(mIpv4Address);
                }

                @Override
                public boolean isLoopback() throws SocketException {
                    return false;
                }

                @Override
                public boolean isUp() throws SocketException {
                    return false;
                }

                @Override
                public boolean supportsMulticast() throws SocketException {
                    return true;
                }
            }), is(false));

            assertThat(NetworkUtils.isAvailableInet4Interface(new NetworkInterfaceWrapper() {
                @Override
                public List<InterfaceAddress> getInterfaceAddresses() {
                    return Arrays.asList(mIpv4Address);
                }

                @Override
                public boolean isLoopback() throws SocketException {
                    return true;
                }

                @Override
                public boolean isUp() throws SocketException {
                    return true;
                }

                @Override
                public boolean supportsMulticast() throws SocketException {
                    return true;
                }
            }), is(false));
        }

        @Test
        public void isAvailableInet4Interface_Exceptionが発生した場合false() {
            assertThat(NetworkUtils.isAvailableInet4Interface(new NetworkInterfaceWrapper() {
                @Override
                public List<InterfaceAddress> getInterfaceAddresses() {
                    return Arrays.asList(mIpv4Address);
                }

                @Override
                public boolean isLoopback() throws SocketException {
                    throw new SocketException();
                }

                @Override
                public boolean isUp() throws SocketException {
                    return true;
                }

                @Override
                public boolean supportsMulticast() throws SocketException {
                    return true;
                }
            }), is(false));

            assertThat(NetworkUtils.isAvailableInet4Interface(new NetworkInterfaceWrapper() {
                @Override
                public List<InterfaceAddress> getInterfaceAddresses() {
                    return Arrays.asList(mIpv4Address);
                }

                @Override
                public boolean isLoopback() throws SocketException {
                    return false;
                }

                @Override
                public boolean isUp() throws SocketException {
                    throw new SocketException();
                }

                @Override
                public boolean supportsMulticast() throws SocketException {
                    return true;
                }
            }), is(false));

            assertThat(NetworkUtils.isAvailableInet4Interface(new NetworkInterfaceWrapper() {
                @Override
                public List<InterfaceAddress> getInterfaceAddresses() {
                    return Arrays.asList(mIpv4Address);
                }

                @Override
                public boolean isLoopback() throws SocketException {
                    return false;
                }

                @Override
                public boolean isUp() throws SocketException {
                    return true;
                }

                @Override
                public boolean supportsMulticast() throws SocketException {
                    throw new SocketException();
                }
            }), is(false));
        }

        @Test
        public void isAvailableInet4Interface_IPv4アドレスを持っていない場合false() {
            final NetworkInterfaceWrapper wrapper = new NetworkInterfaceWrapper() {
                @Override
                public List<InterfaceAddress> getInterfaceAddresses() {
                    return Arrays.asList(mIpv6Address);
                }

                @Override
                public boolean isLoopback() throws SocketException {
                    return false;
                }

                @Override
                public boolean isUp() throws SocketException {
                    return true;
                }

                @Override
                public boolean supportsMulticast() throws SocketException {
                    return true;
                }
            };

            assertThat(NetworkUtils.isAvailableInet4Interface(wrapper), is(false));
        }

        private static InterfaceAddress createInterfaceAddress(
                final String address,
                final String broadcast,
                final short maskLength)
                throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, InstantiationException, UnknownHostException {
            final Class<InterfaceAddress> cls = InterfaceAddress.class;
            final Constructor<InterfaceAddress> constructor = cls.getDeclaredConstructor();
            constructor.setAccessible(true);
            final InterfaceAddress interfaceAddress = constructor.newInstance();
            final Field fAddress = cls.getDeclaredField("address");
            fAddress.setAccessible(true);
            fAddress.set(interfaceAddress, InetAddress.getByName(address));
            final Field fBroadcast = cls.getDeclaredField("broadcast");
            fBroadcast.setAccessible(true);
            fBroadcast.set(interfaceAddress, InetAddress.getByName(broadcast));
            final Field fMaskLength = cls.getDeclaredField("maskLength");
            fMaskLength.setAccessible(true);
            fMaskLength.setShort(interfaceAddress, maskLength);
            return interfaceAddress;
        }
    }
}