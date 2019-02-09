/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message;


import net.mm2d.upnp.internal.message.HttpMessageDelegate.StartLineDelegate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.UnsupportedEncodingException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class HttpMessageDelegateTest {
    @Test
    public void setBody_エンコード不可でもExceptionは発生しない() throws Exception {
        final String body = "body";
        final HttpMessageDelegate message = Mockito.spy(new HttpMessageDelegate(mock(StartLineDelegate.class)));
        doThrow(new UnsupportedEncodingException()).when(message).getBytes(anyString());
        message.setBody(body, true);

        assertThat(message.getBody(), is(body));
    }

    @Test
    public void getBody_デコード不可ならnullが返る() throws Exception {
        final HttpMessageDelegate message = Mockito.spy(new HttpMessageDelegate(mock(StartLineDelegate.class)));
        message.setBodyBinary("body".getBytes("utf-8"));
        doThrow(new UnsupportedEncodingException()).when(message).newString(ArgumentMatchers.any(byte[].class));

        assertThat(message.getBody(), is(nullValue()));
    }


    @Test
    public void getHeaderBytes_エンコード不可でもnullが返らない() throws Exception {
        final HttpMessageDelegate message = Mockito.spy(new HttpMessageDelegate(mock(StartLineDelegate.class)));
        doThrow(new UnsupportedEncodingException()).when(message).getBytes(anyString());

        assertThat(message.getHeaderBytes().length, is(0));
    }
}