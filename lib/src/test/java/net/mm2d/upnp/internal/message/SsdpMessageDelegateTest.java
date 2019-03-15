/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message;

import net.mm2d.upnp.Http;
import net.mm2d.upnp.HttpMessage;
import net.mm2d.upnp.HttpRequest;
import net.mm2d.upnp.HttpResponse;
import net.mm2d.upnp.util.TestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.net.Inet6Address;
import java.net.InetAddress;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class SsdpMessageDelegateTest {
    private static final int DEFAULT_MAX_AGE = 1800;

    @Test
    public void parseCacheControl_正常() {
        final HttpMessage message = HttpResponse.create();
        message.setHeader(Http.CACHE_CONTROL, "max-age=100");

        assertThat(SsdpMessageDelegate.parseCacheControl(message), is(100));
    }

    @Test
    public void parseCacheControl_空() {
        final HttpMessage message = HttpResponse.create();

        assertThat(SsdpMessageDelegate.parseCacheControl(message), is(DEFAULT_MAX_AGE));
    }

    @Test
    public void parseCacheControl_max_ageから始まらない() {
        final HttpMessage message = HttpResponse.create();
        message.setHeader(Http.CACHE_CONTROL, "age=100");

        assertThat(SsdpMessageDelegate.parseCacheControl(message), is(DEFAULT_MAX_AGE));
    }

    @Test
    public void parseCacheControl_デリミタが違う() {
        final HttpMessage message = HttpResponse.create();
        message.setHeader(Http.CACHE_CONTROL, "max-age:100");

        assertThat(SsdpMessageDelegate.parseCacheControl(message), is(DEFAULT_MAX_AGE));
    }

    @Test
    public void parseCacheControl_数値がない() {
        final HttpMessage message = HttpResponse.create();
        message.setHeader(Http.CACHE_CONTROL, "max-age=");

        assertThat(SsdpMessageDelegate.parseCacheControl(message), is(DEFAULT_MAX_AGE));
    }

    @Test
    public void parseCacheControl_10進数でない() {
        final HttpMessage message = HttpResponse.create();
        message.setHeader(Http.CACHE_CONTROL, "max-age=ff");

        assertThat(SsdpMessageDelegate.parseCacheControl(message), is(DEFAULT_MAX_AGE));
    }

    @Test
    public void parseUsn_正常1() {
        final HttpMessage message = HttpResponse.create();
        message.setHeader(Http.USN, "uuid:01234567-89ab-cdef-0123-456789abcdef::upnp:rootdevice");

        final String[] result = SsdpMessageDelegate.parseUsn(message);

        assertThat(result[0], is("uuid:01234567-89ab-cdef-0123-456789abcdef"));
        assertThat(result[1], is("upnp:rootdevice"));
    }

    @Test
    public void parseUsn_正常2() {
        final HttpMessage message = HttpResponse.create();
        message.setHeader(Http.USN, "uuid:01234567-89ab-cdef-0123-456789abcdef");

        final String[] result = SsdpMessageDelegate.parseUsn(message);

        assertThat(result[0], is("uuid:01234567-89ab-cdef-0123-456789abcdef"));
        assertThat(result[1], is(""));
    }

    @Test
    public void parseUsn_空() {
        final HttpMessage message = HttpResponse.create();

        final String[] result = SsdpMessageDelegate.parseUsn(message);

        assertThat(result[0], is(""));
        assertThat(result[1], is(""));
    }

    @Test
    public void parseUsn_uuidでない() {
        final HttpMessage message = HttpResponse.create();
        message.setHeader(Http.USN, "01234567-89ab-cdef-0123-456789abcdef");

        final String[] result = SsdpMessageDelegate.parseUsn(message);

        assertThat(result[0], is(""));
        assertThat(result[1], is(""));
    }

    @Test
    public void getScopeId_インターフェース指定がなければ0() throws Exception {
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
        final HttpRequest request = HttpRequest.create();
        request.readData(new ByteArrayInputStream(data, 0, data.length));
        final SsdpMessageDelegate delegate = new SsdpMessageDelegate(request);

        assertThat(delegate.getScopeId(), is(0));
    }

    @Test
    public void getScopeId_インターフェースIPv4なら0() throws Exception {
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
        final HttpRequest request = HttpRequest.create();
        request.readData(new ByteArrayInputStream(data, 0, data.length));
        final SsdpMessageDelegate delegate = new SsdpMessageDelegate(request, InetAddress.getByName("192.0.2.3"));

        assertThat(delegate.getScopeId(), is(0));
    }

    @Test
    public void getScopeId_インターフェースに紐付かないIPv6なら0() throws Exception {
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
        final HttpRequest request = HttpRequest.create();
        request.readData(new ByteArrayInputStream(data, 0, data.length));
        final SsdpMessageDelegate delegate = new SsdpMessageDelegate(request, InetAddress.getByName("fe80::a831:801b:8dc6:421f"));

        assertThat(delegate.getScopeId(), is(0));
    }

    @Test
    public void getScopeId_インターフェースに紐付くIPv6ならその値() throws Exception {
        final int scopeId = 1;
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
        final HttpRequest request = HttpRequest.create();
        request.readData(new ByteArrayInputStream(data, 0, data.length));
        final Inet6Address address = Inet6Address.getByAddress(null, InetAddress.getByName("fe80::a831:801b:8dc6:421f").getAddress(), scopeId);
        final SsdpMessageDelegate delegate = new SsdpMessageDelegate(request, address);

        assertThat(delegate.getScopeId(), is(scopeId));
    }
}
