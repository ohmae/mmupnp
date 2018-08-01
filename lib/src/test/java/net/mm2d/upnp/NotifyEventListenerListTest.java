/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.ControlPoint.NotifyEventListener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.annotation.Nonnull;

import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
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

    @Test
    public void removeOnNotify() {
        final Service service = mock(Service.class);
        final long seq = 0;
        final String variable = "variable";
        final String value = "variable";
        final NotifyEventListenerList list = new NotifyEventListenerList();
        list.add(new NotifyEventListener() {
            @Override
            public void onNotifyEvent(
                    @Nonnull final Service service,
                    final long seq,
                    @Nonnull final String variable,
                    @Nonnull final String value) {
                list.remove(this);
            }
        });
        list.add(new NotifyEventListener() {
            @Override
            public void onNotifyEvent(
                    @Nonnull final Service service,
                    final long seq,
                    @Nonnull final String variable,
                    @Nonnull final String value) {
                list.remove(this);
            }
        });
        list.onNotifyEvent(service, seq, variable, value);
    }

    @Test
    public void addOnNotify() {
        final Service service = mock(Service.class);
        final long seq = 0;
        final String variable = "variable";
        final String value = "variable";
        final NotifyEventListenerList list = new NotifyEventListenerList();
        list.add((service1, seq1, variable1, value1) ->
                list.add(mock(NotifyEventListener.class)));
        list.add((service2, seq2, variable2, value2) ->
                list.add(mock(NotifyEventListener.class)));
        list.onNotifyEvent(service, seq, variable, value);
    }
}