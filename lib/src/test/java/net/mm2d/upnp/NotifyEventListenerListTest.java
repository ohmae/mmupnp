/*
 * Copyright(C)  2018 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.ControlPoint.NotifyEventListener;

import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class NotifyEventListenerListTest {
    @Test
    public void onNotifyEvent_通知される() {
        final Service service = mock(Service.class);
        final long seq = 0;
        final String variable = "variable";
        final String value = "variable";
        final NotifyEventListenerList list = new NotifyEventListenerList();
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        list.add(listener);
        list.onNotifyEvent(service, seq, variable, value);

        verify(listener, times(1)).onNotifyEvent(service, seq, variable, value);
    }

    @Test
    public void onNotifyEvent_削除したら通知されない() {
        final Service service = mock(Service.class);
        final long seq = 0;
        final String variable = "variable";
        final String value = "variable";
        final NotifyEventListenerList list = new NotifyEventListenerList();
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        list.add(listener);
        list.remove(listener);
        list.onNotifyEvent(service, seq, variable, value);

        verify(listener, never()).onNotifyEvent(service, seq, variable, value);
    }

    @Test
    public void onNotifyEvent_2回追加しても通知されるのは1回() {
        final Service service = mock(Service.class);
        final long seq = 0;
        final String variable = "variable";
        final String value = "variable";
        final NotifyEventListenerList list = new NotifyEventListenerList();
        final NotifyEventListener listener = mock(NotifyEventListener.class);
        list.add(listener);
        list.add(listener);
        list.onNotifyEvent(service, seq, variable, value);

        verify(listener, times(1)).onNotifyEvent(service, seq, variable, value);
    }
}