/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.SsdpServer.Address;
import net.mm2d.upnp.SsdpServerDelegate.ReceiveTask;
import net.mm2d.upnp.SsdpServerDelegate.Receiver;
import net.mm2d.util.NetworkUtils;
import net.mm2d.util.TestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class SsdpServerDelegateTest {
    @Test(timeout = 1000L)
    public void open_close_デッドロックしない() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = new SsdpServerDelegate(mock(Receiver.class), Address.IP_V4, networkInterface);
        server.open();
        server.close();
    }

    @Test(timeout = 1000L)
    public void start_stop_デッドロックしない() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = new SsdpServerDelegate(mock(Receiver.class), Address.IP_V4, networkInterface);
        server.open();
        server.start();
        server.stop();
        server.close();
    }

    @Test(timeout = 1000L)
    public void start_stop1_デッドロックしない() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = new SsdpServerDelegate(mock(Receiver.class), Address.IP_V4, networkInterface);
        server.open();
        server.start();
        server.stop(true);
        server.close();
    }

    @Test
    public void findInet4Address() throws Exception {
        final InterfaceAddress ipv4 = TestUtils.createInterfaceAddress("192.168.0.1", "255.255.255.0", 24);
        final InterfaceAddress ipv6 = TestUtils.createInterfaceAddress("fe80::a831:801b:8dc6:421f", "255.255.0.0", 16);
        assertThat(SsdpServerDelegate.findInet4Address(Arrays.asList(ipv4, ipv6)), is(ipv4));
        assertThat(SsdpServerDelegate.findInet4Address(Arrays.asList(ipv6, ipv4)), is(ipv4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void findInet4Address_見つからなければException1() throws Exception {
        final InterfaceAddress ipv6 = TestUtils.createInterfaceAddress("fe80::a831:801b:8dc6:421f", "255.255.0.0", 16);
        SsdpServerDelegate.findInet4Address(Arrays.asList(ipv6, ipv6));
    }

    @Test(expected = IllegalArgumentException.class)
    public void findInet4Address_見つからなければException2() throws Exception {
        SsdpServerDelegate.findInet4Address(Collections.<InterfaceAddress>emptyList());
    }

    @Test
    public void findInet6Address() throws Exception {
        final InterfaceAddress ipv4 = TestUtils.createInterfaceAddress("192.168.0.1", "255.255.255.0", 24);
        final InterfaceAddress ipv6 = TestUtils.createInterfaceAddress("fe80::a831:801b:8dc6:421f", "255.255.0.0", 16);
        assertThat(SsdpServerDelegate.findInet6Address(Arrays.asList(ipv4, ipv6)), is(ipv6));
        assertThat(SsdpServerDelegate.findInet6Address(Arrays.asList(ipv6, ipv4)), is(ipv6));
    }

    @Test(expected = IllegalArgumentException.class)
    public void findInet6Address_見つからなければException1() throws Exception {
        final InterfaceAddress ipv4 = TestUtils.createInterfaceAddress("192.168.0.1", "255.255.255.0", 24);
        SsdpServerDelegate.findInet6Address(Collections.singletonList(ipv4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void findInet6Address_見つからなければException2() throws Exception {
        SsdpServerDelegate.findInet6Address(Collections.<InterfaceAddress>emptyList());
    }

    @Test
    public void open_close() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = spy(new SsdpServerDelegate(mock(Receiver.class), Address.IP_V4, networkInterface));
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
        final SsdpServerDelegate server = spy(new SsdpServerDelegate(mock(Receiver.class), Address.IP_V4, networkInterface));
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
        final SsdpServerDelegate server = spy(new SsdpServerDelegate(mock(Receiver.class), Address.IP_V4, networkInterface));
        final MulticastSocket socket = mock(MulticastSocket.class);
        doReturn(socket).when(server).createMulticastSocket(anyInt());

        final SsdpRequest message = new SsdpRequest();
        message.setMethod(SsdpMessage.M_SEARCH);
        message.setUri("*");
        message.setHeader(Http.HOST, server.getSsdpAddressString());
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER);
        message.setHeader(Http.MX, "1");
        message.setHeader(Http.ST, SsdpSearchServer.ST_ROOTDEVICE);

        server.send(message);

        verify(socket, never()).send(ArgumentMatchers.any(DatagramPacket.class));
    }

    @Test
    public void send_socketから送信される() throws IOException {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = spy(new SsdpServerDelegate(mock(Receiver.class), Address.IP_V4, networkInterface));
        final MockMulticastSocket socket = spy(new MockMulticastSocket());
        doReturn(socket).when(server).createMulticastSocket(anyInt());

        final SsdpRequest message = new SsdpRequest();
        message.setMethod(SsdpMessage.M_SEARCH);
        message.setUri("*");
        message.setHeader(Http.HOST, server.getSsdpAddressString());
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER);
        message.setHeader(Http.MX, "1");
        message.setHeader(Http.ST, SsdpSearchServer.ST_ROOTDEVICE);

        server.open();
        server.send(message);

        verify(socket, times(1)).send(ArgumentMatchers.any(DatagramPacket.class));

        final DatagramPacket packet = socket.getSendPacket();
        assertThat(packet.getAddress(), is(server.getSsdpInetAddress()));
        assertThat(packet.getPort(), is(SsdpServer.SSDP_PORT));
        assertThat(new String(packet.getData()), is(message.getMessage().getMessageString()));
    }

    @Test
    public void send_socketでExceptionが発生したら無視する() throws IOException {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = spy(new SsdpServerDelegate(mock(Receiver.class), Address.IP_V4, networkInterface));
        final MulticastSocket socket = mock(MulticastSocket.class);
        doReturn(socket).when(server).createMulticastSocket(anyInt());
        doThrow(new IOException()).when(socket).send(ArgumentMatchers.any(DatagramPacket.class));

        final SsdpRequest message = new SsdpRequest();
        message.setMethod(SsdpMessage.M_SEARCH);
        message.setUri("*");
        message.setHeader(Http.HOST, server.getSsdpAddressString());
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER);
        message.setHeader(Http.MX, "1");
        message.setHeader(Http.ST, SsdpSearchServer.ST_ROOTDEVICE);

        server.open();
        server.send(message);
    }

    @Test
    public void setNotifyListener_受信メッセージが通知されること() throws Exception {
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final InetAddress address = InetAddress.getByName("192.0.2.2");

        final MockMulticastSocket socket = new MockMulticastSocket();
        socket.setReceiveData(address, data, 0);
        final Receiver receiver = mock(Receiver.class);
        final SsdpServerDelegate delegate = spy(new SsdpServerDelegate(receiver, Address.IP_V4, networkInterface));
        doReturn(socket).when(delegate).createMulticastSocket(anyInt());

        delegate.open();
        delegate.start();
        Thread.sleep(1900);
        delegate.stop(true);
        delegate.close();

        final byte[] packetData = new byte[1500];
        System.arraycopy(data, 0, packetData, 0, data.length);
        verify(receiver, times(1)).onReceive(address, packetData, data.length);
    }

    @Test(timeout = 1000)
    public void ReceiveTask_shutdownRequest_start前にコールしても何も起こらない() throws Exception {
        final MulticastSocket socket = mock(MulticastSocket.class);
        final ReceiveTask receiveTask = new ReceiveTask(mock(Receiver.class), socket, Address.IP_V4.getInetAddress(), 0);

        receiveTask.shutdownRequest(false);
    }

    @Test(timeout = 1000)
    public void ReceiveTask_shutdownRequest_即抜け() throws Exception {
        final MulticastSocket socket = mock(MulticastSocket.class);
        final ReceiveTask receiveTask = new ReceiveTask(mock(Receiver.class), socket, Address.IP_V4.getInetAddress(), 0) {
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
        final MulticastSocket socket = mock(MulticastSocket.class);
        final ReceiveTask receiveTask = new ReceiveTask(mock(Receiver.class), socket, Address.IP_V4.getInetAddress(), 0) {
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
        final MulticastSocket socket = mock(MulticastSocket.class);
        final ReceiveTask receiveTask = new ReceiveTask(mock(Receiver.class), socket, Address.IP_V4.getInetAddress(), 0) {
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
        final ReceiveTask receiveTask = spy(new ReceiveTask(mock(Receiver.class), socket, Address.IP_V4.getInetAddress(), 0));

        receiveTask.start();
        Thread.sleep(500);
        receiveTask.shutdownRequest(true);
        verify(receiveTask, times(1)).joinGroup();
        verify(receiveTask, times(1)).receiveLoop();
        verify(receiveTask, times(1)).leaveGroup();

        verify(socket, never()).joinGroup(ArgumentMatchers.any(InetAddress.class));
        verify(socket, never()).leaveGroup(ArgumentMatchers.any(InetAddress.class));
    }

    @Test(timeout = 5000)
    public void ReceiveTask_スレッド内の処理_port_non0() throws Exception {
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
        final ReceiveTask receiveTask = spy(new ReceiveTask(mock(Receiver.class), socket, Address.IP_V4.getInetAddress(), 10));

        receiveTask.start();
        Thread.sleep(500);
        receiveTask.shutdownRequest(true);
        verify(receiveTask, times(1)).joinGroup();
        verify(receiveTask, times(1)).receiveLoop();
        verify(receiveTask, times(1)).leaveGroup();

        verify(socket, times(1)).joinGroup(ArgumentMatchers.any(InetAddress.class));
        verify(socket, times(1)).leaveGroup(ArgumentMatchers.any(InetAddress.class));
    }

    @Test(timeout = 1000)
    public void ReceiveTask_receiveLoop_shutdown済みなら何もせず抜ける() throws Exception {
        final Receiver receiver = mock(Receiver.class);
        final MulticastSocket socket = mock(MulticastSocket.class);
        final ReceiveTask receiveTask = spy(new ReceiveTask(receiver, socket, Address.IP_V4.getInetAddress(), 10));

        receiveTask.shutdownRequest(false);
        receiveTask.receiveLoop();

        verify(socket, never()).receive(ArgumentMatchers.any(DatagramPacket.class));
        verify(receiver, never()).onReceive(ArgumentMatchers.any(InetAddress.class), ArgumentMatchers.any(byte[].class), anyInt());
    }

    @Test(timeout = 1000)
    public void ReceiveTask_receiveLoop_exceptionが発生してもループを続ける() throws Exception {
        final Receiver receiver = mock(Receiver.class);
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
        final ReceiveTask receiveTask = spy(new ReceiveTask(receiver, socket, Address.IP_V4.getInetAddress(), 0));
        tasks[0] = receiveTask;
        receiveTask.receiveLoop();

        verify(socket, times(2)).receive(ArgumentMatchers.any(DatagramPacket.class));
        verify(receiver, never()).onReceive(ArgumentMatchers.any(InetAddress.class), ArgumentMatchers.any(byte[].class), anyInt());
    }

    @Test(timeout = 1000)
    public void ReceiveTask_run_exceptionが発生したらループを抜ける() throws Exception {
        final Receiver receiver = mock(Receiver.class);
        final MulticastSocket socket = mock(MulticastSocket.class);
        final ReceiveTask receiveTask = spy(new ReceiveTask(receiver, socket, Address.IP_V4.getInetAddress(), 10));
        doThrow(new IOException()).when(socket).receive(ArgumentMatchers.any(DatagramPacket.class));

        receiveTask.run();
    }

    private static SsdpResponse makeFromResource(final String name) throws IOException {
        final byte[] data = TestUtils.getResourceAsByteArray(name);
        return new SsdpResponse(mock(InterfaceAddress.class), data, data.length);
    }

    @Test
    public void isInvalidLocation_アドレス一致() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = new SsdpServerDelegate(mock(Receiver.class), Address.IP_V4, networkInterface);
        final SsdpResponse message = makeFromResource("ssdp-search-response0.bin");
        assertThat(server.isInvalidLocation(message, InetAddress.getByName("192.0.2.2")), is(false));
    }

    @Test
    public void isInvalidLocation_http以外() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = new SsdpServerDelegate(mock(Receiver.class), Address.IP_V4, networkInterface);
        final SsdpResponse message = makeFromResource("ssdp-search-response-invalid-location0.bin");
        assertThat(server.isInvalidLocation(message, InetAddress.getByName("192.0.2.2")), is(true));
    }

    @Test
    public void isInvalidLocation_表記に問題() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = new SsdpServerDelegate(mock(Receiver.class), Address.IP_V4, networkInterface);
        final SsdpResponse message = makeFromResource("ssdp-search-response-invalid-location1.bin");
        assertThat(server.isInvalidLocation(message, InetAddress.getByName("192.0.2.2")), is(true));
    }
}