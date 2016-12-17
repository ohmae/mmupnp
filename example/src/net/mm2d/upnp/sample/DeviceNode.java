/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample;

import net.mm2d.upnp.Device;
import net.mm2d.upnp.Service;

import java.util.List;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class DeviceNode extends UpnpNode {
    public DeviceNode(Device device) {
        super(device);
        final List<Service> services = device.getServiceList();
        for (final Service service : services) {
            add(new ServiceNode(service));
        }
    }

    @Override
    public String toString() {
        final Device device = (Device) getUserObject();
        return device.getFriendlyName();
    }

    @Override
    public String getDetailText() {
        final Device device = (Device) getUserObject();
        return "uuid: " +
                device.getUuid() +
                '\n' +
                "UDN:  " +
                device.getUdn() +
                '\n' +
                "DeviceType: " +
                device.getDeviceType() +
                '\n' +
                "FriendlyName: " +
                device.getFriendlyName() +
                '\n' +
                "Manufacture: " +
                device.getManufacture() +
                '\n' +
                "ManufactureUrl: " +
                device.getManufactureUrl() +
                '\n' +
                "ModelName: " +
                device.getModelName() +
                '\n' +
                "ModelUrl: " +
                device.getModelUrl() +
                '\n' +
                "ModelDescription: " +
                device.getModelDescription() +
                '\n' +
                "ModelNumber: " +
                device.getModelNumber() +
                '\n' +
                "SerialNumber: " +
                device.getSerialNumber() +
                '\n' +
                "PresentationUrl: " +
                device.getPresentationUrl();
    }

    @Override
    public String getDetailXml() {
        final Device device = (Device) getUserObject();
        return formatXml(device.getDescription());
    }
}
