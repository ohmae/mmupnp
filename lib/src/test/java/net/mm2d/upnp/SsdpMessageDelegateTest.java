/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class SsdpMessageDelegateTest {
    private static final int DEFAULT_MAX_AGE = 1800;

    @Test
    public void parseCacheControl_正常() {
        final HttpMessage message = new HttpResponse();
        message.setHeader(Http.CACHE_CONTROL, "max-age=100");

        assertThat(SsdpMessageDelegate.parseCacheControl(message), is(100));
    }

    @Test
    public void parseCacheControl_空() {
        final HttpMessage message = new HttpResponse();

        assertThat(SsdpMessageDelegate.parseCacheControl(message), is(DEFAULT_MAX_AGE));
    }

    @Test
    public void parseCacheControl_max_ageから始まらない() {
        final HttpMessage message = new HttpResponse();
        message.setHeader(Http.CACHE_CONTROL, "age=100");

        assertThat(SsdpMessageDelegate.parseCacheControl(message), is(DEFAULT_MAX_AGE));
    }

    @Test
    public void parseCacheControl_デリミタが違う() {
        final HttpMessage message = new HttpResponse();
        message.setHeader(Http.CACHE_CONTROL, "max-age:100");

        assertThat(SsdpMessageDelegate.parseCacheControl(message), is(DEFAULT_MAX_AGE));
    }

    @Test
    public void parseCacheControl_数値がない() {
        final HttpMessage message = new HttpResponse();
        message.setHeader(Http.CACHE_CONTROL, "max-age=");

        assertThat(SsdpMessageDelegate.parseCacheControl(message), is(DEFAULT_MAX_AGE));
    }

    @Test
    public void parseCacheControl_10進数でない() {
        final HttpMessage message = new HttpResponse();
        message.setHeader(Http.CACHE_CONTROL, "max-age=ff");

        assertThat(SsdpMessageDelegate.parseCacheControl(message), is(DEFAULT_MAX_AGE));
    }

    @Test
    public void parseUsn_正常1() {
        final HttpMessage message = new HttpResponse();
        message.setHeader(Http.USN, "uuid:01234567-89ab-cdef-0123-456789abcdef::upnp:rootdevice");

        final String[] result = SsdpMessageDelegate.parseUsn(message);

        assertThat(result[0], is("uuid:01234567-89ab-cdef-0123-456789abcdef"));
        assertThat(result[1], is("upnp:rootdevice"));
    }

    @Test
    public void parseUsn_正常2() {
        final HttpMessage message = new HttpResponse();
        message.setHeader(Http.USN, "uuid:01234567-89ab-cdef-0123-456789abcdef");

        final String[] result = SsdpMessageDelegate.parseUsn(message);

        assertThat(result[0], is("uuid:01234567-89ab-cdef-0123-456789abcdef"));
        assertThat(result[1], is(""));
    }

    @Test
    public void parseUsn_空() {
        final HttpMessage message = new HttpResponse();

        final String[] result = SsdpMessageDelegate.parseUsn(message);

        assertThat(result[0], is(""));
        assertThat(result[1], is(""));
    }

    @Test
    public void parseUsn_uuidでない() {
        final HttpMessage message = new HttpResponse();
        message.setHeader(Http.USN, "01234567-89ab-cdef-0123-456789abcdef");

        final String[] result = SsdpMessageDelegate.parseUsn(message);

        assertThat(result[0], is(""));
        assertThat(result[1], is(""));
    }
}
