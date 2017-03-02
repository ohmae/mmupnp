/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.ControlPoint.DiscoveryListener;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * 複数のDiscoveryListenerをまとめるためのクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class DiscoveryListenerList implements DiscoveryListener {
    private final List<DiscoveryListener> mList = new ArrayList<>();

    synchronized void add(final @Nonnull DiscoveryListener l) {
        if (!mList.contains(l)) {
            mList.add(l);
        }
    }

    synchronized void remove(final @Nonnull DiscoveryListener l) {
        mList.remove(l);
    }

    @Override
    public synchronized void onDiscover(final @Nonnull Device device) {
        for (final DiscoveryListener l : mList) {
            l.onDiscover(device);
        }
    }

    @Override
    public synchronized void onLost(final @Nonnull Device device) {
        for (final DiscoveryListener l : mList) {
            l.onLost(device);
        }
    }
}
