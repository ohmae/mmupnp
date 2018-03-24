/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.log.Log;
import net.mm2d.upnp.SsdpNotifyReceiver.NotifyListener;
import net.mm2d.upnp.SsdpServer.Address;
import net.mm2d.util.NetworkUtils;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * 全インターフェース分のSsdpNotifyReceiverをまとめるためのクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
class SsdpNotifyReceiverList {
    @Nonnull
    private final List<SsdpNotifyReceiver> mList = new ArrayList<>();

    SsdpNotifyReceiverList init(
            @Nonnull final Protocol protocol,
            @Nonnull final Collection<NetworkInterface> interfaces,
            @Nonnull final NotifyListener listener) {
        for (final NetworkInterface nif : interfaces) {
            switch (protocol) {
                case IP_V4_ONLY:
                    if (NetworkUtils.isAvailableInet4Interface(nif)) {
                        mList.add(newSsdpNotifyReceiver(Address.IP_V4, nif, listener));
                    }
                    break;
                case IP_V6_ONLY:
                    if (NetworkUtils.isAvailableInet6Interface(nif)) {
                        mList.add(newSsdpNotifyReceiver(Address.IP_V6_LINK_LOCAL, nif, listener));
                    }
                    break;
                case DUAL_STACK:
                    if (NetworkUtils.isAvailableInet4Interface(nif)) {
                        mList.add(newSsdpNotifyReceiver(Address.IP_V4, nif, listener));
                    }
                    if (NetworkUtils.isAvailableInet6Interface(nif)) {
                        mList.add(newSsdpNotifyReceiver(Address.IP_V6_LINK_LOCAL, nif, listener));
                    }
                    break;
            }
        }
        return this;
    }

    // VisibleForTesting
    @Nonnull
    SsdpNotifyReceiver newSsdpNotifyReceiver(
            @Nonnull final Address address,
            @Nonnull final NetworkInterface nif,
            @Nonnull final NotifyListener listener) {
        final SsdpNotifyReceiver receiver = new SsdpNotifyReceiver(address, nif);
        receiver.setNotifyListener(listener);
        return receiver;
    }

    void openAndStart() {
        for (final SsdpNotifyReceiver receiver : mList) {
            try {
                receiver.open();
                receiver.start();
            } catch (final IOException e) {
                Log.w(e);
            }
        }
    }

    void stop() {
        for (final SsdpNotifyReceiver receiver : mList) {
            receiver.stop();
        }
    }

    void close() {
        for (final SsdpNotifyReceiver receiver : mList) {
            receiver.close();
        }
    }
}
