/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.SsdpSearchServer.ResponseListener;
import net.mm2d.util.Log;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 全インターフェース分のSsdpSearchServerをまとめるためのクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class SsdpSearchServerList {
    private static final String TAG = SsdpSearchServerList.class.getSimpleName();
    @Nonnull
    private final List<SsdpSearchServer> mList;

    SsdpSearchServerList(final @Nonnull Collection<NetworkInterface> interfaces,
                         final @Nonnull ResponseListener listener) {
        mList = new ArrayList<>(interfaces.size());
        for (final NetworkInterface nif : interfaces) {
            final SsdpSearchServer search = new SsdpSearchServer(nif);
            search.setResponseListener(listener);
            mList.add(search);
        }
    }

    void openAndStart() {
        for (SsdpSearchServer server : mList) {
            try {
                server.open();
                server.start();
            } catch (final IOException e) {
                Log.w(TAG, e);
            }
        }
    }

    void stop() {
        for (SsdpSearchServer server : mList) {
            server.stop();
        }
    }

    void close() {
        for (SsdpSearchServer server : mList) {
            server.close();
        }
    }

    void search(final @Nullable String st) {
        for (final SsdpSearchServer server : mList) {
            server.search(st);
        }
    }
}
