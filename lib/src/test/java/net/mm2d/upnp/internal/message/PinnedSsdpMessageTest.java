/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message;

import net.mm2d.upnp.SsdpMessage;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;
import java.net.InetAddress;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PinnedSsdpMessageTest {
    private static final String LOCATION = "http://127.0.0.1/";
    private PinnedSsdpMessage mPinnedSsdpMessage;

    @Before
    public void setUp() throws Exception {
        mPinnedSsdpMessage = new PinnedSsdpMessage(LOCATION);
    }

    @Test
    public void getScopeId() {
        assertThat(mPinnedSsdpMessage.getScopeId(), is(0));
    }

    @Test
    public void setLocalAddress() throws Exception {
        final InetAddress address = InetAddress.getByName("127.0.0.1");
        mPinnedSsdpMessage.setLocalAddress(address);
        assertThat(mPinnedSsdpMessage.getLocalAddress(), is(address));
    }

    @Test
    public void getLocalAddress() {
        assertThat(mPinnedSsdpMessage.getLocalAddress(), is(nullValue()));
    }

    @Test
    public void getHeader() {
        assertThat(mPinnedSsdpMessage.getHeader(""), is(nullValue()));
    }

    @Test
    public void setHeader() {
        mPinnedSsdpMessage.setHeader("", "");
    }

    @Test
    public void setUuid() {
        final String uuid = "uuid";
        mPinnedSsdpMessage.setUuid(uuid);
        assertThat(mPinnedSsdpMessage.getUuid(), is(uuid));
    }

    @Test
    public void getUuid() {
        assertThat(mPinnedSsdpMessage.getUuid(), isEmptyString());
    }

    @Test
    public void getType() {
        assertThat(mPinnedSsdpMessage.getType(), isEmptyString());
    }

    @Test
    public void getNts() {
        assertThat(mPinnedSsdpMessage.getNts(), Matchers.is(SsdpMessage.SSDP_ALIVE));
    }

    @Test
    public void getMaxAge() {
        assertThat(mPinnedSsdpMessage.getMaxAge(), is(Integer.MAX_VALUE));
    }

    @Test
    public void getExpireTime() {
        assertThat(mPinnedSsdpMessage.getExpireTime(), is(Long.MAX_VALUE));
    }

    @Test
    public void getLocation() {
        assertThat(mPinnedSsdpMessage.getLocation(), is(LOCATION));
    }

    @Test
    public void writeData() {
        mPinnedSsdpMessage.writeData(mock(OutputStream.class));
    }
}
