/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.ControlPoint.NotifyEventListener;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * 複数のNotifyEventListenerをまとめるためのクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class NotifyEventListenerList implements NotifyEventListener {
    @Nonnull
    private final List<NotifyEventListener> mList = new ArrayList<>();

    synchronized void add(@Nonnull final NotifyEventListener l) {
        if (!mList.contains(l)) {
            mList.add(l);
        }
    }

    synchronized void remove(@Nonnull final NotifyEventListener l) {
        mList.remove(l);
    }

    @Override
    public synchronized void onNotifyEvent(
            @Nonnull final Service service, final long seq,
            @Nonnull final String variable, @Nonnull final String value) {
        for (final NotifyEventListener l : mList) {
            l.onNotifyEvent(service, seq, variable, value);
        }
    }
}
