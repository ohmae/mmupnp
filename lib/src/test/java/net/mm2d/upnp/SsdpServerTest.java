/*
 * Copyright(C)  2018 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.SsdpServer.ReceiveTask;
import net.mm2d.util.NetworkUtils;
import net.mm2d.util.TestUtils;

import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class SsdpServerTest {

    @Test
    public void findInet4Address() throws Exception {
        final InterfaceAddress ipv4 = TestUtils.createInterfaceAddress("192.168.0.1", "255.255.255.0", (short) 24);
        final InterfaceAddress ipv6 = TestUtils.createInterfaceAddress("fe80::a831:801b:8dc6:421f", "255.255.0.0", (short) 16);
        assertThat(SsdpServer.findInet4Address(Arrays.asList(ipv4, ipv6)), is(ipv4));
        assertThat(SsdpServer.findInet4Address(Arrays.asList(ipv6, ipv4)), is(ipv4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void findInet4Address_見つからなければException1() throws Exception {
        final InterfaceAddress ipv6 = TestUtils.createInterfaceAddress("fe80::a831:801b:8dc6:421f", "255.255.0.0", (short) 16);
        SsdpServer.findInet4Address(Arrays.asList(ipv6, ipv6));
    }

    @Test(expected = IllegalArgumentException.class)
    public void findInet4Address_見つからなければException2() throws Exception {
        SsdpServer.findInet4Address(Collections.<InterfaceAddress>emptyList());
    }

    @Test
    public void open_close() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServer server = spy(new SsdpServer(networkInterface) {
            @Override
            protected void onReceive(
                    @Nonnull final InetAddress sourceAddress,
                    @Nonnull final byte[] data,
                    final int length) {
            }
        });
        final MulticastSocket socket = mock(MulticastSocket.class);
        doReturn(socket).when(server).createMulticastSocket(anyInt());
        server.open();
        verify(socket, times(1)).setNetworkInterface(networkInterface);
        verify(socket, times(1)).setTimeToLive(4);
        server.open();
        verify(server, times(1)).close();
        verify(server, times(1)).stop();
        verify(socket, times(1)).close();
        server.close();
    }

    @Test
    public void start_stop() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServer server = spy(new SsdpServer(networkInterface) {
            @Override
            protected void onReceive(
                    @Nonnull final InetAddress sourceAddress,
                    @Nonnull final byte[] data,
                    final int length) {
            }
        });
        final MulticastSocket socket = mock(MulticastSocket.class);
        doReturn(socket).when(server).createMulticastSocket(anyInt());
        server.open();
        server.start();
        server.start();
        verify(server, times(1)).stop();
        server.stop();
        server.close();
    }

    @Test
    public void send_open前は何も起こらない() throws IOException {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServer server = spy(new SsdpServer(networkInterface) {
            @Override
            protected void onReceive(
                    @Nonnull final InetAddress sourceAddress,
                    @Nonnull final byte[] data,
                    final int length) {
            }
        });
        final MulticastSocket socket = mock(MulticastSocket.class);
        doReturn(socket).when(server).createMulticastSocket(anyInt());

        final SsdpRequestMessage message = new SsdpRequestMessage();
        message.setMethod(SsdpMessage.M_SEARCH);
        message.setUri("*");
        message.setHeader(Http.HOST, SsdpServer.SSDP_ADDR + ":" + String.valueOf(SsdpServer.SSDP_PORT));
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER);
        message.setHeader(Http.MX, "1");
        message.setHeader(Http.ST, SsdpSearchServer.ST_ROOTDEVICE);

        server.send(message);

        verify(socket, never()).send(ArgumentMatchers.any(DatagramPacket.class));
    }

    @Test
    public void send_socketから送信される() throws IOException {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServer server = spy(new SsdpServer(networkInterface) {
            @Override
            protected void onReceive(
                    @Nonnull final InetAddress sourceAddress,
                    @Nonnull final byte[] data,
                    final int length) {
            }
        });
        final MulticastSocket socket = mock(MulticastSocket.class);
        doReturn(socket).when(server).createMulticastSocket(anyInt());

        final SsdpRequestMessage message = new SsdpRequestMessage();
        message.setMethod(SsdpMessage.M_SEARCH);
        message.setUri("*");
        message.setHeader(Http.HOST, SsdpServer.SSDP_ADDR + ":" + String.valueOf(SsdpServer.SSDP_PORT));
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER);
        message.setHeader(Http.MX, "1");
        message.setHeader(Http.ST, SsdpSearchServer.ST_ROOTDEVICE);

        server.open();
        server.send(message);

        verify(socket, times(1)).send(ArgumentMatchers.any(DatagramPacket.class));
    }

    @Test
    public void send_socketでExceptionが発生したら無視する() throws IOException {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServer server = spy(new SsdpServer(networkInterface) {
            @Override
            protected void onReceive(
                    @Nonnull final InetAddress sourceAddress,
                    @Nonnull final byte[] data,
                    final int length) {
            }
        });
        final MulticastSocket socket = mock(MulticastSocket.class);
        doReturn(socket).when(server).createMulticastSocket(anyInt());
        doThrow(new IOException()).when(socket).send(ArgumentMatchers.any(DatagramPacket.class));

        final SsdpRequestMessage message = new SsdpRequestMessage();
        message.setMethod(SsdpMessage.M_SEARCH);
        message.setUri("*");
        message.setHeader(Http.HOST, SsdpServer.SSDP_ADDR + ":" + String.valueOf(SsdpServer.SSDP_PORT));
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER);
        message.setHeader(Http.MX, "1");
        message.setHeader(Http.ST, SsdpSearchServer.ST_ROOTDEVICE);

        server.open();
        server.send(message);
    }

    @Test(timeout = 1000)
    public void ReceiveTask_shutdownRequest_start前にコールしても何も起こらない() throws Exception {
        final SsdpServer server = mock(SsdpServer.class);
        final MulticastSocket socket = mock(MulticastSocket.class);
        final ReceiveTask receiveTask = new ReceiveTask(server, socket, 0);

        receiveTask.shutdownRequest(false);
    }

    @Test(timeout = 1000)
    public void ReceiveTask_shutdownRequest_即抜け() throws Exception {
        final SsdpServer server = mock(SsdpServer.class);
        final MulticastSocket socket = mock(MulticastSocket.class);
        final ReceiveTask receiveTask = new ReceiveTask(server, socket, 0) {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (final InterruptedException ignored) {
                }
            }
        };
        receiveTask.start();
        receiveTask.shutdownRequest(false);
    }

    @Test(timeout = 1000)
    public void ReceiveTask_shutdownRequest_割り込みで終了() throws Exception {
        final SsdpServer server = mock(SsdpServer.class);
        final MulticastSocket socket = mock(MulticastSocket.class);
        final ReceiveTask receiveTask = new ReceiveTask(server, socket, 0) {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (final InterruptedException ignored) {
                }
            }
        };
        receiveTask.start();
        receiveTask.shutdownRequest(true);
    }

    @Test(timeout = 1000)
    public void ReceiveTask_shutdownRequest_終了待ちに割り込める() throws Exception {
        final SsdpServer server = mock(SsdpServer.class);
        final MulticastSocket socket = mock(MulticastSocket.class);
        final ReceiveTask receiveTask = new ReceiveTask(server, socket, 0) {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (final InterruptedException ignored) {
                }
                try {
                    Thread.sleep(2000);
                } catch (final InterruptedException ignored) {
                }
            }
        };
        receiveTask.start();
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                receiveTask.shutdownRequest(true);
            }
        });
        thread.start();
        thread.interrupt();
        thread.join();
    }

    @Test(timeout = 5000)
    public void ReceiveTask_スレッド内の処理_port0() throws Exception {
        final SsdpServer server = mock(SsdpServer.class);
        final MulticastSocket socket = spy(new MulticastSocket() {
            @Override
            public void joinGroup(final InetAddress mcastaddr) throws IOException {
            }

            @Override
            public void leaveGroup(final InetAddress mcastaddr) throws IOException {
            }

            @Override
            public synchronized void receive(final DatagramPacket p) throws IOException {
                try { // avoid busy loop
                    Thread.sleep(10);
                } catch (final InterruptedException ignored) {
                }
                p.setAddress(InetAddress.getByName("192.168.0.1"));
                p.setData(new byte[1]);
            }
        });
        final ReceiveTask receiveTask = spy(new ReceiveTask(server, socket, 0));

        receiveTask.start();
        Thread.sleep(1000);
        receiveTask.shutdownRequest(false);
        verify(receiveTask, times(1)).joinGroup();
        verify(receiveTask, times(1)).receiveLoop();
        verify(receiveTask, times(1)).leaveGroup();

        verify(socket, never()).joinGroup(ArgumentMatchers.any(InetAddress.class));
        verify(socket, never()).leaveGroup(ArgumentMatchers.any(InetAddress.class));
    }

    @Test(timeout = 5000)
    public void ReceiveTask_スレッド内の処理_port_non0() throws Exception {
        final SsdpServer server = mock(SsdpServer.class);
        final MulticastSocket socket = spy(new MulticastSocket() {
            @Override
            public void joinGroup(final InetAddress mcastaddr) throws IOException {
            }

            @Override
            public void leaveGroup(final InetAddress mcastaddr) throws IOException {
            }

            @Override
            public synchronized void receive(final DatagramPacket p) throws IOException {
                try { // avoid busy loop
                    Thread.sleep(10);
                } catch (final InterruptedException ignored) {
                }
                p.setAddress(InetAddress.getByName("192.168.0.1"));
                p.setData(new byte[1]);
            }
        });
        final ReceiveTask receiveTask = spy(new ReceiveTask(server, socket, 10));

        receiveTask.start();
        Thread.sleep(1000);
        receiveTask.shutdownRequest(false);
        verify(receiveTask, times(1)).joinGroup();
        verify(receiveTask, times(1)).receiveLoop();
        verify(receiveTask, times(1)).leaveGroup();

        verify(socket, times(1)).joinGroup(ArgumentMatchers.any(InetAddress.class));
        verify(socket, times(1)).leaveGroup(ArgumentMatchers.any(InetAddress.class));
    }

    @Test(timeout = 1000)
    public void ReceiveTask_receiveLoop_shutdown済みなら何もせず抜ける() throws Exception {
        final SsdpServer server = mock(SsdpServer.class);
        final MulticastSocket socket = mock(MulticastSocket.class);
        final ReceiveTask receiveTask = spy(new ReceiveTask(server, socket, 10));

        receiveTask.shutdownRequest(false);
        receiveTask.receiveLoop();

        verify(socket, never()).receive(ArgumentMatchers.any(DatagramPacket.class));
        verify(server, never()).onReceive(ArgumentMatchers.any(InetAddress.class), ArgumentMatchers.any(byte[].class), anyInt());
    }

    @Test(timeout = 1000)
    public void ReceiveTask_receiveLoop_exceptionが発生してもループを続ける() throws Exception {
        final SsdpServer server = mock(SsdpServer.class);
        final ReceiveTask[] tasks = new ReceiveTask[1];
        final MulticastSocket socket = spy(new MulticastSocket() {
            private int mCount;
            @Override
            public synchronized void receive(final DatagramPacket p) throws IOException {
                mCount++;
                if (mCount == 1) {
                    throw new SocketTimeoutException();
                }
                tasks[0].shutdownRequest(false);
            }
        });
        final ReceiveTask receiveTask = spy(new ReceiveTask(server, socket, 0));
        tasks[0] = receiveTask;
        receiveTask.receiveLoop();

        verify(socket, times(2)).receive(ArgumentMatchers.any(DatagramPacket.class));
        verify(server, never()).onReceive(ArgumentMatchers.any(InetAddress.class), ArgumentMatchers.any(byte[].class), anyInt());
    }

    @Test(timeout = 1000)
    public void ReceiveTask_run_exceptionが発生したらループを抜ける() throws Exception {
        final SsdpServer server = mock(SsdpServer.class);
        final MulticastSocket socket = mock(MulticastSocket.class);
        final ReceiveTask receiveTask = spy(new ReceiveTask(server, socket, 10));
        doThrow(new IOException()).when(socket).receive(ArgumentMatchers.any(DatagramPacket.class));

        receiveTask.run();
    }
}
