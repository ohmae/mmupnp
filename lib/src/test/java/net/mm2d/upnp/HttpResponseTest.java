/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.TestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(JUnit4.class)
public class HttpResponseTest {
    private static final Date DATE;
    static {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(2017, 0, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        DATE = cal.getTime();
    }

    @Test
    public void readData_読み出しができること() throws IOException {
        final HttpResponse response = new HttpResponse();
        response.readData(TestUtils.getResourceAsStream("device-length.bin"));

        assertThat(response.getStartLine(), is("HTTP/1.1 200 OK"));
        assertThat(response.getStatus(), is(Http.Status.HTTP_OK));
        assertThat(Http.parseDate(response.getHeader(Http.DATE)), is(DATE));
        assertThat(response.getBody(), is(TestUtils.getResourceAsString("device.xml")));
    }

    @Test
    public void readData_Chunk読み出しができること() throws IOException {
        final HttpResponse response = new HttpResponse();
        response.readData(TestUtils.getResourceAsStream("device-chunked.bin"));

        assertThat(response.getStartLine(), is("HTTP/1.1 200 OK"));
        assertThat(response.getStatus(), is(Http.Status.HTTP_OK));
        assertThat(Http.parseDate(response.getHeader(Http.DATE)), is(DATE));
        assertThat(response.getBody(), is(TestUtils.getResourceAsString("device.xml")));
    }
    @Test
    public void writeData_書き出しができること() throws IOException {
        final String data = TestUtils.getResourceAsString("device.xml");
        final HttpResponse response = new HttpResponse();
        response.setStatus(Http.Status.HTTP_OK);
        response.setHeader(Http.SERVER, Property.SERVER_VALUE);
        response.setHeader(Http.DATE, Http.formatDate(System.currentTimeMillis()));
        response.setHeader(Http.CONNECTION, Http.CLOSE);
        response.setBody(data, true);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.writeData(baos);

        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final HttpResponse readResponse = new HttpResponse();
        readResponse.readData(bais);

        assertThat(readResponse.getStartLine(), is(response.getStartLine()));
        assertThat(readResponse.getBody(), is(response.getBody()));
    }

    @Test
    public void writeData_Chunk書き出しができること() throws IOException {
        final String data = TestUtils.getResourceAsString("device.xml");
        final HttpResponse response = new HttpResponse();
        response.setStatus(Http.Status.HTTP_OK);
        response.setHeader(Http.SERVER, Property.SERVER_VALUE);
        response.setHeader(Http.DATE, Http.formatDate(System.currentTimeMillis()));
        response.setHeader(Http.CONNECTION, Http.CLOSE);
        response.setHeader(Http.TRANSFER_ENCODING, Http.CHUNKED);
        response.setBody(data, false);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.writeData(baos);

        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final HttpResponse readResponse = new HttpResponse();
        readResponse.readData(bais);

        assertThat(readResponse.getStartLine(), is(response.getStartLine()));
        assertThat(readResponse.getBody(), is(response.getBody()));
    }

    @Test
    public void setStatusLine_version_status_phraseに反映される() {
        final HttpResponse response = new HttpResponse();
        response.setStatusLine("HTTP/1.1 200 OK");

        assertThat(response.getVersion(), is(Http.HTTP_1_1));
        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getReasonPhrase(), is("OK"));
        assertThat(response.getStatus(), is(Http.Status.HTTP_OK));
    }

    @Test
    public void HttpResponse_Socketの情報が反映される() throws IOException {
        final InetAddress address = InetAddress.getByName("192.0.2.2");
        final int port = 12345;
        final Socket socket = mock(Socket.class);
        doReturn(address).when(socket).getInetAddress();
        doReturn(port).when(socket).getPort();
        final HttpResponse response = new HttpResponse(socket);

        assertThat(response.getAddress(), is(address));
        assertThat(response.getPort(), is(port));
    }
}
