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

public class FakeSsdpMessageTest {
    private static final String LOCATION = "http://127.0.0.1/";
    private FakeSsdpMessage mFakeSsdpMessage;

    @Before
    public void setUp() {
        mFakeSsdpMessage = new FakeSsdpMessage(LOCATION);
    }

    @Test
    public void isPinned() {
        SsdpMessage message = new FakeSsdpMessage("", LOCATION, true);
        assertThat(message.isPinned(), is(true));
        message = new FakeSsdpMessage("", LOCATION, false);
        assertThat(message.isPinned(), is(false));
    }

    @Test
    public void getScopeId() {
        assertThat(mFakeSsdpMessage.getScopeId(), is(0));
    }

    @Test
    public void setLocalAddress() throws Exception {
        final InetAddress address = InetAddress.getByName("127.0.0.1");
        mFakeSsdpMessage.setLocalAddress(address);
        assertThat(mFakeSsdpMessage.getLocalAddress(), is(address));
    }

    @Test
    public void getLocalAddress() {
        assertThat(mFakeSsdpMessage.getLocalAddress(), is(nullValue()));
    }

    @Test
    public void getHeader() {
        assertThat(mFakeSsdpMessage.getHeader(""), is(nullValue()));
    }

    @Test
    public void setHeader() {
        mFakeSsdpMessage.setHeader("", "");
    }

    @Test
    public void setUuid() {
        final String uuid = "uuid";
        mFakeSsdpMessage.setUuid(uuid);
        assertThat(mFakeSsdpMessage.getUuid(), is(uuid));
    }

    @Test
    public void getUuid() {
        assertThat(mFakeSsdpMessage.getUuid(), isEmptyString());
    }

    @Test
    public void getType() {
        assertThat(mFakeSsdpMessage.getType(), isEmptyString());
    }

    @Test
    public void getNts() {
        assertThat(mFakeSsdpMessage.getNts(), Matchers.is(SsdpMessage.SSDP_ALIVE));
    }

    @Test
    public void getMaxAge() {
        assertThat(mFakeSsdpMessage.getMaxAge(), is(Integer.MAX_VALUE));
    }

    @Test
    public void getExpireTime() {
        assertThat(mFakeSsdpMessage.getExpireTime(), is(Long.MAX_VALUE));
    }

    @Test
    public void getLocation() {
        assertThat(mFakeSsdpMessage.getLocation(), is(LOCATION));
    }

    @Test
    public void writeData() {
        mFakeSsdpMessage.writeData(mock(OutputStream.class));
    }
}
