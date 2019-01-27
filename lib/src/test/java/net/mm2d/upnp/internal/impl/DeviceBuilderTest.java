/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl;

import net.mm2d.upnp.ControlPoint;
import net.mm2d.upnp.Device;
import net.mm2d.upnp.HttpClient;
import net.mm2d.upnp.Icon;
import net.mm2d.upnp.Service;
import net.mm2d.upnp.SsdpMessage;
import net.mm2d.upnp.internal.manager.SubscribeManager;
import net.mm2d.upnp.internal.message.FakeSsdpMessage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.InetAddress;
import java.util.Collections;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class DeviceBuilderTest {
    @Test
    public void build() {
        final ControlPoint cp = mock(ControlPoint.class);
        final SubscribeManager manager = mock(SubscribeManager.class);
        final SsdpMessage message = mock(SsdpMessage.class);
        final String location = "location";
        final String uuid = "uuid";
        doReturn(location).when(message).getLocation();
        doReturn(uuid).when(message).getUuid();
        final String description = "description";
        final String upc = "upc";
        final String deviceType = "deviceType";
        final String friendlyName = "friendlyName";
        final String manufacture = "manufacture";
        final String manufactureUrl = "manufactureUrl";
        final String modelName = "modelName";
        final String modelUrl = "modelUrl";
        final String modelDescription = "modelDescription";
        final String modelNumber = "modelNumber";
        final String serialNumber = "serialNumber";
        final String presentationUrl = "presentationUrl";
        final String urlBase = "urlBase";
        final Icon icon = mock(Icon.class);
        final Service service = mock(Service.class);
        final ServiceImpl.Builder serviceBuilder = mock(ServiceImpl.Builder.class);
        doReturn(serviceBuilder).when(serviceBuilder).setDevice(any(Device.class));
        doReturn(serviceBuilder).when(serviceBuilder).setSubscribeManager(any(SubscribeManager.class));
        doReturn(service).when(serviceBuilder).build();

        final Device device = new DeviceImpl.Builder(cp, manager, message)
                .setDescription(description)
                .setUdn(uuid)
                .setUpc(upc)
                .setDeviceType(deviceType)
                .setFriendlyName(friendlyName)
                .setManufacture(manufacture)
                .setManufactureUrl(manufactureUrl)
                .setModelName(modelName)
                .setModelUrl(modelUrl)
                .setModelDescription(modelDescription)
                .setModelNumber(modelNumber)
                .setSerialNumber(serialNumber)
                .setPresentationUrl(presentationUrl)
                .setUrlBase(urlBase)
                .addIcon(icon)
                .addServiceBuilder(serviceBuilder)
                .build();

        assertThat(device.getDescription(), is(description));
        assertThat(device.getUdn(), is(uuid));
        assertThat(device.getUpc(), is(upc));
        assertThat(device.getDeviceType(), is(deviceType));
        assertThat(device.getFriendlyName(), is(friendlyName));
        assertThat(device.getManufacture(), is(manufacture));
        assertThat(device.getManufactureUrl(), is(manufactureUrl));
        assertThat(device.getModelName(), is(modelName));
        assertThat(device.getModelUrl(), is(modelUrl));
        assertThat(device.getModelDescription(), is(modelDescription));
        assertThat(device.getModelNumber(), is(modelNumber));
        assertThat(device.getSerialNumber(), is(serialNumber));
        assertThat(device.getPresentationUrl(), is(presentationUrl));
        assertThat(device.getBaseUrl(), is(urlBase));
        assertThat(device.getIconList(), contains(icon));
        assertThat(device.getServiceList(), contains(service));
    }

    @Test
    public void build_最低限のパラメータ() {
        final ControlPoint cp = mock(ControlPoint.class);
        final SubscribeManager manager = mock(SubscribeManager.class);
        final SsdpMessage message = mock(SsdpMessage.class);
        final String location = "location";
        final String uuid = "uuid";
        doReturn(location).when(message).getLocation();
        doReturn(uuid).when(message).getUuid();
        final String description = "description";
        final String upc = "upc";
        final String deviceType = "deviceType";
        final String friendlyName = "friendlyName";
        final String manufacture = "manufacture";
        final String modelName = "modelName";

        final Device device = new DeviceImpl.Builder(cp, manager, message)
                .setDescription(description)
                .setUdn(uuid)
                .setUpc(upc)
                .setDeviceType(deviceType)
                .setFriendlyName(friendlyName)
                .setManufacture(manufacture)
                .setModelName(modelName)
                .build();

        assertThat(device.getDescription(), is(description));
        assertThat(device.getUdn(), is(uuid));
        assertThat(device.getUpc(), is(upc));
        assertThat(device.getDeviceType(), is(deviceType));
        assertThat(device.getFriendlyName(), is(friendlyName));
        assertThat(device.getManufacture(), is(manufacture));
        assertThat(device.getModelName(), is(modelName));
    }

    @Test(expected = IllegalStateException.class)
    public void build_Description不足() {
        final ControlPoint cp = mock(ControlPoint.class);
        final SubscribeManager manager = mock(SubscribeManager.class);
        final SsdpMessage message = mock(SsdpMessage.class);
        final String location = "location";
        final String uuid = "uuid";
        doReturn(location).when(message).getLocation();
        doReturn(uuid).when(message).getUuid();
        final String deviceType = "deviceType";
        final String friendlyName = "friendlyName";
        final String manufacture = "manufacture";
        final String modelName = "modelName";

        new DeviceImpl.Builder(cp, manager, message)
                .setUdn(uuid)
                .setDeviceType(deviceType)
                .setFriendlyName(friendlyName)
                .setManufacture(manufacture)
                .setModelName(modelName)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_DeviceType不足() {
        final ControlPoint cp = mock(ControlPoint.class);
        final SubscribeManager manager = mock(SubscribeManager.class);
        final SsdpMessage message = mock(SsdpMessage.class);
        final String location = "location";
        final String uuid = "uuid";
        doReturn(location).when(message).getLocation();
        doReturn(uuid).when(message).getUuid();
        final String description = "description";
        final String friendlyName = "friendlyName";
        final String manufacture = "manufacture";
        final String modelName = "modelName";

        new DeviceImpl.Builder(cp, manager, message)
                .setDescription(description)
                .setUdn(uuid)
                .setFriendlyName(friendlyName)
                .setManufacture(manufacture)
                .setModelName(modelName)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_FriendlyName不足() {
        final ControlPoint cp = mock(ControlPoint.class);
        final SubscribeManager manager = mock(SubscribeManager.class);
        final SsdpMessage message = mock(SsdpMessage.class);
        final String location = "location";
        final String uuid = "uuid";
        doReturn(location).when(message).getLocation();
        doReturn(uuid).when(message).getUuid();
        final String description = "description";
        final String deviceType = "deviceType";
        final String manufacture = "manufacture";
        final String modelName = "modelName";

        new DeviceImpl.Builder(cp, manager, message)
                .setDescription(description)
                .setUdn(uuid)
                .setDeviceType(deviceType)
                .setManufacture(manufacture)
                .setModelName(modelName)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_Manufacture不足() {
        final ControlPoint cp = mock(ControlPoint.class);
        final SubscribeManager manager = mock(SubscribeManager.class);
        final SsdpMessage message = mock(SsdpMessage.class);
        final String location = "location";
        final String uuid = "uuid";
        doReturn(location).when(message).getLocation();
        doReturn(uuid).when(message).getUuid();
        final String description = "description";
        final String deviceType = "deviceType";
        final String friendlyName = "friendlyName";
        final String modelName = "modelName";

        new DeviceImpl.Builder(cp, manager, message)
                .setDescription(description)
                .setUdn(uuid)
                .setDeviceType(deviceType)
                .setFriendlyName(friendlyName)
                .setModelName(modelName)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_ModelName不足() {
        final ControlPoint cp = mock(ControlPoint.class);
        final SubscribeManager manager = mock(SubscribeManager.class);
        final SsdpMessage message = mock(SsdpMessage.class);
        final String location = "location";
        final String uuid = "uuid";
        doReturn(location).when(message).getLocation();
        doReturn(uuid).when(message).getUuid();
        final String description = "description";
        final String deviceType = "deviceType";
        final String friendlyName = "friendlyName";
        final String manufacture = "manufacture";

        new DeviceImpl.Builder(cp, manager, message)
                .setDescription(description)
                .setUdn(uuid)
                .setDeviceType(deviceType)
                .setFriendlyName(friendlyName)
                .setManufacture(manufacture)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_Udn不足() {
        final ControlPoint cp = mock(ControlPoint.class);
        final SubscribeManager manager = mock(SubscribeManager.class);
        final SsdpMessage message = mock(SsdpMessage.class);
        final String location = "location";
        final String uuid = "uuid";
        doReturn(location).when(message).getLocation();
        doReturn(uuid).when(message).getUuid();
        final String description = "description";
        final String deviceType = "deviceType";
        final String friendlyName = "friendlyName";
        final String manufacture = "manufacture";
        final String modelName = "modelName";

        new DeviceImpl.Builder(cp, manager, message)
                .setDescription(description)
                .setDeviceType(deviceType)
                .setFriendlyName(friendlyName)
                .setManufacture(manufacture)
                .setModelName(modelName)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_Udn不一致() {
        final ControlPoint cp = mock(ControlPoint.class);
        final SubscribeManager manager = mock(SubscribeManager.class);
        final SsdpMessage message = mock(SsdpMessage.class);
        final String location = "location";
        final String uuid = "uuid";
        doReturn(location).when(message).getLocation();
        doReturn(uuid).when(message).getUuid();
        final String description = "description";
        final String udn = "udn";
        final String upc = "upc";
        final String deviceType = "deviceType";
        final String friendlyName = "friendlyName";
        final String manufacture = "manufacture";
        final String modelName = "modelName";

        new DeviceImpl.Builder(cp, manager, message)
                .setDescription(description)
                .setUdn(udn)
                .setUpc(upc)
                .setDeviceType(deviceType)
                .setFriendlyName(friendlyName)
                .setManufacture(manufacture)
                .setModelName(modelName)
                .build();
    }

    @Test
    public void build_PinnedSsdpMessage_update() {
        final SsdpMessage message = mock(FakeSsdpMessage.class);
        doReturn("location").when(message).getLocation();
        doReturn(true).when(message).isPinned();
        final SsdpMessage newMessage = mock(SsdpMessage.class);
        final DeviceImpl.Builder builder = new DeviceImpl.Builder(mock(ControlPoint.class), mock(SubscribeManager.class), message);
        builder.updateSsdpMessage(newMessage);
        assertThat(builder.getSsdpMessage(), is(message));
    }

    @Test(expected = IllegalArgumentException.class)
    public void build_不正なSsdpMessage1() {
        new DeviceImpl.Builder(mock(ControlPoint.class), mock(SubscribeManager.class), mock(SsdpMessage.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void build_不正なSsdpMessage2() {
        final SsdpMessage message = mock(SsdpMessage.class);
        final String location = "location";
        final String uuid = "uuid";
        doReturn(location).when(message).getLocation();
        doReturn(uuid).when(message).getUuid();
        new DeviceImpl.Builder(mock(ControlPoint.class), mock(SubscribeManager.class), message)
                .updateSsdpMessage(mock(SsdpMessage.class));
    }

    @Test
    public void updateSsdpMessage_EmbeddedDeviceへの伝搬() {
        final SsdpMessage message = mock(SsdpMessage.class);
        final String location = "location";
        final String uuid = "uuid";
        doReturn(location).when(message).getLocation();
        doReturn(uuid).when(message).getUuid();
        final DeviceImpl.Builder embeddedDeviceBuilder = mock(DeviceImpl.Builder.class);
        new DeviceImpl.Builder(mock(ControlPoint.class), mock(SubscribeManager.class), message)
                .setEmbeddedDeviceBuilderList(Collections.singletonList(embeddedDeviceBuilder))
                .updateSsdpMessage(message);

        verify(embeddedDeviceBuilder, times(1)).updateSsdpMessage(message);
    }

    @Test
    public void putTag_namespaceのnullと空白は等価() {
        final SsdpMessage message = mock(SsdpMessage.class);
        doReturn("location").when(message).getLocation();
        doReturn("uuid").when(message).getUuid();
        final String tag1 = "tag1";
        final String value1 = "value1";
        final String tag2 = "tag2";
        final String value2 = "value2";

        final Device device = new DeviceImpl.Builder(mock(ControlPoint.class), mock(SubscribeManager.class), message)
                .setDescription("description")
                .setUdn("uuid")
                .setUpc("upc")
                .setDeviceType("deviceType")
                .setFriendlyName("friendlyName")
                .setManufacture("manufacture")
                .setModelName("modelName")
                .putTag(null, tag1, value1)
                .putTag("", tag2, value2)
                .build();
        assertThat(device.getValueWithNamespace("", tag1), is(value1));
        assertThat(device.getValueWithNamespace("", tag2), is(value2));
    }

    @Test()
    public void onDownloadDescription() throws Exception {
        final FakeSsdpMessage message = mock(FakeSsdpMessage.class);
        doReturn("location").when(message).getLocation();
        final DeviceImpl.Builder builder = new DeviceImpl.Builder(mock(ControlPoint.class), mock(SubscribeManager.class), message);
        final HttpClient client = mock(HttpClient.class);
        final InetAddress address = InetAddress.getByName("127.0.0.1");
        doReturn(address).when(client).getLocalAddress();
        builder.setDownloadInfo(client);

        verify(client).getLocalAddress();
        verify(message).setLocalAddress(address);
    }

    @Test(expected = IllegalStateException.class)
    public void onDownloadDescription_before_download() {
        final FakeSsdpMessage message = mock(FakeSsdpMessage.class);
        doReturn("location").when(message).getLocation();
        final DeviceImpl.Builder builder = new DeviceImpl.Builder(mock(ControlPoint.class), mock(SubscribeManager.class), message);
        final HttpClient client = new HttpClient();
        builder.setDownloadInfo(client);
    }
}
