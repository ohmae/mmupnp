/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.testui;

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
        final StringBuilder sb = new StringBuilder();
        sb.append("uuid: ");
        sb.append(device.getUuid());
        sb.append('\n');
        sb.append("UDN:  ");
        sb.append(device.getUdn());
        sb.append('\n');
        sb.append("DeviceType: ");
        sb.append(device.getDeviceType());
        sb.append('\n');
        sb.append("FriendlyName: ");
        sb.append(device.getFriendlyName());
        sb.append('\n');
        sb.append("Manufacture: ");
        sb.append(device.getManufacture());
        sb.append('\n');
        sb.append("ManufactureUrl: ");
        sb.append(device.getManufactureUrl());
        sb.append('\n');
        sb.append("ModelName: ");
        sb.append(device.getModelName());
        sb.append('\n');
        sb.append("ModelUrl: ");
        sb.append(device.getModelUrl());
        sb.append('\n');
        sb.append("ModelDescription: ");
        sb.append(device.getModelDescription());
        sb.append('\n');
        sb.append("ModelNumber: ");
        sb.append(device.getModelNumber());
        sb.append('\n');
        sb.append("SerialNumber: ");
        sb.append(device.getSerialNumber());
        sb.append('\n');
        sb.append("PresentationUrl: ");
        sb.append(device.getPresentationUrl());
        return sb.toString();
    }

    @Override
    public String getDetailXml() {
        final Device device = (Device) getUserObject();
        return formatXml(device.getDescription());
    }
}
