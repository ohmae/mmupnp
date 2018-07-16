/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.log.Log;
import net.mm2d.upnp.SsdpSearchServer.ResponseListener;
import net.mm2d.upnp.SsdpServer.Address;
import net.mm2d.util.NetworkUtils;

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
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
class SsdpSearchServerList {
    @Nonnull
    private final List<SsdpSearchServer> mList = new ArrayList<>();

    SsdpSearchServerList init(
            @Nonnull final Protocol protocol,
            @Nonnull final Collection<NetworkInterface> interfaces,
            @Nonnull final ResponseListener listener) {
        for (final NetworkInterface nif : interfaces) {
            switch (protocol) {
                case IP_V4_ONLY:
                    if (NetworkUtils.isAvailableInet4Interface(nif)) {
                        mList.add(newSsdpSearchServer(Address.IP_V4, nif, listener));
                    }
                    break;
                case IP_V6_ONLY:
                    if (NetworkUtils.isAvailableInet6Interface(nif)) {
                        mList.add(newSsdpSearchServer(Address.IP_V6_LINK_LOCAL, nif, listener));
                    }
                    break;
                case DUAL_STACK:
                    if (NetworkUtils.isAvailableInet4Interface(nif)) {
                        mList.add(newSsdpSearchServer(Address.IP_V4, nif, listener));
                    }
                    if (NetworkUtils.isAvailableInet6Interface(nif)) {
                        mList.add(newSsdpSearchServer(Address.IP_V6_LINK_LOCAL, nif, listener));
                    }
                    break;
            }
        }
        return this;
    }

    // VisibleForTesting
    SsdpSearchServer newSsdpSearchServer(
            @Nonnull final Address address,
            @Nonnull final NetworkInterface nif,
            @Nonnull final ResponseListener listener) {
        final SsdpSearchServer server = new SsdpSearchServer(address, nif);
        server.setResponseListener(listener);
        return server;
    }

    void openAndStart() {
        for (final SsdpSearchServer server : mList) {
            try {
                server.open();
                server.start();
            } catch (final IOException e) {
                Log.w(e);
            }
        }
    }

    void stop() {
        for (final SsdpSearchServer server : mList) {
            server.stop();
        }
    }

    void close() {
        for (final SsdpSearchServer server : mList) {
            server.close();
        }
    }

    void search(@Nullable final String st) {
        for (final SsdpSearchServer server : mList) {
            server.search(st);
        }
    }
}
