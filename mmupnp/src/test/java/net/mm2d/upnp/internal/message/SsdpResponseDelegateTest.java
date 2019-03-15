/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message;

import net.mm2d.upnp.HttpResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class SsdpResponseDelegateTest {
    private SsdpMessageDelegate mDelegate;
    private SsdpResponse mMessage;

    @Before
    public void setUp() throws Exception {
        mDelegate = mock(SsdpMessageDelegate.class);
        doReturn("").when(mDelegate).getType();
        doReturn("").when(mDelegate).getUuid();
        mMessage = new SsdpResponse(mock(HttpResponse.class), mDelegate);
    }

    @Test
    public void getLocalAddress() {
        mMessage.getLocalAddress();

        verify(mDelegate, times(1)).getLocalAddress();
    }

    @Test
    public void getHeader() {
        final String name = "name";
        mMessage.getHeader(name);

        verify(mDelegate, times(1)).getHeader(name);
    }

    @Test
    public void setHeader() {
        final String name = "name";
        final String value = "value";
        mMessage.setHeader(name, value);

        verify(mDelegate, times(1)).setHeader(name, value);
    }

    @Test
    public void getUuid() {
        mMessage.getUuid();

        verify(mDelegate, times(1)).getUuid();
    }

    @Test
    public void getType() {
        mMessage.getType();

        verify(mDelegate, times(1)).getType();
    }

    @Test
    public void getNts() {
        mMessage.getNts();

        verify(mDelegate, times(1)).getNts();
    }

    @Test
    public void getMaxAge() {
        mMessage.getMaxAge();

        verify(mDelegate, times(1)).getMaxAge();
    }

    @Test
    public void getExpireTime() {
        mMessage.getExpireTime();

        verify(mDelegate, times(1)).getExpireTime();
    }

    @Test
    public void getLocation() {
        mMessage.getLocation();

        verify(mDelegate, times(1)).getLocation();
    }

    @Test
    public void writeData() throws Exception {
        final OutputStream os = new ByteArrayOutputStream();
        mMessage.writeData(os);

        verify(mDelegate, times(1)).writeData(os);
    }

    @Test
    public void toString_() {
        mMessage.toString();
    }
}