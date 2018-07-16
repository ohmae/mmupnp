/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.SsdpSearchServer.ResponseListener;
import net.mm2d.upnp.SsdpServer.Address;
import net.mm2d.util.NetworkUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Collections;

import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class SsdpSearchServerListTest {
    @Test
    public void openAndStart() throws Exception {
        final SsdpSearchServerList list = spy(new SsdpSearchServerList());
        final SsdpSearchServer server = mock(SsdpSearchServer.class);
        final ResponseListener listener = mock(ResponseListener.class);
        doReturn(server).when(list).newSsdpSearchServer(eq(Address.IP_V4), any(NetworkInterface.class), eq(listener));

        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        list.init(Protocol.DEFAULT, Collections.singletonList(nif), listener);

        list.openAndStart();
        verify(server, times(1)).open();
        verify(server, times(1)).start();
    }

    @Test
    public void openAndStart_Exceptionが発生しても無視する() throws Exception {
        final SsdpSearchServerList list = spy(new SsdpSearchServerList());
        final SsdpSearchServer server = mock(SsdpSearchServer.class);
        final ResponseListener listener = mock(ResponseListener.class);
        doReturn(server).when(list).newSsdpSearchServer(eq(Address.IP_V4), any(NetworkInterface.class), eq(listener));

        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        list.init(Protocol.DEFAULT, Collections.singletonList(nif), listener);

        doThrow(new IOException()).when(server).open();
        list.openAndStart();
    }

    @Test
    public void stop() {
        final SsdpSearchServerList list = spy(new SsdpSearchServerList());
        final SsdpSearchServer server = mock(SsdpSearchServer.class);
        final ResponseListener listener = mock(ResponseListener.class);
        doReturn(server).when(list).newSsdpSearchServer(eq(Address.IP_V4), any(NetworkInterface.class), eq(listener));

        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        list.init(Protocol.DEFAULT, Collections.singletonList(nif), listener);

        list.stop();

        verify(server, times(1)).stop();
    }

    @Test
    public void close() {
        final SsdpSearchServerList list = spy(new SsdpSearchServerList());
        final SsdpSearchServer server = mock(SsdpSearchServer.class);
        final ResponseListener listener = mock(ResponseListener.class);
        doReturn(server).when(list).newSsdpSearchServer(eq(Address.IP_V4), any(NetworkInterface.class), eq(listener));

        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        list.init(Protocol.DEFAULT, Collections.singletonList(nif), listener);

        list.close();

        verify(server, times(1)).close();
    }

    @Test
    public void search() {
        final SsdpSearchServerList list = spy(new SsdpSearchServerList());
        final SsdpSearchServer server = mock(SsdpSearchServer.class);
        final ResponseListener listener = mock(ResponseListener.class);
        doReturn(server).when(list).newSsdpSearchServer(eq(Address.IP_V4), any(NetworkInterface.class), eq(listener));

        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        list.init(Protocol.DEFAULT, Collections.singletonList(nif), listener);

        list.search("");

        verify(server, times(1)).search("");
    }
}