/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.InetAddress;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class SsdpServerTest {
    @Test
    public void parseAddress_ipv4() throws Exception {
        final String address = "192.0.2.1";
        assertThat(Address.parseAddress(address), is(InetAddress.getByName(address)));
    }

    @Test
    public void parseAddress_ipv6() throws Exception {
        final String address = "2001:db8::9abc";
        assertThat(Address.parseAddress(address), is(InetAddress.getByName(address)));
    }

    @Test(expected = AssertionError.class)
    public void parseAddress_error() throws Exception {
        final String address = "unknown_host";
        assertThat(Address.parseAddress(address), is(InetAddress.getByName(address)));
    }
}
