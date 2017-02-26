/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.List;

import javax.annotation.Nonnull;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class NetworkUtilsTest {
    @Test
    public void getAvailableInet4Interfaces() throws Exception {
        final List<NetworkInterface> list = NetworkUtils.getAvailableInet4Interfaces();
        for (NetworkInterface ni : list) {
            assertThat(ni.isLoopback(), is(false));
            assertThat(ni.isUp(), is(true));
            assertThat(hasInet4Address(ni), is(true));
        }
    }

    private static boolean hasInet4Address(@Nonnull NetworkInterface netIf) {
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