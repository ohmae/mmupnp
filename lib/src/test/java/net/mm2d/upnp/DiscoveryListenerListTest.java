/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.ControlPoint.DiscoveryListener;

import org.junit.Test;

import javax.annotation.Nonnull;

import static org.mockito.Mockito.*;

public class DiscoveryListenerListTest {

    @Test
    public void onDiscover() {
        final Device device = mock(Device.class);
        final DiscoveryListenerList list = new DiscoveryListenerList();
        final DiscoveryListener listener = mock(DiscoveryListener.class);
        list.add(listener);
        list.onDiscover(device);
        verify(listener).onDiscover(device);
    }

    @Test
    public void onLost() {
        final Device device = mock(Device.class);
        final DiscoveryListenerList list = new DiscoveryListenerList();
        final DiscoveryListener listener = mock(DiscoveryListener.class);
        list.add(listener);
        list.onLost(device);
        verify(listener).onLost(device);
    }

    @Test
    public void removeOnDiscover() {
        final Device device = mock(Device.class);
        final DiscoveryListenerList list = new DiscoveryListenerList();
        list.add(new DiscoveryListener() {
            @Override
            public void onDiscover(@Nonnull final Device device) {
                list.remove(this);
            }

            @Override
            public void onLost(@Nonnull final Device device) {
                list.remove(this);
            }
        });
        list.add(new DiscoveryListener() {
            @Override
            public void onDiscover(@Nonnull final Device device) {
                list.remove(this);
            }

            @Override
            public void onLost(@Nonnull final Device device) {
                list.remove(this);
            }
        });
        list.onDiscover(device);
    }

    @Test
    public void addOnDiscover() {
        final Device device = mock(Device.class);
        final DiscoveryListenerList list = new DiscoveryListenerList();
        list.add(new DiscoveryListener() {
            @Override
            public void onDiscover(@Nonnull final Device device) {
                list.add(mock(DiscoveryListener.class));
            }

            @Override
            public void onLost(@Nonnull final Device device) {
                list.add(mock(DiscoveryListener.class));
            }
        });
        list.add(new DiscoveryListener() {
            @Override
            public void onDiscover(@Nonnull final Device device) {
                list.add(mock(DiscoveryListener.class));
            }

            @Override
            public void onLost(@Nonnull final Device device) {
                list.add(mock(DiscoveryListener.class));
            }
        });
        list.onDiscover(device);
    }
}