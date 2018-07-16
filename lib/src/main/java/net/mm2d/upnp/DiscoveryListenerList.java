/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.ControlPoint.DiscoveryListener;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.Nonnull;

/**
 * 複数のDiscoveryListenerをまとめるためのクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
class DiscoveryListenerList implements DiscoveryListener {
    @Nonnull
    private final Set<DiscoveryListener> mSet = new CopyOnWriteArraySet<>();

    void add(@Nonnull final DiscoveryListener l) {
        mSet.add(l);
    }

    void remove(@Nonnull final DiscoveryListener l) {
        mSet.remove(l);
    }

    @Override
    public void onDiscover(@Nonnull final Device device) {
        for (final DiscoveryListener l : mSet) {
            l.onDiscover(device);
        }
    }

    @Override
    public void onLost(@Nonnull final Device device) {
        for (final DiscoveryListener l : mSet) {
            l.onLost(device);
        }
    }
}
