/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty;

import net.mm2d.upnp.Device;
import net.mm2d.upnp.HttpClient;
import net.mm2d.upnp.IconFilter;
import net.mm2d.upnp.SsdpMessage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class EmptyDeviceTest {

    @Test
    public void loadIconBinary() {
        final Device device = new EmptyDevice();
        device.loadIconBinary(mock(HttpClient.class), mock(IconFilter.class));
    }

    @Test
    public void getControlPoint() {
        final Device device = new EmptyDevice();
        assertThat(device.getControlPoint(), is(not(nullValue())));
    }

    @Test
    public void updateSsdpMessage() {
        final Device device = new EmptyDevice();
        device.updateSsdpMessage(mock(SsdpMessage.class));
    }

    @Test
    public void getSsdpMessage() {
        final Device device = new EmptyDevice();
        assertThat(device.getSsdpMessage(), is(not(nullValue())));
    }

    @Test
    public void getExpireTime() {
        final Device device = new EmptyDevice();
        assertThat(device.getExpireTime() >= 0, is(true));
    }

    @Test
    public void getDescription() {
        final Device device = new EmptyDevice();
        assertThat(device.getDescription(), is(not(nullValue())));
    }

    @Test
    public void getScopeId() {
        final Device device = new EmptyDevice();
        assertThat(device.getScopeId(), is(0));
    }

    @Test
    public void getValue() {
        final Device device = new EmptyDevice();
        assertThat(device.getValue(""), is(nullValue()));
    }

    @Test
    public void getValueWithNamespace() {
        final Device device = new EmptyDevice();
        assertThat(device.getValueWithNamespace("", ""), is(nullValue()));
    }

    @Test
    public void getLocation() {
        final Device device = new EmptyDevice();
        assertThat(device.getLocation(), is(not(nullValue())));
    }

    @Test
    public void getBaseUrl() {
        final Device device = new EmptyDevice();
        assertThat(device.getBaseUrl(), is(not(nullValue())));
    }

    @Test
    public void getIpAddress() {
        final Device device = new EmptyDevice();
        assertThat(device.getIpAddress(), is(not(nullValue())));
    }

    @Test
    public void getUdn() {
        final Device device = new EmptyDevice();
        assertThat(device.getUdn(), is(not(nullValue())));
    }

    @Test
    public void getUpc() {
        final Device device = new EmptyDevice();
        assertThat(device.getUpc(), is(nullValue()));
    }

    @Test
    public void getDeviceType() {
        final Device device = new EmptyDevice();
        assertThat(device.getDeviceType(), is(not(nullValue())));
    }

    @Test
    public void getFriendlyName() {
        final Device device = new EmptyDevice();
        assertThat(device.getFriendlyName(), is(not(nullValue())));
    }

    @Test
    public void getManufacture() {
        final Device device = new EmptyDevice();
        assertThat(device.getManufacture(), is(nullValue()));
    }

    @Test
    public void getManufactureUrl() {
        final Device device = new EmptyDevice();
        assertThat(device.getManufactureUrl(), is(nullValue()));
    }

    @Test
    public void getModelName() {
        final Device device = new EmptyDevice();
        assertThat(device.getModelName(), is(not(nullValue())));
    }

    @Test
    public void getModelUrl() {
        final Device device = new EmptyDevice();
        assertThat(device.getModelUrl(), is(nullValue()));
    }

    @Test
    public void getModelDescription() {
        final Device device = new EmptyDevice();
        assertThat(device.getModelDescription(), is(nullValue()));
    }

    @Test
    public void getModelNumber() {
        final Device device = new EmptyDevice();
        assertThat(device.getModelNumber(), is(nullValue()));
    }

    @Test
    public void getSerialNumber() {
        final Device device = new EmptyDevice();
        assertThat(device.getSerialNumber(), is(nullValue()));
    }

    @Test
    public void getPresentationUrl() {
        final Device device = new EmptyDevice();
        assertThat(device.getPresentationUrl(), is(nullValue()));
    }

    @Test
    public void getIconList() {
        final Device device = new EmptyDevice();
        assertThat(device.getIconList(), is(not(nullValue())));
    }

    @Test
    public void getServiceList() {
        final Device device = new EmptyDevice();
        assertThat(device.getServiceList(), is(not(nullValue())));
    }

    @Test
    public void findServiceById() {
        final Device device = new EmptyDevice();
        assertThat(device.findServiceById(""), is(nullValue()));
    }

    @Test
    public void findServiceByType() {
        final Device device = new EmptyDevice();
        assertThat(device.findServiceByType(""), is(nullValue()));
    }

    @Test
    public void findAction() {
        final Device device = new EmptyDevice();
        assertThat(device.findAction(""), is(nullValue()));
    }

    @Test
    public void isEmbeddedDevice() {
        final Device device = new EmptyDevice();
        assertThat(device.isEmbeddedDevice(), is(false));
    }

    @Test
    public void getParent() {
        final Device device = new EmptyDevice();
        assertThat(device.getParent(), is(nullValue()));
    }

    @Test
    public void getDeviceList() {
        final Device device = new EmptyDevice();
        assertThat(device.getDeviceList(), is(not(nullValue())));
    }

    @Test
    public void findDeviceByType() {
        final Device device = new EmptyDevice();
        assertThat(device.findDeviceByType(""), is(nullValue()));
    }

    @Test
    public void findDeviceByTypeRecursively() {
        final Device device = new EmptyDevice();
        assertThat(device.findDeviceByTypeRecursively(""), is(nullValue()));
    }

    @Test
    public void isPinned() {
        final Device device = new EmptyDevice();
        assertThat(device.isPinned(), is(false));
    }
}
