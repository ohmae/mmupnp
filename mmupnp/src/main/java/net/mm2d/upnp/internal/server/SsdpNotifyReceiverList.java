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

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * 全インターフェース分のSsdpNotifyReceiverをまとめるためのクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class SsdpNotifyReceiverList {
    @Nonnull
    private final List<SsdpNotifyReceiver> mList = new ArrayList<>();

    @Nonnull
    public SsdpNotifyReceiverList init(
            @Nonnull final TaskExecutors executors,
            @Nonnull final Protocol protocol,
            @Nonnull final Iterable<NetworkInterface> interfaces,
            @Nonnull final NotifyListener listener) {
        for (final NetworkInterface nif : interfaces) {
            switch (protocol) {
                case IP_V4_ONLY:
                    if (NetworkUtils.isAvailableInet4Interface(nif)) {
                        mList.add(newSsdpNotifyReceiver(executors, Address.IP_V4, nif, listener));
                    }
                    break;
                case IP_V6_ONLY:
                    if (NetworkUtils.isAvailableInet6Interface(nif)) {
                        mList.add(newSsdpNotifyReceiver(executors, Address.IP_V6_LINK_LOCAL, nif, listener));
                    }
                    break;
                case DUAL_STACK:
                    if (NetworkUtils.isAvailableInet4Interface(nif)) {
                        mList.add(newSsdpNotifyReceiver(executors, Address.IP_V4, nif, listener));
                    }
                    if (NetworkUtils.isAvailableInet6Interface(nif)) {
                        mList.add(newSsdpNotifyReceiver(executors, Address.IP_V6_LINK_LOCAL, nif, listener));
                    }
                    break;
            }
        }
        return this;
    }

    // VisibleForTesting
    @Nonnull
    SsdpNotifyReceiver newSsdpNotifyReceiver(
            @Nonnull final TaskExecutors executors,
            @Nonnull final Address address,
            @Nonnull final NetworkInterface nif,
            @Nonnull final NotifyListener listener) {
        final SsdpNotifyReceiver receiver = new SsdpNotifyReceiver(executors, address, nif);
        receiver.setNotifyListener(listener);
        return receiver;
    }

    public void setSegmentCheckEnabled(final boolean enabled) {
        for (final SsdpNotifyReceiver receiver : mList) {
            receiver.setSegmentCheckEnabled(enabled);
        }
    }

    public void start() {
        for (final SsdpNotifyReceiver receiver : mList) {
            receiver.start();
        }
    }

    public void stop() {
        for (final SsdpNotifyReceiver receiver : mList) {
            receiver.stop();
        }
    }
}
