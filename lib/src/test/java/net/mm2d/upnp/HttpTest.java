/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.Http.Status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class HttpTest {
    @Test(expected = InvocationTargetException.class)
    public void constructor() throws Exception {
        final Constructor<Http> constructor = Http.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void status_valueOf_変換できる() {
        assertThat(Status.valueOf(200), is(Status.HTTP_OK));
    }

    @Test
    public void status_valueOf_想定外の値はINVALID() {
        assertThat(Status.valueOf(1), is(Status.HTTP_INVALID));
    }

    @Test
    public void parseDate_RFC_1123_GMT() throws Exception {
        final Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
                .parse("2018-1-28 13:45:55 GMT");
        assertThat(Http.parseDate("Sun, 28 Jan 2018 13:45:55 GMT"), is(date));
    }

    @Test
    public void parseDate_RFC_1123_JST() throws Exception {
        final Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
                .parse("2018-1-28 13:45:55 JST");
        assertThat(Http.parseDate("Sun, 28 Jan 2018 13:45:55 JST"), is(date));
    }

    @Test
    public void parseDate_RFC_1036_GMT() throws Exception {
        final Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
                .parse("2018-1-28 13:45:55 GMT");
        assertThat(Http.parseDate("Sunday, 28-Jan-18 13:45:55 GMT"), is(date));
    }

    @Test
    public void parseDate_RFC_1036_JST() throws Exception {
        final Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
                .parse("2018-1-28 13:45:55 JST");
        assertThat(Http.parseDate("Sunday, 28-Jan-18 13:45:55 JST"), is(date));
    }

    @Test
    public void parseDate_ASC_TIME() throws Exception {
        final Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
                .parse("2018-1-28 13:45:55 GMT");
        assertThat(Http.parseDate("Sun Jan 28 13:45:55 2018"), is(date));
    }

    @Test
    public void parseDate_Error() throws Exception {
        assertThat(Http.parseDate("2018-1-28 13:45:55"), is(nullValue()));
    }

    @Test
    public void parseDate_nullを渡してもException発生しない() throws Exception {
        assertThat(Http.parseDate(null), is(nullValue()));
    }

    @Test
    public void getCurrentDate() {
        assertThat(Http.parseDate(Http.getCurrentDate()), is(not(nullValue())));
    }

    @Test
    public void isHttpUrl() {
        assertThat(Http.isHttpUrl(null), is(false));
        assertThat(Http.isHttpUrl(""), is(false));
        assertThat(Http.isHttpUrl("https://example.com/"), is(false));
        assertThat(Http.isHttpUrl("http://example.com/"), is(true));
    }

    @Test
    public void makeUrlWithScopeId_0指定は何もしない() throws Exception {
        assertThat(Http.makeUrlWithScopeId("http://[fe80::1234]:8888/device.xml", 0).toString(),
                is("http://[fe80::1234]:8888/device.xml"));
        assertThat(Http.makeUrlWithScopeId("http://192.0.2.3:8888/device.xml", 0).toString(),
                is("http://192.0.2.3:8888/device.xml"));
        assertThat(Http.makeUrlWithScopeId("http://[fe80::1234]/device.xml", 0).toString(),
                is("http://[fe80::1234]/device.xml"));
        assertThat(Http.makeUrlWithScopeId("http://192.0.2.3/device.xml", 0).toString(),
                is("http://192.0.2.3/device.xml"));
    }

    @Test
    public void makeUrlWithScopeId_scope_idが追加できる() throws Exception {
        assertThat(Http.makeUrlWithScopeId("http://[fe80::1234]:8888/device.xml", 1).toString(),
                is("http://[fe80::1234%1]:8888/device.xml"));
        assertThat(Http.makeUrlWithScopeId("http://[fe80::1234]/device.xml", 1).toString(),
                is("http://[fe80::1234%1]/device.xml"));
        assertThat(Http.makeUrlWithScopeId("http://[fe80::1234]:80/device.xml", 1).toString(),
                is("http://[fe80::1234%1]/device.xml"));
        assertThat(Http.makeUrlWithScopeId("http://[fe80::1234]:8888/device.xml&bitrate=1200", 1).toString(),
                is("http://[fe80::1234%1]:8888/device.xml&bitrate=1200"));
    }

    @Test
    public void makeUrlWithScopeId_指定済みの場合は置換する() throws Exception {
        assertThat(Http.makeUrlWithScopeId("http://[fe80::1234%22]:8888/device.xml", 1).toString(),
                is("http://[fe80::1234%1]:8888/device.xml"));
    }

    @Test
    public void makeUrlWithScopeId_IPv6リテラル以外は追加しない() throws Exception {
        assertThat(Http.makeUrlWithScopeId("http://192.0.2.1:8888/device.xml", 1).toString(),
                is("http://192.0.2.1:8888/device.xml"));
        assertThat(Http.makeUrlWithScopeId("http://192.0.2.1/device.xml", 1).toString(),
                is("http://192.0.2.1/device.xml"));
        assertThat(Http.makeUrlWithScopeId("http://www.example.com:8888/device.xml", 1).toString(),
                is("http://www.example.com:8888/device.xml"));
        assertThat(Http.makeUrlWithScopeId("http://www.example.com/device.xml", 1).toString(),
                is("http://www.example.com/device.xml"));
    }
}
