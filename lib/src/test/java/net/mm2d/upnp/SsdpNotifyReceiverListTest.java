/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.SsdpNotifyReceiver.NotifyListener;
import net.mm2d.util.NetworkUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class SsdpNotifyReceiverListTest {

    @Test
    public void openAndStart() throws Exception {
        final SsdpNotifyReceiver receiver = mock(SsdpNotifyReceiver.class);
        final SsdpNotifyReceiverList list = spy(new SsdpNotifyReceiverList());
        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        doReturn(receiver).when(list).newSsdpNotifyReceiver(nif);
        list.init(Collections.singletonList(nif), mock(NotifyListener.class));

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
        when(list.newSsdpNotifyReceiver(nif))
                .thenReturn(receiver1)
                .thenReturn(receiver2);
        doThrow(new IOException()).when(receiver1).open();
        list.init(Arrays.asList(nif, nif), mock(NotifyListener.class));

        list.openAndStart();

        verify(receiver2, times(1)).open();
        verify(receiver2, times(1)).start();
    }

    @Test
    public void stop() {
        final SsdpNotifyReceiver receiver = mock(SsdpNotifyReceiver.class);
        final SsdpNotifyReceiverList list = spy(new SsdpNotifyReceiverList());
        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        doReturn(receiver).when(list).newSsdpNotifyReceiver(nif);
        list.init(Collections.singletonList(nif), mock(NotifyListener.class));

        list.stop();

        verify(receiver, times(1)).stop();
    }

    @Test
    public void close() {
        final SsdpNotifyReceiver receiver = mock(SsdpNotifyReceiver.class);
        final SsdpNotifyReceiverList list = spy(new SsdpNotifyReceiverList());
        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        doReturn(receiver).when(list).newSsdpNotifyReceiver(nif);
        list.init(Collections.singletonList(nif), mock(NotifyListener.class));

        list.close();

        verify(receiver, times(1)).close();
    }
}