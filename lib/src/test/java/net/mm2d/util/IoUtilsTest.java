/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import net.mm2d.upnp.Http;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Selector;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class IoUtilsTest {
    @Test(expected = InvocationTargetException.class)
    public void constructor() throws Exception {
        final Constructor<IoUtils> constructor = IoUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void closeQuietly_Closeable_closeがコールされる() throws IOException {
        final Closeable closeable = mock(Closeable.class);
        IoUtils.closeQuietly(closeable);
        verify(closeable).close();
    }

    @Test
    public void closeQuietly_Closeable_nullを渡してもExceptionが発生しない() {
        final Closeable closeable = null;
        IoUtils.closeQuietly(closeable);
    }

    @Test
    public void closeQuietly_Closeable_closeでIOExceptionが発生しても外に伝搬しない() {
        try {
            final Closeable closeable = mock(Closeable.class);
            doThrow(new IOException()).when(closeable).close();
            IoUtils.closeQuietly(closeable);
        } catch (final IOException e) {
            fail();
        }
    }

    @Test
    public void closeQuietly_Socket_closeがコールされる() throws IOException {
        final Socket socket = mock(Socket.class);
        IoUtils.closeQuietly(socket);
        verify(socket).close();
    }

    @Test
    public void closeQuietly_Socket_nullを渡してもExceptionが発生しない() {
        final Socket socket = null;
        IoUtils.closeQuietly(socket);
    }

    @Test
    public void closeQuietly_Socket_closeでIOExceptionが発生しても外に伝搬しない() {
        try {
            final Socket socket = mock(Socket.class);
            doThrow(new IOException()).when(socket).close();
            IoUtils.closeQuietly(socket);
        } catch (final IOException e) {
            fail();
        }
    }

    @Test
    public void closeQuietly_DatagramSocket_closeがコールされる() {
        final DatagramSocket datagramSocket = mock(DatagramSocket.class);
        IoUtils.closeQuietly(datagramSocket);
        verify(datagramSocket).close();
    }

    @Test
    public void closeQuietly_DatagramSocket_nullを渡してもExceptionが発生しない() {
        final DatagramSocket datagramSocket = null;
        IoUtils.closeQuietly(datagramSocket);
    }

    @Test
    public void closeQuietly_ServerSocket_closeがコールされる() throws IOException {
        final ServerSocket serverSocket = mock(ServerSocket.class);
        IoUtils.closeQuietly(serverSocket);
        verify(serverSocket).close();
    }

    @Test
    public void closeQuietly_ServerSocket_nullを渡してもExceptionが発生しない() {
        final ServerSocket serverSocket = null;
        IoUtils.closeQuietly(serverSocket);
    }

    @Test
    public void closeQuietly_ServerSocket_closeでIOExceptionが発生しても外に伝搬しない() {
        try {
            final ServerSocket serverSocket = mock(ServerSocket.class);
            doThrow(new IOException()).when(serverSocket).close();
            IoUtils.closeQuietly(serverSocket);
        } catch (final IOException e) {
            fail();
        }
    }

    @Test
    public void closeQuietly_Selector_closeがコールされる() throws IOException {
        final Selector selector = mock(Selector.class);
        IoUtils.closeQuietly(selector);
        verify(selector).close();
    }

    @Test
    public void closeQuietly_Selector_nullを渡してもExceptionが発生しない() {
        final Selector selector = null;
        IoUtils.closeQuietly(selector);
    }

    @Test
    public void closeQuietly_Selector_closeでIOExceptionが発生しても外に伝搬しない() {
        try {
            final Selector selector = mock(Selector.class);
            doThrow(new IOException()).when(selector).close();
            IoUtils.closeQuietly(selector);
        } catch (final IOException e) {
            fail();
        }
    }
}
