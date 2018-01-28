/*
 * Copyright(C)  2018 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.SsdpSearchServer.ResponseListener;
import net.mm2d.util.NetworkUtils;

import org.junit.Test;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Arrays;

import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class SsdpSearchServerListTest {
    @Test
    public void openAndStart() throws Exception {
        final SsdpSearchServerList list = spy(new SsdpSearchServerList());
        final SsdpSearchServer server = mock(SsdpSearchServer.class);
        doReturn(server).when(list).newSsdpSearchServer(any(NetworkInterface.class));

        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        list.init(Arrays.asList(nif), mock(ResponseListener.class));

        list.openAndStart();
        verify(server, times(1)).open();
        verify(server, times(1)).start();
    }

    @Test
    public void openAndStart_Exceptionが発生しても無視する() throws Exception {
        final SsdpSearchServerList list = spy(new SsdpSearchServerList());
        final SsdpSearchServer server = mock(SsdpSearchServer.class);
        doReturn(server).when(list).newSsdpSearchServer(any(NetworkInterface.class));

        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        list.init(Arrays.asList(nif), mock(ResponseListener.class));

        doThrow(new IOException()).when(server).open();
        list.openAndStart();
    }

    @Test
    public void stop() {
        final SsdpSearchServerList list = spy(new SsdpSearchServerList());
        final SsdpSearchServer server = mock(SsdpSearchServer.class);
        doReturn(server).when(list).newSsdpSearchServer(any(NetworkInterface.class));

        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        list.init(Arrays.asList(nif), mock(ResponseListener.class));

        list.stop();

        verify(server, times(1)).stop();
    }

    @Test
    public void close() {
        final SsdpSearchServerList list = spy(new SsdpSearchServerList());
        final SsdpSearchServer server = mock(SsdpSearchServer.class);
        doReturn(server).when(list).newSsdpSearchServer(any(NetworkInterface.class));

        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        list.init(Arrays.asList(nif), mock(ResponseListener.class));

        list.close();

        verify(server, times(1)).close();
    }

    @Test
    public void search() {
        final SsdpSearchServerList list = spy(new SsdpSearchServerList());
        final SsdpSearchServer server = mock(SsdpSearchServer.class);
        doReturn(server).when(list).newSsdpSearchServer(any(NetworkInterface.class));

        final NetworkInterface nif = NetworkUtils.getAvailableInet4Interfaces().get(0);
        list.init(Arrays.asList(nif), mock(ResponseListener.class));

        list.search("");

        verify(server, times(1)).search("");
    }
}