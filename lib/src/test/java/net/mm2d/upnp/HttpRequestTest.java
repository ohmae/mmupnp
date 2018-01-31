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
import org.mockito.ArgumentMatchers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class HttpRequestTest {
    private static final String ACTION = "\"urn:schemas-upnp-org:service:ContentDirectory:1#Browse\"";

    @Test
    public void setMethod_getMethodに反映される() {
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.GET);

        assertThat(request.getMethod(), is(Http.GET));
    }

    @Test
    public void setUrl_getAddress_getPort_getUriに反映される() throws IOException {
        final HttpRequest request = new HttpRequest()
                .setUrl(new URL("http://192.0.2.2:12345/cds/control"), true);
        final int port = 12345;
        final InetAddress address = InetAddress.getByName("192.0.2.2");
        final SocketAddress socketAddress = new InetSocketAddress(address, port);

        assertThat(request.getAddressString(), is("192.0.2.2:12345"));
        assertThat(request.getHeader(Http.HOST), is("192.0.2.2:12345"));
        assertThat(request.getAddress(), is(address));
        assertThat(request.getPort(), is(port));
        assertThat(request.getSocketAddress(), is(socketAddress));
        assertThat(request.getUri(), is("/cds/control"));
    }

    @Test(expected = IOException.class)
    public void setUrl_http以外はException() throws IOException {
        final HttpRequest request = new HttpRequest()
                .setUrl(new URL("https://192.0.2.2:12345/cds/control"), true);
    }

    @Test(expected = IOException.class)
    public void setUrl_portがUnsignedShortMax以上ならException() throws IOException {
        final HttpRequest request = new HttpRequest()
                .setUrl(new URL("http://192.0.2.2:65536/cds/control"), true);
    }

    @Test
    public void setUrl_falseではHOSTに反映されない() throws IOException {
        final HttpRequest request = new HttpRequest();
        request.setUrl(new URL("http://192.0.2.2:12345/cds/control"));

        assertThat(request.getHeader(Http.HOST), is(nullValue()));
    }

    @Test
    public void readData_読み込めること() throws IOException {
        final InputStream is = TestUtils.getResourceAsStream("browse-request-length.bin");
        final HttpRequest request = new HttpRequest();
        request.readData(is);

        assertThat(request.getMethod(), is(Http.POST));
        assertThat(request.getUri(), is("/cds/control"));
        assertThat(request.getHeader(Http.SOAPACTION), is(ACTION));
        assertThat(request.getHeader(Http.CONNECTION), is(Http.CLOSE));
        assertThat(request.getHeader(Http.CONTENT_TYPE), is(Http.CONTENT_TYPE_DEFAULT));

        assertThat(request.getBody(), is(TestUtils.getResourceAsString("browse-request.xml")));
    }

    @Test
    public void readData_Chunk読み込めること() throws IOException {
        final InputStream is = TestUtils.getResourceAsStream("browse-request-chunked.bin");
        final HttpRequest request = new HttpRequest();
        request.readData(is);

        assertThat(request.getMethod(), is(Http.POST));
        assertThat(request.getUri(), is("/cds/control"));
        assertThat(request.getHeader(Http.SOAPACTION), is(ACTION));
        assertThat(request.getHeader(Http.CONNECTION), is(Http.CLOSE));
        assertThat(request.getHeader(Http.CONTENT_TYPE), is(Http.CONTENT_TYPE_DEFAULT));
        assertThat(request.getHeader(Http.TRANSFER_ENCODING), is(Http.CHUNKED));

        assertThat(request.getBody(), is(TestUtils.getResourceAsString("browse-request.xml")));
    }

    @Test
    public void writeData_書き出しができること() throws IOException {
        final String soap = TestUtils.getResourceAsString("browse-request.xml");
        final HttpRequest request = new HttpRequest()
                .setMethod(Http.POST)
                .setUrl(new URL("http://192.0.2.2:12345/cds/control"), true)
                .setHeader(Http.SOAPACTION, ACTION)
                .setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
                .setHeader(Http.CONNECTION, Http.CLOSE)
                .setHeader(Http.CONTENT_TYPE, Http.CONTENT_TYPE_DEFAULT)
                .setBody(soap, true);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        request.writeData(baos);

        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final HttpRequest readRequest = new HttpRequest();
        readRequest.readData(bais);

        assertThat(readRequest.getStartLine(), is(request.getStartLine()));
        assertThat(readRequest.getBody(), is(request.getBody()));
    }

    @Test
    public void writeData_Chunk書き出しができること() throws IOException {
        final String soap = TestUtils.getResourceAsString("browse-request.xml");
        final HttpRequest request = new HttpRequest()
                .setMethod(Http.POST)
                .setUrl(new URL("http://192.0.2.2:12345/cds/control"), true)
                .setHeader(Http.SOAPACTION, ACTION)
                .setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
                .setHeader(Http.CONNECTION, Http.CLOSE)
                .setHeader(Http.CONTENT_TYPE, Http.CONTENT_TYPE_DEFAULT)
                .setHeader(Http.TRANSFER_ENCODING, Http.CHUNKED)
                .setBody(soap, false);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        request.writeData(baos);

        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final HttpRequest readRequest = new HttpRequest();
        readRequest.readData(bais);

        assertThat(readRequest.getStartLine(), is(request.getStartLine()));
        assertThat(readRequest.getBody(), is(request.getBody()));
    }

    @Test
    public void setVersion_getVersionで取得できる() {
        final HttpRequest request = new HttpRequest();
        request.setVersion(Http.HTTP_1_0);

        assertThat(request.getVersion(), is(Http.HTTP_1_0));
    }

    @Test
    public void setRequestLine_method_uri_versionに反映される() {
        final HttpRequest request = new HttpRequest();
        request.setRequestLine("GET /cds/control HTTP/1.1");

        assertThat(request.getMethod(), is(Http.GET));
        assertThat(request.getUri(), is("/cds/control"));
        assertThat(request.getVersion(), is(Http.HTTP_1_1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setRequestLine_不足があればException() {
        final HttpRequest request = new HttpRequest();
        request.setRequestLine("GET /cds/control");
    }

    @Test
    public void HttpRequest_ディープコピーができる() throws IOException {
        final InputStream is = TestUtils.getResourceAsStream("browse-request-length.bin");
        final HttpRequest request = new HttpRequest();
        request.readData(is);

        final HttpRequest readRequest = new HttpRequest(request);
        assertThat(readRequest.getMethod(), is(request.getMethod()));
        assertThat(readRequest.getUri(), is(request.getUri()));
        assertThat(readRequest.getHeader(Http.SOAPACTION), is(request.getHeader(Http.SOAPACTION)));
        assertThat(readRequest.getHeader(Http.CONNECTION), is(request.getHeader(Http.CONNECTION)));
        assertThat(readRequest.getHeader(Http.CONTENT_TYPE), is(request.getHeader(Http.CONTENT_TYPE)));
        assertThat(readRequest.getBody(), is(request.getBody()));

        readRequest.setMethod(Http.GET);
        assertThat(request.getMethod(), is(Http.POST));
        assertThat(readRequest.getMethod(), is(Http.GET));
    }

    @Test
    public void HttpRequest_Socketの情報が反映される() throws IOException {
        final InetAddress address = InetAddress.getByName("192.0.2.2");
        final int port = 12345;
        final Socket socket = mock(Socket.class);
        doReturn(address).when(socket).getInetAddress();
        doReturn(port).when(socket).getPort();
        final HttpRequest request = new HttpRequest(socket);

        assertThat(request.getAddress(), is(address));
        assertThat(request.getPort(), is(port));
    }

    @Test(expected = IllegalStateException.class)
    public void getAddressString_未設定ならException() throws Exception {
        final HttpRequest request = new HttpRequest();
        request.getAddressString();
    }

    @Test
    public void getAddressString_デフォルトポート() throws Exception {
        final String address = "192.168.0.1";
        final int port = 8080;
        final HttpRequest request = new HttpRequest();
        request.setAddress(InetAddress.getByName(address));
        assertThat(request.getAddressString(), is(address));
        request.setPort(Http.DEFAULT_PORT);
        assertThat(request.getAddressString(), is(address));
        request.setPort(port);
        assertThat(request.getAddressString(), is(address + ":" + port));
    }

    @Test(expected = IllegalStateException.class)
    public void getSocketAddress_未設定ならException() throws Exception {
        final HttpRequest request = new HttpRequest();
        request.getSocketAddress();
    }

    @Test
    public void setHeaderLine_設定できる() throws Exception {
        final HttpRequest request = new HttpRequest();
        request.setHeaderLine("SOAPACTION: " + ACTION);
        assertThat(request.getHeader(Http.SOAPACTION), is(ACTION));
    }

    @Test
    public void setHeaderLine_フォーマットエラーでもExceptionは発生しない() throws Exception {
        final HttpRequest request = new HttpRequest();
        request.setHeaderLine("SOAPACTION");
        assertThat(request.getHeader(Http.SOAPACTION), is(nullValue()));
    }

    @Test
    public void isKeepAlive() throws Exception {
        final HttpRequest request = new HttpRequest();
        request.setVersion(Http.HTTP_1_0);
        assertThat(request.isKeepAlive(), is(false));
        request.setVersion(Http.HTTP_1_1);
        assertThat(request.isKeepAlive(), is(true));

        request.setHeader(Http.CONNECTION, Http.KEEP_ALIVE);
        request.setVersion(Http.HTTP_1_0);
        assertThat(request.isKeepAlive(), is(true));
        request.setVersion(Http.HTTP_1_1);
        assertThat(request.isKeepAlive(), is(true));

        request.setHeader(Http.CONNECTION, Http.CLOSE);
        request.setVersion(Http.HTTP_1_0);
        assertThat(request.isKeepAlive(), is(false));
        request.setVersion(Http.HTTP_1_1);
        assertThat(request.isKeepAlive(), is(false));
    }

    @Test
    public void getContentLength_正常系() throws Exception {
        final HttpRequest request = new HttpRequest();
        request.setHeader(Http.CONTENT_LENGTH, "10");
        assertThat(request.getContentLength(), is(10));
    }

    @Test
    public void getContentLength_異常系() throws Exception {
        final HttpRequest request = new HttpRequest();
        request.setHeader(Http.CONTENT_LENGTH, "length");
        assertThat(request.getContentLength(), is(0));
    }

    @Test
    public void setBody_getBodyで取得できる() {
        final String body = "body";
        final HttpRequest request = new HttpRequest();
        request.setBody(body);

        assertThat(request.getBody(), is(body));
    }

    @Test
    public void setBody_長さ0() {
        final String body = "";
        final HttpRequest request = new HttpRequest();
        request.setBody(body, true);

        assertThat(request.getBody(), is(body));
        assertThat(request.getBodyBinary().length, is(0));
        assertThat(request.getHeader(Http.CONTENT_LENGTH), is("0"));
    }

    @Test
    public void setBody_エンコード不可でもExceptionは発生しない() throws Exception {
        final String body = "body";
        final HttpRequest request = spy(new HttpRequest());
        doThrow(new UnsupportedEncodingException()).when(request).getBytes(anyString());
        request.setBody(body, true);

        assertThat(request.getBody(), is(body));
    }

    @Test
    public void setBodyBinary_長さ0() {
        final HttpRequest request = new HttpRequest();
        request.setBodyBinary(new byte[0]);

        assertThat(request.getBody().length(), is(0));
        assertThat(request.getBodyBinary().length, is(0));
    }

    @Test
    public void getBody_デコード不可ならnullが返る() throws Exception {
        final HttpRequest request = spy(new HttpRequest());
        request.setBodyBinary("body".getBytes("utf-8"));
        doThrow(new UnsupportedEncodingException()).when(request).newString(ArgumentMatchers.any(byte[].class));

        assertThat(request.getBody(), is(nullValue()));
    }

    @Test
    public void getHeaderBytes_エンコード不可でもnullが返らない() throws Exception {
        final HttpRequest request = spy(new HttpRequest());
        doThrow(new UnsupportedEncodingException()).when(request).getBytes(anyString());

        assertThat(request.getHeaderBytes().length, is(0));
    }
}
