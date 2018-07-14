/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.ControlPoint.NotifyEventListener;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.Nonnull;

/**
 * 複数のNotifyEventListenerをまとめるためのクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
class NotifyEventListenerList implements NotifyEventListener {
    @Nonnull
    private final Set<NotifyEventListener> mSet = new CopyOnWriteArraySet<>();

    void add(@Nonnull final NotifyEventListener l) {
        mSet.add(l);
    }

    void remove(@Nonnull final NotifyEventListener l) {
        mSet.remove(l);
    }

    @Override
    public void onNotifyEvent(
            @Nonnull final Service service,
            final long seq,
            @Nonnull final String variable,
            @Nonnull final String value) {
        for (final NotifyEventListener l : mSet) {
            l.onNotifyEvent(service, seq, variable, value);
        }
    }
}
