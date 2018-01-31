/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.SsdpSearchServer.ResponseListener;
import net.mm2d.util.NetworkUtils;
import net.mm2d.util.TestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatchers;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.List;

import javax.annotation.Nonnull;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class SsdpSearchServerTest {
    @Test(timeout = 1000L)
    public void open_close_デッドロックしない() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpSearchServer server = new SsdpSearchServer(networkInterface);
        server.open();
        server.close();
    }

    @Test(timeout = 1000L)
    public void start_stop_デッドロックしない() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpSearchServer server = new SsdpSearchServer(networkInterface);
        server.open();
        server.start();
        server.stop();
        server.close();
    }

    @Test(timeout = 1000L)
    public void start_stop1_デッドロックしない() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpSearchServer server = new SsdpSearchServer(networkInterface);
        server.open();
        server.start();
        server.stop(true);
        server.close();
    }

    @Test
    public void search_ST_ALLでのサーチ() throws Exception {
        final MockMulticastSocket socket = new MockMulticastSocket();
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpSearchServer server = spy(new SsdpSearchServer(networkInterface));
        doReturn(socket).when(server).createMulticastSocket(anyInt());
        server.open();
        server.start();
        server.search();
        server.stop(true);
        server.close();

        final DatagramPacket packet = socket.getSendPacket();
        final SsdpRequestMessage message = new SsdpRequestMessage(
                mock(InterfaceAddress.class), packet.getData(), packet.getLength());
        assertThat(message.getMethod(), is(SsdpMessage.M_SEARCH));
        assertThat(message.getHeader(Http.ST), is(SsdpSearchServer.ST_ALL));
        assertThat(message.getHeader(Http.MAN), is(SsdpMessage.SSDP_DISCOVER));
    }

    @Test
    public void search_ST_ROOTDEVICEでのサーチ() throws Exception {
        final MockMulticastSocket socket = new MockMulticastSocket();
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpSearchServer server = spy(new SsdpSearchServer(networkInterface));
        doReturn(socket).when(server).createMulticastSocket(anyInt());
        server.open();
        server.start();
        server.search(SsdpSearchServer.ST_ROOTDEVICE);
        server.stop(true);
        server.close();

        final DatagramPacket packet = socket.getSendPacket();
        final SsdpRequestMessage message = new SsdpRequestMessage(
                mock(InterfaceAddress.class), packet.getData(), packet.getLength());
        assertThat(message.getMethod(), is(SsdpMessage.M_SEARCH));
        assertThat(message.getHeader(Http.ST), is(SsdpSearchServer.ST_ROOTDEVICE));
        assertThat(message.getHeader(Http.MAN), is(SsdpMessage.SSDP_DISCOVER));
    }

    @Test
    public void setResponseListener_受信メッセージが通知されること() throws Exception {
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin");
        final SsdpResponseMessage message = new SsdpResponseMessage(mock(InterfaceAddress.class), data, data.length);
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final InterfaceAddress address = findInet4Address(networkInterface);
        message.setHeader(Http.LOCATION, "http://" + address.getAddress().getHostAddress() + "/");
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.getMessage().writeData(baos);

        final MockMulticastSocket socket = new MockMulticastSocket();
        socket.setReceiveData(address.getAddress(), baos.toByteArray(), 0);
        final SsdpSearchServer server = spy(new SsdpSearchServer(networkInterface));
        doReturn(socket).when(server).createMulticastSocket(anyInt());
        final SsdpResponseMessage result[] = new SsdpResponseMessage[1];
        server.setResponseListener(new SsdpSearchServer.ResponseListener() {
            @Override
            public void onReceiveResponse(@Nonnull final SsdpResponseMessage message) {
                result[0] = message;
            }
        });
        server.open();
        server.start();
        Thread.sleep(100);
        server.stop(true);
        server.close();

        assertThat(result[0].getStatus(), is(Http.Status.HTTP_OK));
        assertThat(result[0].getUuid(), is("uuid:01234567-89ab-cdef-0123-456789abcdef"));
    }

    @Test
    public void onReceive_listenerがコールされる() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpSearchServer server = new SsdpSearchServer(networkInterface);
        final ResponseListener listener = mock(ResponseListener.class);
        server.setResponseListener(listener);
        final InetAddress address = InetAddress.getByName("192.0.2.2");
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin");

        server.onReceive(address, data, data.length);

        verify(listener, times(1)).onReceiveResponse(ArgumentMatchers.any(SsdpResponseMessage.class));
    }

    @Test
    public void onReceive_アドレス不一致ならlistenerコールされない() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpSearchServer server = new SsdpSearchServer(networkInterface);
        final ResponseListener listener = mock(ResponseListener.class);
        server.setResponseListener(listener);
        final InetAddress address = InetAddress.getByName("192.0.2.3");
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin");

        server.onReceive(address, data, data.length);

        verify(listener, never()).onReceiveResponse(ArgumentMatchers.any(SsdpResponseMessage.class));
    }

    @Test
    public void onReceive_データに問題がある場合listenerコールされない() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpSearchServer server = new SsdpSearchServer(networkInterface);
        final ResponseListener listener = mock(ResponseListener.class);
        server.setResponseListener(listener);
        final InetAddress address = InetAddress.getByName("192.0.2.2");
        final byte[] data = new byte[0];

        server.onReceive(address, data, data.length);

        verify(listener, never()).onReceiveResponse(ArgumentMatchers.any(SsdpResponseMessage.class));
    }

    @Test
    public void onReceive_listenerが登録されていなくてもクラッシュしない() throws Exception {
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final SsdpSearchServer server = new SsdpSearchServer(networkInterface);

        final InetAddress address = InetAddress.getByName("192.0.2.2");
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin");

        server.onReceive(address, data, data.length);
    }

    private static InterfaceAddress findInet4Address(final NetworkInterface networkInterface) {
        final List<InterfaceAddress> addressList = networkInterface.getInterfaceAddresses();
        for (final InterfaceAddress address : addressList) {
            if (address.getAddress() instanceof Inet4Address) {
                return address;
            }
        }
        throw new IllegalArgumentException("ni does not have IPv4 address.");
    }
}