/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.StringPair;
import net.mm2d.util.TestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import javax.annotation.Nonnull;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class EventReceiverTest {
    private byte[] mBadRequest;
    private byte[] mFailRequest;
    private byte[] mNotifyRequest;
    private static final String SID = "uuid:s1234567-89ab-cdef-0123-456789abcdef";

    @Before
    public void setUp() throws Exception {
        final HttpRequest notify = new HttpRequest();
        notify.setMethod(Http.NOTIFY);
        notify.setUri("/");
        notify.setHeader(Http.CONNECTION, Http.CLOSE);
        notify.setHeader(Http.SEQ, "0");
        notify.setBody(TestUtils.getResourceAsString("propchange.xml"), true);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        notify.writeData(baos);
        mBadRequest = baos.toByteArray();
        baos.reset();

        notify.setHeader(Http.NT, Http.UPNP_EVENT);
        notify.setHeader(Http.NTS, Http.UPNP_PROPCHANGE);
        notify.writeData(baos);
        mFailRequest = baos.toByteArray();
        baos.reset();

        notify.setHeader(Http.SID, SID);
        notify.writeData(baos);
        mNotifyRequest = baos.toByteArray();
    }

    @Test(timeout = 10000L)
    public void open_close_デッドロックしない() throws Exception {
        final EventReceiver receiver = new EventReceiver(null);
        receiver.open();
        receiver.close();
    }

    @Test
    public void getLocalPort() throws Exception {
        final int port = 12345;
        final EventReceiver receiver = new EventReceiver(null) {
            @Override
            ServerSocket createServerSocket() throws IOException {
                final ServerSocket serverSocket = mock(ServerSocket.class);
                doReturn(port).when(serverSocket).getLocalPort();
                doThrow(new IOException()).when(serverSocket).accept();
                return serverSocket;
            }
        };

        receiver.open();
        assertThat(receiver.getLocalPort(), is(port));
        receiver.close();
    }

    private class Result {
        String sid;
        long seq;
        List<StringPair> properties;
    }

    @Test(timeout = 10000L)
    public void onEventReceived_イベントの値が取得できること() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ByteArrayInputStream bais = new ByteArrayInputStream(mNotifyRequest);
        final Result result = new Result();
        final EventReceiver receiver = new EventReceiver(new EventReceiver.EventMessageListener() {
            @Override
            public boolean onEventReceived(@Nonnull final String sid, final long seq, @Nonnull final List<StringPair> properties) {
                result.sid = sid;
                result.seq = seq;
                result.properties = properties;
                return true;
            }
        }) {
            @Override
            ServerSocket createServerSocket() throws IOException {
                final Socket socket = mock(Socket.class);
                doReturn(bais).when(socket).getInputStream();
                doReturn(baos).when(socket).getOutputStream();
                final ServerSocket serverSocket = mock(ServerSocket.class);
                doAnswer(new Answer<Socket>() {
                    private int count;

                    @Override
                    public Socket answer(final InvocationOnMock invocation) throws Throwable {
                        if (count++ == 0) {
                            return socket;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (final InterruptedException e) {
                        }
                        throw new IOException();
                    }
                }).when(serverSocket).accept();
                return serverSocket;
            }
        };
        receiver.open();
        Thread.sleep(100);
        receiver.close();
        Thread.sleep(10);

        assertThat(result.sid, is(SID));
        assertThat(result.seq, is(0L));
        assertThat(result.properties, hasItem(new StringPair("SystemUpdateID", "0")));
        assertThat(result.properties, hasItem(new StringPair("ContainerUpdateIDs", "")));

        final HttpResponse response = new HttpResponse();
        response.readData(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(response.getStatus(), is(Http.Status.HTTP_OK));
    }

    @Test(timeout = 10000L)
    public void onEventReceived_Failedが返る1() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ByteArrayInputStream bais = new ByteArrayInputStream(mNotifyRequest);
        final EventReceiver receiver = new EventReceiver(new EventReceiver.EventMessageListener() {
            @Override
            public boolean onEventReceived(@Nonnull final String sid, final long seq, @Nonnull final List<StringPair> properties) {
                return false;
            }
        }) {
            @Override
            ServerSocket createServerSocket() throws IOException {
                final Socket socket = mock(Socket.class);
                doReturn(bais).when(socket).getInputStream();
                doReturn(baos).when(socket).getOutputStream();
                final ServerSocket serverSocket = mock(ServerSocket.class);
                doAnswer(new Answer<Socket>() {
                    private int count;

                    @Override
                    public Socket answer(final InvocationOnMock invocation) throws Throwable {
                        if (count++ == 0) {
                            return socket;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (final InterruptedException e) {
                        }
                        throw new IOException();
                    }
                }).when(serverSocket).accept();
                return serverSocket;
            }
        };
        receiver.open();
        Thread.sleep(100);
        receiver.close();
        Thread.sleep(10);

        final HttpResponse response = new HttpResponse();
        response.readData(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(response.getStatus(), is(Http.Status.HTTP_PRECON_FAILED));
    }

    @Test(timeout = 10000L)
    public void onEventReceived_Failedが返る2() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ByteArrayInputStream bais = new ByteArrayInputStream(mFailRequest);
        final EventReceiver receiver = new EventReceiver(null) {
            @Override
            ServerSocket createServerSocket() throws IOException {
                final Socket socket = mock(Socket.class);
                doReturn(bais).when(socket).getInputStream();
                doReturn(baos).when(socket).getOutputStream();
                final ServerSocket serverSocket = mock(ServerSocket.class);
                doAnswer(new Answer<Socket>() {
                    private int count;

                    @Override
                    public Socket answer(final InvocationOnMock invocation) throws Throwable {
                        if (count++ == 0) {
                            return socket;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (final InterruptedException e) {
                        }
                        throw new IOException();
                    }
                }).when(serverSocket).accept();
                return serverSocket;
            }
        };
        receiver.open();
        Thread.sleep(100);
        receiver.close();
        Thread.sleep(10);

        final HttpResponse response = new HttpResponse();
        response.readData(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(response.getStatus(), is(Http.Status.HTTP_PRECON_FAILED));
    }

    @Test(timeout = 10000L)
    public void onEventReceived_BadRequestが返る() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ByteArrayInputStream bais = new ByteArrayInputStream(mBadRequest);
        final EventReceiver receiver = new EventReceiver(null) {
            @Override
            ServerSocket createServerSocket() throws IOException {
                final Socket socket = mock(Socket.class);
                doReturn(bais).when(socket).getInputStream();
                doReturn(baos).when(socket).getOutputStream();
                final ServerSocket serverSocket = mock(ServerSocket.class);
                doAnswer(new Answer<Socket>() {
                    private int count;

                    @Override
                    public Socket answer(final InvocationOnMock invocation) throws Throwable {
                        if (count++ == 0) {
                            return socket;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (final InterruptedException e) {
                        }
                        throw new IOException();
                    }
                }).when(serverSocket).accept();
                return serverSocket;
            }
        };
        receiver.open();
        Thread.sleep(100);
        receiver.close();
        Thread.sleep(10);

        final HttpResponse response = new HttpResponse();
        response.readData(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(response.getStatus(), is(Http.Status.HTTP_BAD_REQUEST));
    }

    @Test(timeout = 10000L)
    public void close_shutdownRequest() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ByteArrayInputStream bais = new ByteArrayInputStream(mNotifyRequest);
        final EventReceiver receiver = new EventReceiver(new EventReceiver.EventMessageListener() {
            @Override
            public boolean onEventReceived(@Nonnull final String sid, final long seq, @Nonnull final List<StringPair> properties) {
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                }
                return false;
            }
        }) {
            @Override
            ServerSocket createServerSocket() throws IOException {
                final Socket socket = mock(Socket.class);
                doReturn(bais).when(socket).getInputStream();
                doReturn(baos).when(socket).getOutputStream();
                final ServerSocket serverSocket = mock(ServerSocket.class);
                doAnswer(new Answer<Socket>() {
                    private int count;

                    @Override
                    public Socket answer(final InvocationOnMock invocation) throws Throwable {
                        if (count++ == 0) {
                            return socket;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (final InterruptedException e) {
                        }
                        throw new IOException();
                    }
                }).when(serverSocket).accept();
                return serverSocket;
            }
        };
        receiver.open();
        Thread.sleep(100);
        receiver.close();
        Thread.sleep(10);

    }
}