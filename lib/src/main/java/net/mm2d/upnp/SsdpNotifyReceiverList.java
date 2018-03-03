/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.log.Log;
import net.mm2d.upnp.SsdpNotifyReceiver.NotifyListener;

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
            @Nonnull final Collection<NetworkInterface> interfaces,
            @Nonnull final NotifyListener listener) {
        for (final NetworkInterface nif : interfaces) {
            final SsdpNotifyReceiver notify = newSsdpNotifyReceiver(nif);
            notify.setNotifyListener(listener);
            mList.add(notify);
        }
        return this;
    }

    // VisibleForTesting
    SsdpNotifyReceiver newSsdpNotifyReceiver(@Nonnull final NetworkInterface nif) {
        return new SsdpNotifyReceiver(nif);
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
