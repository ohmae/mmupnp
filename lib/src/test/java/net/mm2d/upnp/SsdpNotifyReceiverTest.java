/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.NetworkUtils;
import net.mm2d.util.TestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.List;

import javax.annotation.Nonnull;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class SsdpNotifyReceiverTest {
    @Test
    public void setNotifyListener_受信メッセージが通知されること() throws Exception {
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
        final SsdpRequestMessage message = new SsdpRequestMessage(mock(InterfaceAddress.class), data, data.length);
        final NetworkInterface networkInterface = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final InterfaceAddress address = findInet4Address(networkInterface);
        message.setHeader(Http.LOCATION, "http://" + address.getAddress().getHostAddress() + "/");
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.getMessage().writeData(baos);

        final MockMulticastSocket socket = new MockMulticastSocket();
        socket.setReceiveData(address.getAddress(), baos.toByteArray(), 0);
        final SsdpNotifyReceiver receiver = new SsdpNotifyReceiver(networkInterface) {
            @Nonnull
            @Override
            MulticastSocket createMulticastSocket(final int port) throws IOException {
                return socket;
            }
        };
        final SsdpRequestMessage result[] = new SsdpRequestMessage[1];
        receiver.setNotifyListener(new SsdpNotifyReceiver.NotifyListener() {
            @Override
            public void onReceiveNotify(@Nonnull final SsdpRequestMessage message) {
                result[0] = message;
            }
        });
        receiver.open();
        receiver.start();
        Thread.sleep(100);
        receiver.stop(true);
        receiver.close();

        assertThat(result[0].getUuid(), is("uuid:01234567-89ab-cdef-0123-456789abcdef"));
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