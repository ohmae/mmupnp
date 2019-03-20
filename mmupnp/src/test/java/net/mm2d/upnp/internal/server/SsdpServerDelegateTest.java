/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server;

import net.mm2d.upnp.Http;
import net.mm2d.upnp.MockMulticastSocket;
import net.mm2d.upnp.SsdpMessage;
import net.mm2d.upnp.internal.message.SsdpRequest;
import net.mm2d.upnp.internal.message.SsdpResponse;
import net.mm2d.upnp.internal.server.SsdpServerDelegate.Receiver;
import net.mm2d.upnp.internal.thread.TaskExecutors;
import net.mm2d.upnp.util.NetworkUtils;
import net.mm2d.upnp.util.TestUtils;

import org.junit.After;
import org.junit.Before;
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
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class SsdpServerDelegateTest {
    private TaskExecutors mTaskExecutors;

    @Before
    public void setUp() {
        mTaskExecutors = new TaskExecutors();
    }

    @After
    public void terminate() {
        mTaskExecutors.terminate();
    }

    @Test(timeout = 1000L)
    public void start_stop_デッドロックしない() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = new SsdpServerDelegate(mTaskExecutors, mock(Receiver.class), Address.IP_V4, networkInterface);
        server.start();
        server.stop();
    }

    @Test(timeout = 1000L)
    public void start_stop1_デッドロックしない() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = new SsdpServerDelegate(mTaskExecutors, mock(Receiver.class), Address.IP_V4, networkInterface);
        server.start();
        server.stop();
        server.stop();
    }

    @Test(timeout = 1000L)
    public void start_stop2_デッドロックしない() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = new SsdpServerDelegate(mTaskExecutors, mock(Receiver.class), Address.IP_V4, networkInterface);
        server.stop();
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
        SsdpServerDelegate.findInet4Address(Collections.emptyList());
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
        SsdpServerDelegate.findInet6Address(Collections.emptyList());
    }

    @Test
    public void getInterfaceAddress() {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = spy(new SsdpServerDelegate(mTaskExecutors, mock(Receiver.class), Address.IP_V4, networkInterface));
        assertThat(server.getInterfaceAddress(), is(SsdpServerDelegate.findInet4Address(networkInterface.getInterfaceAddresses())));
    }

    @Test
    public void start_stop() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = spy(new SsdpServerDelegate(mTaskExecutors, mock(Receiver.class), Address.IP_V4, networkInterface));
        final MulticastSocket socket = mock(MulticastSocket.class);
        doReturn(socket).when(server).createMulticastSocket(anyInt());
        server.start();
        server.start();
        verify(server, times(1)).stop();
        server.stop();
    }

    @Test
    public void send_open前は何も起こらない() throws IOException {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = spy(new SsdpServerDelegate(mTaskExecutors, mock(Receiver.class), Address.IP_V4, networkInterface));
        final MulticastSocket socket = mock(MulticastSocket.class);
        doReturn(socket).when(server).createMulticastSocket(anyInt());

        final SsdpRequest message = SsdpRequest.create();
        message.setMethod(SsdpMessage.M_SEARCH);
        message.setUri("*");
        message.setHeader(Http.HOST, server.getSsdpAddressString());
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER);
        message.setHeader(Http.MX, "1");
        message.setHeader(Http.ST, SsdpSearchServer.ST_ROOTDEVICE);

        server.send(message);

        verify(socket, never()).send(ArgumentMatchers.any(DatagramPacket.class));
    }

    @Test(timeout = 2000)
    public void send_socketから送信される() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = spy(new SsdpServerDelegate(mTaskExecutors, mock(Receiver.class), Address.IP_V4, networkInterface));
        final MockMulticastSocket socket = spy(new MockMulticastSocket());
        doReturn(socket).when(server).createMulticastSocket(anyInt());

        final SsdpRequest message = SsdpRequest.create();
        message.setMethod(SsdpMessage.M_SEARCH);
        message.setUri("*");
        message.setHeader(Http.HOST, server.getSsdpAddressString());
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER);
        message.setHeader(Http.MX, "1");
        message.setHeader(Http.ST, SsdpSearchServer.ST_ROOTDEVICE);

        server.start();
        server.send(message);
        Thread.sleep(100);
        server.stop();

        verify(socket, times(1)).send(ArgumentMatchers.any(DatagramPacket.class));

        final DatagramPacket packet = socket.getSendPacket();
        assertThat(packet.getAddress(), is(server.getSsdpInetAddress()));
        assertThat(packet.getPort(), is(SsdpServer.SSDP_PORT));
        assertThat(new String(packet.getData()), is(message.getMessage().getMessageString()));
    }

    @Test
    public void send_socketでExceptionが発生したら無視する() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = spy(new SsdpServerDelegate(mTaskExecutors, mock(Receiver.class), Address.IP_V4, networkInterface));
        final MulticastSocket socket = mock(MulticastSocket.class);
        doReturn(socket).when(server).createMulticastSocket(anyInt());
        doThrow(new IOException()).when(socket).send(ArgumentMatchers.any(DatagramPacket.class));

        final SsdpRequest message = SsdpRequest.create();
        message.setMethod(SsdpMessage.M_SEARCH);
        message.setUri("*");
        message.setHeader(Http.HOST, server.getSsdpAddressString());
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER);
        message.setHeader(Http.MX, "1");
        message.setHeader(Http.ST, SsdpSearchServer.ST_ROOTDEVICE);

        server.start();
        server.send(message);
        Thread.sleep(100);
        server.stop();
    }

    @Test
    public void setNotifyListener_受信メッセージが通知されること() throws Exception {
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final InetAddress address = InetAddress.getByName("192.0.2.2");

        final MockMulticastSocket socket = new MockMulticastSocket();
        socket.setReceiveData(address, data, 0);
        final Receiver receiver = mock(Receiver.class);
        final SsdpServerDelegate delegate = spy(new SsdpServerDelegate(mTaskExecutors, receiver, Address.IP_V4, networkInterface));
        doReturn(socket).when(delegate).createMulticastSocket(anyInt());

        delegate.start();
        Thread.sleep(1900);
        delegate.stop();

        final byte[] packetData = new byte[1500];
        System.arraycopy(data, 0, packetData, 0, data.length);
        verify(receiver, times(1)).onReceive(address, packetData, data.length);
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
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = spy(new SsdpServerDelegate(mTaskExecutors, mock(Receiver.class), Address.IP_V4, networkInterface));
        doReturn(socket).when(server).createMulticastSocket(anyInt());
        server.start();
        Thread.sleep(500);
        server.stop();
        Thread.sleep(100);
        verify(server, times(1)).receiveLoop();

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

        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = spy(new SsdpServerDelegate(mTaskExecutors, mock(Receiver.class), Address.IP_V4, networkInterface, 10));
        doReturn(socket).when(server).createMulticastSocket(anyInt());
        server.start();
        Thread.sleep(500);
        server.stop();
        Thread.sleep(100);

        verify(server, times(1)).receiveLoop();

        verify(socket, times(1)).joinGroup(ArgumentMatchers.any(InetAddress.class));
        verify(socket, times(1)).leaveGroup(ArgumentMatchers.any(InetAddress.class));
    }

    @Test(timeout = 5000)
    public void ReceiveTask_receiveLoop_exceptionが発生してもループを続ける() throws Exception {
        final Receiver receiver = mock(Receiver.class);
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = spy(new SsdpServerDelegate(mTaskExecutors, receiver, Address.IP_V4, networkInterface));
        final MulticastSocket socket = spy(new MulticastSocket() {
            private int mCount;

            @Override
            public synchronized void receive(final DatagramPacket p) throws IOException {
                mCount++;
                if (mCount == 1) {
                    throw new SocketTimeoutException();
                }
                server.stop();
            }
        });
        doReturn(socket).when(server).createMulticastSocket(anyInt());
        server.start();
        Thread.sleep(500);
        server.stop();

        verify(socket, times(2)).receive(ArgumentMatchers.any(DatagramPacket.class));
        verify(receiver, never()).onReceive(ArgumentMatchers.any(InetAddress.class), ArgumentMatchers.any(byte[].class), anyInt());
    }

    @Test(timeout = 1000)
    public void ReceiveTask_run_exceptionが発生したらループを抜ける() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = new SsdpServerDelegate(mTaskExecutors, mock(Receiver.class), Address.IP_V4, networkInterface);
        final MulticastSocket socket = mock(MulticastSocket.class);
        doThrow(new IOException()).when(socket).receive(ArgumentMatchers.any(DatagramPacket.class));

        server.run();
    }

    private static SsdpResponse makeFromResource(final String name) throws IOException {
        final byte[] data = TestUtils.getResourceAsByteArray(name);
        return SsdpResponse.create(mock(InetAddress.class), data, data.length);
    }

    @Test
    public void isInvalidLocation_アドレス一致() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = new SsdpServerDelegate(mTaskExecutors, mock(Receiver.class), Address.IP_V4, networkInterface);
        final SsdpResponse message = makeFromResource("ssdp-search-response0.bin");
        assertThat(server.isInvalidLocation(message, InetAddress.getByName("192.0.2.2")), is(false));
    }

    @Test
    public void isInvalidLocation_http以外() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = new SsdpServerDelegate(mTaskExecutors, mock(Receiver.class), Address.IP_V4, networkInterface);
        final SsdpResponse message = makeFromResource("ssdp-search-response-invalid-location0.bin");
        assertThat(server.isInvalidLocation(message, InetAddress.getByName("192.0.2.2")), is(true));
    }

    @Test
    public void isInvalidLocation_表記に問題() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpServerDelegate server = new SsdpServerDelegate(mTaskExecutors, mock(Receiver.class), Address.IP_V4, networkInterface);
        final SsdpResponse message = makeFromResource("ssdp-search-response-invalid-location1.bin");
        assertThat(server.isInvalidLocation(message, InetAddress.getByName("192.0.2.2")), is(true));
    }
}