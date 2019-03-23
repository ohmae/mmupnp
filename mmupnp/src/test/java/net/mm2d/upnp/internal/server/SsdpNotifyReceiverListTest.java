/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server;

import net.mm2d.upnp.Protocol;
import net.mm2d.upnp.internal.server.SsdpNotifyReceiver.NotifyListener;
import net.mm2d.upnp.internal.thread.TaskExecutors;
import net.mm2d.upnp.util.NetworkUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.NetworkInterface;
import java.util.Collections;

import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class SsdpNotifyReceiverListTest {
    @Test
    public void openAndStart() {
        final SsdpNotifyReceiver receiver = mock(SsdpNotifyReceiver.class);
        final SsdpNotifyReceiverList list = spy(new SsdpNotifyReceiverList());
        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final NotifyListener listener = mock(NotifyListener.class);
        final TaskExecutors executors = mock(TaskExecutors.class);
        doReturn(receiver).when(list).newSsdpNotifyReceiver(executors, Address.IP_V4, nif, listener);
        list.init(executors, Protocol.DEFAULT, Collections.singletonList(nif), listener);

        list.start();

        verify(receiver, times(1)).start();
    }

    @Test
    public void stop() {
        final SsdpNotifyReceiver receiver = mock(SsdpNotifyReceiver.class);
        final SsdpNotifyReceiverList list = spy(new SsdpNotifyReceiverList());
        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        final NotifyListener listener = mock(NotifyListener.class);
        final TaskExecutors executors = mock(TaskExecutors.class);
        doReturn(receiver).when(list).newSsdpNotifyReceiver(executors, Address.IP_V4, nif, listener);
        list.init(executors, Protocol.DEFAULT, Collections.singletonList(nif), listener);

        list.stop();

        verify(receiver, times(1)).stop();
    }
}
