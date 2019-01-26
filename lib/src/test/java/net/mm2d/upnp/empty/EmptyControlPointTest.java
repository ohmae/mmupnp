/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty;

import net.mm2d.upnp.ControlPoint;
import net.mm2d.upnp.ControlPoint.DiscoveryListener;
import net.mm2d.upnp.ControlPoint.NotifyEventListener;
import net.mm2d.upnp.IconFilter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class EmptyControlPointTest {

    @Test
    public void initialize() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        controlPoint.initialize();
    }

    @Test
    public void terminate() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        controlPoint.terminate();
    }

    @Test
    public void start() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        controlPoint.start();
    }

    @Test
    public void stop() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        controlPoint.stop();
    }

    @Test
    public void clearDeviceList() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        controlPoint.clearDeviceList();
    }

    @Test
    public void search() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        controlPoint.search();
    }

    @Test
    public void search1() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        controlPoint.search("upnp:rootdevice");
    }

    @Test
    public void setIconFilter() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        controlPoint.setIconFilter(IconFilter.NONE);
    }

    @Test
    public void addDiscoveryListener() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        controlPoint.addDiscoveryListener(mock(DiscoveryListener.class));
    }

    @Test
    public void removeDiscoveryListener() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        controlPoint.removeDiscoveryListener(mock(DiscoveryListener.class));
    }

    @Test
    public void addNotifyEventListener() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        controlPoint.addNotifyEventListener(mock(NotifyEventListener.class));
    }

    @Test
    public void removeNotifyEventListener() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        controlPoint.removeNotifyEventListener(mock(NotifyEventListener.class));
    }

    @Test
    public void getDeviceListSize() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        assertThat(controlPoint.getDeviceListSize(), is(0));
    }

    @Test
    public void getDeviceList() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        assertThat(controlPoint.getDeviceList(), is(not(nullValue())));
    }

    @Test
    public void getDevice() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        assertThat(controlPoint.getDevice(""), is(nullValue()));
    }

    @Test
    public void tryAddDevice() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        controlPoint.tryAddDevice("", "");
    }

    @Test
    public void addPinnedDevice() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        controlPoint.tryAddPinnedDevice("");
    }

    @Test
    public void removePinnedDevice() {
        final ControlPoint controlPoint = new EmptyControlPoint();
        controlPoint.removePinnedDevice("");
    }
}
