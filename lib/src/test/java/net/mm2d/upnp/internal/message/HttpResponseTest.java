/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message;

import net.mm2d.upnp.Http;
import net.mm2d.upnp.Property;
import net.mm2d.util.TestUtils;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class HttpResponseTest {
    private static final Date DATE;

    static {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(2017, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        DATE = cal.getTime();
    }

    @Test
    public void readData_読み出しができること() throws IOException {
        final HttpResponse response = HttpResponse.create()
                .readData(TestUtils.getResourceAsStream("cds-length.bin"));

        assertThat(response.getStartLine(), is("HTTP/1.1 200 OK"));
        assertThat(response.getStatus(), Matchers.is(Http.Status.HTTP_OK));
        assertThat(Http.parseDate(response.getHeader(Http.DATE)), is(DATE));
        assertThat(response.getBody(), is(TestUtils.getResourceAsString("cds.xml")));
    }

    @Test
    public void HttpRequest_ディープコピーができる() throws IOException {
        final HttpResponse response1 = HttpResponse.create()
                .readData(TestUtils.getResourceAsStream("cds-length.bin"));

        final HttpResponse response2 = HttpResponse.copy(response1);

        assertThat(response1.getStartLine(), is(response2.getStartLine()));
        assertThat(response1.getStatus(), is(response2.getStatus()));
        assertThat(response1.getHeader(Http.DATE), is(response2.getHeader(Http.DATE)));
        assertThat(response1.getBody(), is(response2.getBody()));
        assertThat(response1.getBodyBinary(), is(response2.getBodyBinary()));

        response1.getBodyBinary()[0] = 0;

        assertThat(response1.getBodyBinary(), is(not(response2.getBodyBinary())));
    }

    @Test
    public void readData_Chunk読み出しができること() throws IOException {
        final HttpResponse response = HttpResponse.create()
                .readData(TestUtils.getResourceAsStream("cds-chunked.bin"));

        assertThat(response.getStartLine(), is("HTTP/1.1 200 OK"));
        assertThat(response.getStatus(), is(Http.Status.HTTP_OK));
        assertThat(Http.parseDate(response.getHeader(Http.DATE)), is(DATE));
        assertThat(response.getBody(), is(TestUtils.getResourceAsString("cds.xml")));
    }

    @Test
    public void readData_Chunk読み出しができること2() throws IOException {
        final HttpResponse response = HttpResponse.create()
                .readData(TestUtils.getResourceAsStream("cds-chunked-large.bin"));

        assertThat(response.getStartLine(), is("HTTP/1.1 200 OK"));
        assertThat(response.getStatus(), is(Http.Status.HTTP_OK));
        assertThat(Http.parseDate(response.getHeader(Http.DATE)), is(DATE));
        assertThat(response.getBody(), is(TestUtils.getResourceAsString("cds.xml")));
    }

    @Test(expected = IOException.class)
    public void readData_読み出せない場合IOException() throws Exception {
        HttpResponse.create().readData(new ByteArrayInputStream("\n".getBytes()));
    }

    @Test(expected = IOException.class)
    public void readData_status_line異常の場合IOException() throws Exception {
        HttpResponse.create().readData(new ByteArrayInputStream("HTTP/1.1 200".getBytes()));
    }

    @Test(expected = IOException.class)
    public void readData_size異常の場合IOException() throws Exception {
        HttpResponse.create().readData(new ByteArrayInputStream("HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\n  ".getBytes()));
    }

    @Test(expected = IOException.class)
    public void readData_chunk_sizeなしの場合IOException() throws Exception {
        HttpResponse.create().readData(new ByteArrayInputStream("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n\r\n".getBytes()));
    }

    @Test(expected = IOException.class)
    public void readData_chunk_sizeが16進数でない場合IOException() throws Exception {
        HttpResponse.create().readData(new ByteArrayInputStream("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\ngg\r\n".getBytes()));
    }

    @Test(expected = IOException.class)
    public void readData_chunk_sizeよりデータが少ない場合IOException() throws Exception {
        HttpResponse.create().readData(new ByteArrayInputStream("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n10\r\n  \r\n".getBytes()));
    }

    @Test(expected = IOException.class)
    public void readData_最後が0で終わっていない場合IOException2() throws Exception {
        HttpResponse.create().readData(new ByteArrayInputStream("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n2\r\n  \r\n".getBytes()));
    }

    @Test
    public void writeData_書き出しができること() throws IOException {
        final String data = TestUtils.getResourceAsString("cds.xml");
        final HttpResponse response = HttpResponse.create()
                .setStatus(Http.Status.HTTP_OK)
                .setHeader(Http.SERVER, Property.SERVER_VALUE)
                .setHeader(Http.DATE, Http.formatDate(System.currentTimeMillis()))
                .setHeader(Http.CONNECTION, Http.CLOSE)
                .setBody(data, true);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.writeData(baos);

        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final HttpResponse readResponse = HttpResponse.create().readData(bais);

        assertThat(readResponse.getStartLine(), is(response.getStartLine()));
        assertThat(readResponse.getBody(), is(response.getBody()));
    }

    @Test
    public void writeData_Chunk書き出しができること() throws IOException {
        final String data = TestUtils.getResourceAsString("cds.xml");
        final HttpResponse response = HttpResponse.create()
                .setStatus(Http.Status.HTTP_OK)
                .setHeader(Http.SERVER, Property.SERVER_VALUE)
                .setHeader(Http.DATE, Http.formatDate(System.currentTimeMillis()))
                .setHeader(Http.CONNECTION, Http.CLOSE)
                .setHeader(Http.TRANSFER_ENCODING, Http.CHUNKED)
                .setBody(data, false);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.writeData(baos);

        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final HttpResponse readResponse = HttpResponse.create().readData(bais);

        assertThat(readResponse.getStartLine(), is(response.getStartLine()));
        assertThat(readResponse.getBody(), is(response.getBody()));
    }

    @Test
    public void setStatusLine_version_status_phraseに反映される() {
        final HttpResponse response = HttpResponse.create();
        response.setStatusLine("HTTP/1.1 200 OK");

        assertThat(response.getVersion(), is(Http.HTTP_1_1));
        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getReasonPhrase(), is("OK"));
        assertThat(response.getStatus(), is(Http.Status.HTTP_OK));
    }

    @Test
    public void setStatusLine_version_status_phraseに反映される2() {
        final HttpResponse response = HttpResponse.create();
        response.setStatusLine("HTTP/1.1 404 Not Found");

        assertThat(response.getVersion(), is(Http.HTTP_1_1));
        assertThat(response.getStatusCode(), is(404));
        assertThat(response.getReasonPhrase(), is("Not Found"));
        assertThat(response.getStatus(), is(Http.Status.HTTP_NOT_FOUND));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setStatusLine_不足がある場合Exception() {
        final HttpResponse response = HttpResponse.create();
        response.setStatusLine("HTTP/1.1 404");
    }

    @Test
    public void setStatusCode_正常系() {
        final HttpResponse response = HttpResponse.create();
        response.setStatusCode(200);

        assertThat(response.getStatus(), is(Http.Status.HTTP_OK));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setStatusCode_不正なステータスコードはException() {
        final HttpResponse response = HttpResponse.create();
        response.setStatusCode(0);
    }
}
