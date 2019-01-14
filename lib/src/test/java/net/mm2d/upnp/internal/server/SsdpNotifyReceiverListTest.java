/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server;

import net.mm2d.upnp.Protocol;
import net.mm2d.upnp.internal.server.SsdpNotifyReceiver.NotifyListener;
import net.mm2d.upnp.internal.server.SsdpServer.Address;
import net.mm2d.util.NetworkUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class SsdpNotifyReceiverListTest {

    @Test
    public void openAndStart() throws Exception {
        final SsdpNotifyReceiver receiver = mock(SsdpNotifyReceiver.class);
        final SsdpNotifyReceiverList list = spy(new SsdpNotifyReceiverList());
        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final NotifyListener listener = mock(NotifyListener.class);
        doReturn(receiver).when(list).newSsdpNotifyReceiver(Address.IP_V4, nif, listener);
        list.init(Protocol.DEFAULT, Collections.singletonList(nif), listener);

        list.openAndStart();

        verify(receiver, times(1)).open();
        verify(receiver, times(1)).start();
    }

    @Test
    public void openAndStart_Exceptionが発生しても後続の処理は実行する() throws Exception {
        final SsdpNotifyReceiver receiver1 = mock(SsdpNotifyReceiver.class);
        final SsdpNotifyReceiver receiver2 = mock(SsdpNotifyReceiver.class);
        final SsdpNotifyReceiverList list = spy(new SsdpNotifyReceiverList());
        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final NotifyListener listener = mock(NotifyListener.class);
        when(list.newSsdpNotifyReceiver(Address.IP_V4, nif, listener))
                .thenReturn(receiver1)
                .thenReturn(receiver2);
        doThrow(new IOException()).when(receiver1).open();
        list.init(Protocol.DEFAULT, Arrays.asList(nif, nif), listener);

        list.openAndStart();

        verify(receiver2, times(1)).open();
        verify(receiver2, times(1)).start();
    }

    @Test
    public void stop() {
        final SsdpNotifyReceiver receiver = mock(SsdpNotifyReceiver.class);
        final SsdpNotifyReceiverList list = spy(new SsdpNotifyReceiverList());
        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final NotifyListener listener = mock(NotifyListener.class);
        doReturn(receiver).when(list).newSsdpNotifyReceiver(Address.IP_V4, nif, listener);
        list.init(Protocol.DEFAULT, Collections.singletonList(nif), listener);

        list.stop();

        verify(receiver, times(1)).stop();
    }

    @Test
    public void close() {
        final SsdpNotifyReceiver receiver = mock(SsdpNotifyReceiver.class);
        final SsdpNotifyReceiverList list = spy(new SsdpNotifyReceiverList());
        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final NotifyListener listener = mock(NotifyListener.class);
        doReturn(receiver).when(list).newSsdpNotifyReceiver(Address.IP_V4, nif, listener);
        list.init(Protocol.DEFAULT, Collections.singletonList(nif), listener);

        list.close();

        verify(receiver, times(1)).close();
    }
}
