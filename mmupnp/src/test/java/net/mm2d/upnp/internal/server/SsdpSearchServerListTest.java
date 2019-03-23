/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server;

import net.mm2d.upnp.Protocol;
import net.mm2d.upnp.internal.server.SsdpSearchServer.ResponseListener;
import net.mm2d.upnp.internal.thread.TaskExecutors;
import net.mm2d.upnp.util.NetworkUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.NetworkInterface;
import java.util.Collections;

import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class SsdpSearchServerListTest {
    private TaskExecutors mTaskExecutors;

    @Before
    public void setUp() {
        mTaskExecutors = new TaskExecutors();
    }

    @After
    public void terminate() {
        mTaskExecutors.terminate();
    }

    @Test
    public void start() {
        final SsdpSearchServerList list = spy(new SsdpSearchServerList());
        final SsdpSearchServer server = mock(SsdpSearchServer.class);
        final ResponseListener listener = mock(ResponseListener.class);
        doReturn(server).when(list).newSsdpSearchServer(any(), eq(Address.IP_V4), any(NetworkInterface.class), eq(listener));

        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        list.init(mTaskExecutors, Protocol.DEFAULT, Collections.singletonList(nif), listener);

        list.start();
        verify(server, times(1)).start();
    }

    @Test
    public void stop() {
        final SsdpSearchServerList list = spy(new SsdpSearchServerList());
        final SsdpSearchServer server = mock(SsdpSearchServer.class);
        final ResponseListener listener = mock(ResponseListener.class);
        doReturn(server).when(list).newSsdpSearchServer(any(), eq(Address.IP_V4), any(NetworkInterface.class), eq(listener));

        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        list.init(mTaskExecutors, Protocol.DEFAULT, Collections.singletonList(nif), listener);

        list.stop();

        verify(server, times(1)).stop();
    }

    @Test
    public void search() {
        final SsdpSearchServerList list = spy(new SsdpSearchServerList());
        final SsdpSearchServer server = mock(SsdpSearchServer.class);
        final ResponseListener listener = mock(ResponseListener.class);
        doReturn(server).when(list).newSsdpSearchServer(any(), eq(Address.IP_V4), any(NetworkInterface.class), eq(listener));

        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        list.init(mTaskExecutors, Protocol.DEFAULT, Collections.singletonList(nif), listener);

        list.search("");

        verify(server, times(1)).search("");
    }
}
