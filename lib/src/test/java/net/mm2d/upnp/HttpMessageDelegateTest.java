/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.HttpMessageDelegate.StartLineProcessor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatchers;

import java.io.UnsupportedEncodingException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class HttpMessageDelegateTest {
    @Test
    public void setBody_エンコード不可でもExceptionは発生しない() throws Exception {
        final String body = "body";
        final HttpMessageDelegate message = spy(new HttpMessageDelegate(mock(StartLineProcessor.class)));
        doThrow(new UnsupportedEncodingException()).when(message).getBytes(anyString());
        message.setBody(body, true);

        assertThat(message.getBody(), is(body));
    }

    @Test
    public void getBody_デコード不可ならnullが返る() throws Exception {
        final HttpMessageDelegate message = spy(new HttpMessageDelegate(mock(StartLineProcessor.class)));
        message.setBodyBinary("body".getBytes("utf-8"));
        doThrow(new UnsupportedEncodingException()).when(message).newString(ArgumentMatchers.any(byte[].class));

        assertThat(message.getBody(), is(nullValue()));
    }


    @Test
    public void getHeaderBytes_エンコード不可でもnullが返らない() throws Exception {
        final HttpMessageDelegate message = spy(new HttpMessageDelegate(mock(StartLineProcessor.class)));
        doThrow(new UnsupportedEncodingException()).when(message).getBytes(anyString());

        assertThat(message.getHeaderBytes().length, is(0));
    }
}