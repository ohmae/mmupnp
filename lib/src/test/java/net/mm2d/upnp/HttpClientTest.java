/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.Http.Status;
import net.mm2d.upnp.HttpServerMock.ServerCore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import javax.annotation.Nonnull;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class HttpClientTest {
    @Test
    public void downloadString_KeepAlive有効() throws Exception {
        final String responseBody = "responseBody";

        final HttpServerMock server = new HttpServerMock();
        server.setServerCore(new ServerCore() {
            @Override
            public boolean receiveAndReply(
                    @Nonnull final Socket socket,
                    @Nonnull final InputStream is,
                    @Nonnull final OutputStream os) throws IOException {
                final HttpRequest request = new HttpRequest().readData(is);
                final HttpResponse response = new HttpResponse();
                response.setStatusLine("HTTP/1.1 200 OK");
                response.setBody(responseBody, true);
                if (request.isKeepAlive()) {
                    response.setHeader(Http.CONNECTION, Http.KEEP_ALIVE);
                    response.writeData(os);
                    return true;
                }
                response.setHeader(Http.CONNECTION, Http.CLOSE);
                response.writeData(os);
                return false;
            }
        });
        server.open();
        final int port = server.getLocalPort();

        try {
            final HttpClient client = new HttpClient(true);
            assertThat(client.downloadString(new URL("http://127.0.0.1:" + port + "/")), is(responseBody));
            assertThat(client.isClosed(), is(false));

            client.setKeepAlive(false);
            assertThat(client.downloadString(new URL("http://127.0.0.1:" + port + "/")), is(responseBody));
            client.close();
        } finally {
            server.close();
        }
    }

    @Test
    public void downloadBinary_KeepAlive有効() throws Exception {
        final byte[] responseBody = "responseBody".getBytes("utf-8");

        final HttpServerMock server = new HttpServerMock();
        server.setServerCore(new ServerCore() {
            @Override
            public boolean receiveAndReply(
                    @Nonnull final Socket socket,
                    @Nonnull final InputStream is,
                    @Nonnull final OutputStream os) throws IOException {
                final HttpRequest request = new HttpRequest().readData(is);
                final HttpResponse response = new HttpResponse();
                response.setStatusLine("HTTP/1.1 200 OK");
                response.setBodyBinary(responseBody, true);
                if (request.isKeepAlive()) {
                    response.setHeader(Http.CONNECTION, Http.KEEP_ALIVE);
                    response.writeData(os);
                    return true;
                }
                response.setHeader(Http.CONNECTION, Http.CLOSE);
                response.writeData(os);
                return false;
            }
        });
        server.open();
        final int port = server.getLocalPort();

        try {
            final HttpClient client = new HttpClient(true);
            assertThat(client.downloadBinary(new URL("http://127.0.0.1:" + port + "/")), is(responseBody));
            assertThat(client.isClosed(), is(false));

            client.setKeepAlive(false);
            assertThat(client.downloadBinary(new URL("http://127.0.0.1:" + port + "/")), is(responseBody));
            client.close();
        } finally {
            server.close();
        }
    }

    @Test
    public void download_KeepAliveなのに切断されても取得にいく() throws Exception {
        final String responseBody = "responseBody";

        final HttpServerMock server = new HttpServerMock();
        server.setServerCore(new ServerCore() {
            @Override
            public boolean receiveAndReply(
                    @Nonnull final Socket socket,
                    @Nonnull final InputStream is,
                    @Nonnull final OutputStream os) throws IOException {
                final HttpRequest request = new HttpRequest().readData(is);
                final HttpResponse response = new HttpResponse();
                response.setStatusLine("HTTP/1.1 200 OK");
                response.setBody(responseBody, true);
                if (request.isKeepAlive()) {
                    response.setHeader(Http.CONNECTION, Http.KEEP_ALIVE);
                    response.writeData(os);
                    return false; // コネクション切断
                }
                response.setHeader(Http.CONNECTION, Http.CLOSE);
                response.writeData(os);
                return false;
            }
        });
        server.open();
        final int port = server.getLocalPort();

        try {
            final HttpClient client = new HttpClient(true);
            HttpResponse response = client.download(new URL("http://127.0.0.1:" + port + "/"));
            assertThat(response.getBody(), is(responseBody));
            assertThat(client.isKeepAlive(), is(true));
            response = client.download(new URL("http://127.0.0.1:" + port + "/"));
            assertThat(response.getBody(), is(responseBody));
            assertThat(client.isKeepAlive(), is(false));
            client.close();
        } finally {
            server.close();
        }
    }

    @Test
    public void download_KeepAliveでリクエストしてもcloseが返されたらclose() throws Exception {
        final String responseBody = "responseBody";

        final HttpServerMock server = new HttpServerMock();
        server.setServerCore(new ServerCore() {
            @Override
            public boolean receiveAndReply(
                    @Nonnull final Socket socket,
                    @Nonnull final InputStream is,
                    @Nonnull final OutputStream os) throws IOException {
                final HttpRequest request = new HttpRequest().readData(is);
                final HttpResponse response = new HttpResponse();
                response.setStatusLine("HTTP/1.1 200 OK");
                response.setBody(responseBody, true);
                response.setHeader(Http.CONNECTION, Http.CLOSE);
                response.writeData(os);
                return false;
            }
        });
        server.open();
        final int port = server.getLocalPort();

        try {
            final HttpClient client = new HttpClient(true);
            HttpResponse response = client.download(new URL("http://127.0.0.1:" + port + "/"));
            assertThat(response.getBody(), is(responseBody));
            assertThat(client.isKeepAlive(), is(true));
            assertThat(client.isClosed(), is(true));

            client.close();
        } finally {
            server.close();
        }
    }

    @Test(timeout = 1000)
    public void post_Redirectが無限ループしない() throws Exception {
        final HttpServerMock server = new HttpServerMock();
        server.open();
        final int port = server.getLocalPort();
        server.setServerCore(new ServerCore() {
            @Override
            public boolean receiveAndReply(
                    @Nonnull final Socket socket,
                    @Nonnull final InputStream is,
                    @Nonnull final OutputStream os) throws IOException {
                final HttpRequest request = new HttpRequest().readData(is);
                final HttpResponse response = new HttpResponse();
                response.setHeader(Http.CONNECTION, Http.CLOSE);
                response.setStatusLine("HTTP/1.1 301 Moved Permanently");
                response.setHeader(Http.LOCATION, "http://127.0.0.1:" + port + "/b");
                response.setBody("a", true);
                response.writeData(os);
                return false;
            }
        });

        try {
            final HttpClient client = new HttpClient(false);
            final HttpResponse response = client.post(new HttpRequest()
                    .setMethod(Http.GET)
                    .setUrl(new URL("http://127.0.0.1:" + port + "/a"), true)
                    .setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
                    .setHeader(Http.CONNECTION, Http.CLOSE));
            client.close();
        } finally {
            server.close();
        }
    }

    @Test
    public void post_Redirectが動作する() throws Exception {
        final HttpServerMock server = new HttpServerMock();
        server.open();
        final int port = server.getLocalPort();
        server.setServerCore(new ServerCore() {
            @Override
            public boolean receiveAndReply(
                    @Nonnull final Socket socket,
                    @Nonnull final InputStream is,
                    @Nonnull final OutputStream os) throws IOException {
                final HttpRequest request = new HttpRequest().readData(is);
                final HttpResponse response = new HttpResponse();
                response.setHeader(Http.CONNECTION, Http.CLOSE);
                if (request.getUri().equals("/b")) {
                    response.setStatusLine("HTTP/1.1 200 OK");
                    response.setBody("b", true);
                    response.writeData(os);
                    return false;
                }
                response.setStatusLine("HTTP/1.1 301 Moved Permanently");
                response.setHeader(Http.LOCATION, "http://127.0.0.1:" + port + "/b");
                response.setBody("a", true);
                response.writeData(os);
                return false;
            }
        });

        try {
            final HttpClient client = new HttpClient(false);
            final HttpResponse response = client.post(new HttpRequest()
                    .setMethod(Http.GET)
                    .setUrl(new URL("http://127.0.0.1:" + port + "/a"), true)
                    .setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
                    .setHeader(Http.CONNECTION, Http.CLOSE));
            assertThat(response.getBody(), is("b"));
            client.close();
        } finally {
            server.close();
        }
    }

    @Test
    public void post_Redirectのlocationがなければひとまずそのまま取得する() throws Exception {
        final HttpServerMock server = new HttpServerMock();
        server.setServerCore(new ServerCore() {
            @Override
            public boolean receiveAndReply(
                    @Nonnull final Socket socket,
                    @Nonnull final InputStream is,
                    @Nonnull final OutputStream os) throws IOException {
                final HttpRequest request = new HttpRequest().readData(is);
                final HttpResponse response = new HttpResponse();
                response.setHeader(Http.CONNECTION, Http.CLOSE);
                if (request.getUri().equals("/b")) {
                    response.setStatusLine("HTTP/1.1 200 OK");
                    response.setBody("b", true);
                    response.writeData(os);
                    return false;
                }
                response.setStatusLine("HTTP/1.1 301 Moved Permanently");
                response.setBody("a", true);
                response.writeData(os);
                return false;
            }
        });

        server.open();
        final int port = server.getLocalPort();
        try {
            final HttpClient client = new HttpClient(false);
            final HttpResponse response = client.post(new HttpRequest()
                    .setMethod(Http.GET)
                    .setUrl(new URL("http://127.0.0.1:" + port + "/a"), true)
                    .setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
                    .setHeader(Http.CONNECTION, Http.CLOSE));
            assertThat(response.getStatus(), is(Status.HTTP_MOVED_PERM));
            assertThat(response.getBody(), is("a"));
            client.close();
        } finally {
            server.close();
        }
    }

    @Test(expected = IOException.class)
    public void post_応答がなければException() throws Exception {
        final HttpServerMock server = new HttpServerMock();
        server.setServerCore(new ServerCore() {
            @Override
            public boolean receiveAndReply(
                    @Nonnull final Socket socket,
                    @Nonnull final InputStream is,
                    @Nonnull final OutputStream os) throws IOException {
                new HttpRequest().readData(is);
                return false;
            }
        });
        server.open();
        final int port = server.getLocalPort();

        try {
            final HttpClient client = new HttpClient(false);
            client.post(new HttpRequest()
                    .setMethod(Http.GET)
                    .setUrl(new URL("http://127.0.0.1:" + port + "/a"), true)
                    .setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
                    .setHeader(Http.CONNECTION, Http.CLOSE));
            assertThat(client.isClosed(), is(true));
            client.close();
        } finally {
            server.close();
        }
    }

    @Test
    public void post_応答がなければcloseしてException() throws Exception {
        final HttpServerMock server = new HttpServerMock();
        server.setServerCore(new ServerCore() {
            @Override
            public boolean receiveAndReply(
                    @Nonnull final Socket socket,
                    @Nonnull final InputStream is,
                    @Nonnull final OutputStream os) throws IOException {
                new HttpRequest().readData(is);
                return false;
            }
        });
        server.open();
        final int port = server.getLocalPort();

        try {
            final HttpClient client = new HttpClient(false);
            try {
                client.post(new HttpRequest()
                        .setMethod(Http.GET)
                        .setUrl(new URL("http://127.0.0.1:" + port + "/a"), true)
                        .setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
                        .setHeader(Http.CONNECTION, Http.CLOSE));
            } catch (final IOException ignored) {
            }
            assertThat(client.isClosed(), is(true));
            client.close();
        } finally {
            server.close();
        }
    }

    @Test(expected = IOException.class)
    public void download_HTTP_OKでなければException() throws Exception {
        final HttpClient client = spy(new HttpClient());
        final HttpResponse response = new HttpResponse();
        response.setStatusLine("HTTP/1.1 404 Not Found");
        doReturn(response).when(client).post(ArgumentMatchers.any(HttpRequest.class));
        client.download(new URL("http://www.example.com/index.html"));
    }

    @Test(expected = IOException.class)
    public void download_bodyがnullならException() throws Exception {
        final HttpClient client = spy(new HttpClient());
        final HttpResponse response = new HttpResponse();
        response.setStatusLine("HTTP/1.1 200 OK");
        doReturn(response).when(client).post(ArgumentMatchers.any(HttpRequest.class));
        client.download(new URL("http://www.example.com/index.html"));
    }

    @Test
    public void canReuse_初期状態ではfalse() throws Exception {
        final HttpClient client = new HttpClient();
        final HttpRequest request = new HttpRequest()
                .setUrl(new URL("http://192.168.0.1/index.html"));

        assertThat(client.canReuse(request), is(false));
    }

    @Test
    public void canReuse_接続状態かつアドレスとポートが一致すればtrue() throws Exception {
        final HttpClient client = new HttpClient();
        final Field socket = HttpClient.class.getDeclaredField("mSocket");
        socket.setAccessible(true);
        socket.set(client, new Socket() {
            @Override
            public boolean isConnected() {
                return true;
            }

            @Override
            public InetAddress getInetAddress() {
                try {
                    return InetAddress.getByName("192.168.0.1");
                } catch (final UnknownHostException e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public int getPort() {
                return 80;
            }
        });
        final HttpRequest request = new HttpRequest()
                .setUrl(new URL("http://192.168.0.1/index.html"));
        assertThat(client.canReuse(request), is(true));
    }

    @Test
    public void canReuse_ポートが不一致ならfalse() throws Exception {
        final HttpClient client = new HttpClient();
        final Field socket = HttpClient.class.getDeclaredField("mSocket");
        socket.setAccessible(true);
        socket.set(client, new Socket() {
            @Override
            public boolean isConnected() {
                return true;
            }

            @Override
            public InetAddress getInetAddress() {
                try {
                    return InetAddress.getByName("192.168.0.1");
                } catch (final UnknownHostException e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public int getPort() {
                return 80;
            }
        });
        final HttpRequest request = new HttpRequest()
                .setUrl(new URL("http://192.168.0.1:8080/index.html"));
        assertThat(client.canReuse(request), is(false));
    }

    @Test
    public void canReuse_アドレスが不一致ならfalse() throws Exception {
        final HttpClient client = new HttpClient();
        final Field socket = HttpClient.class.getDeclaredField("mSocket");
        socket.setAccessible(true);
        socket.set(client, new Socket() {
            @Override
            public boolean isConnected() {
                return true;
            }

            @Override
            public InetAddress getInetAddress() {
                try {
                    return InetAddress.getByName("192.168.0.2");
                } catch (final UnknownHostException e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public int getPort() {
                return 80;
            }
        });
        final HttpRequest request = new HttpRequest()
                .setUrl(new URL("http://192.168.0.1/index.html"));
        assertThat(client.canReuse(request), is(false));
    }

    @Test
    public void canReuse_接続状態でなければfalse() throws Exception {
        final HttpClient client = new HttpClient();
        final Field socket = HttpClient.class.getDeclaredField("mSocket");
        socket.setAccessible(true);
        socket.set(client, new Socket() {
            @Override
            public boolean isConnected() {
                return false;
            }

            @Override
            public InetAddress getInetAddress() {
                try {
                    return InetAddress.getByName("192.168.0.1");
                } catch (final UnknownHostException e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public int getPort() {
                return 80;
            }
        });
        final HttpRequest request = new HttpRequest()
                .setUrl(new URL("http://192.168.0.1/index.html"));
        assertThat(client.canReuse(request), is(false));
    }
}
