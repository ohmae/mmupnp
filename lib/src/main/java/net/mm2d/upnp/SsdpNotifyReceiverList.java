/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.SsdpNotifyReceiver.NotifyListener;
import net.mm2d.util.Log;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * 全インターフェース分のSsdpNotifyReceiverをまとめるためのクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class SsdpNotifyReceiverList {
    @Nonnull
    private final List<SsdpNotifyReceiver> mList;

    SsdpNotifyReceiverList(@Nonnull final Collection<NetworkInterface> interfaces,
                           @Nonnull final NotifyListener listener) {
        mList = new ArrayList<>(interfaces.size());
        for (final NetworkInterface nif : interfaces) {
            final SsdpNotifyReceiver notify = new SsdpNotifyReceiver(nif);
            notify.setNotifyListener(listener);
            mList.add(notify);
        }
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
