/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty;

import net.mm2d.upnp.SsdpMessage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.OutputStream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class EmptySsdpMessageTest {
    @Test
    public void isPinned() {
        final SsdpMessage message = new EmptySsdpMessage();
        assertThat(message.isPinned(), is(false));
    }

    @Test
    public void getScopeId() {
        final SsdpMessage message = new EmptySsdpMessage();
        assertThat(message.getScopeId(), is(0));
    }

    @Test
    public void getLocalAddress() {
        final SsdpMessage message = new EmptySsdpMessage();
        assertThat(message.getLocalAddress(), is(nullValue()));
    }

    @Test
    public void getHeader() {
        final SsdpMessage message = new EmptySsdpMessage();
        assertThat(message.getHeader(""), is(nullValue()));
    }

    @Test
    public void setHeader() {
        final SsdpMessage message = new EmptySsdpMessage();
        message.setHeader("", "");
    }

    @Test
    public void getUuid() {
        final SsdpMessage message = new EmptySsdpMessage();
        assertThat(message.getUuid(), is(not(nullValue())));
    }

    @Test
    public void getType() {
        final SsdpMessage message = new EmptySsdpMessage();
        assertThat(message.getType(), is(not(nullValue())));
    }

    @Test
    public void getNts() {
        final SsdpMessage message = new EmptySsdpMessage();
        assertThat(message.getNts(), is(nullValue()));
    }

    @Test
    public void getMaxAge() {
        final SsdpMessage message = new EmptySsdpMessage();
        assertThat(message.getMaxAge() >= 0, is(true));
    }

    @Test
    public void getExpireTime() {
        final SsdpMessage message = new EmptySsdpMessage();
        assertThat(message.getExpireTime() >= 0, is(true));
    }

    @Test
    public void getLocation() {
        final SsdpMessage message = new EmptySsdpMessage();
        assertThat(message.getLocation(), is(nullValue()));
    }

    @Test(expected = IOException.class)
    public void writeData() throws Exception {
        final SsdpMessage message = new EmptySsdpMessage();
        message.writeData(mock(OutputStream.class));
    }
}
