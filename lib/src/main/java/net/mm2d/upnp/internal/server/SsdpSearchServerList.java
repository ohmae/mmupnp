/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server;

import net.mm2d.log.Logger;
import net.mm2d.upnp.Protocol;
import net.mm2d.upnp.internal.server.SsdpSearchServer.ResponseListener;
import net.mm2d.upnp.internal.server.SsdpServer.Address;
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
public class SsdpSearchServerList {
    @Nonnull
    private final List<SsdpSearchServer> mList = new ArrayList<>();

    public SsdpSearchServerList init(
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

    public void openAndStart() {
        for (final SsdpSearchServer server : mList) {
            try {
                server.open();
                server.start();
            } catch (final IOException e) {
                Logger.w(e);
            }
        }
    }

    public void stop() {
        for (final SsdpSearchServer server : mList) {
            server.stop();
        }
    }

    public void close() {
        for (final SsdpSearchServer server : mList) {
            server.close();
        }
    }

    public void search(@Nullable final String st) {
        for (final SsdpSearchServer server : mList) {
            server.search(st);
        }
    }
}
