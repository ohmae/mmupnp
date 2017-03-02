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
    private final List<NotifyEventListener> mList = new ArrayList<>();

    synchronized void add(final @Nonnull NotifyEventListener l) {
        if (!mList.contains(l)) {
            mList.add(l);
        }
    }

    synchronized void remove(final @Nonnull NotifyEventListener l) {
        mList.remove(l);
    }

    @Override
    public synchronized void onNotifyEvent(
            final @Nonnull Service service, final long seq,
            final @Nonnull String variable, final @Nonnull String value) {
        for (final NotifyEventListener l : mList) {
            l.onNotifyEvent(service, seq, variable, value);
        }
    }
}
