/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample;

import net.mm2d.upnp.Device;
import net.mm2d.upnp.Service;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class DeviceNode extends UpnpNode {
    public DeviceNode(final Device device) {
        super(device);
        final List<Service> services = device.getServiceList();
        for (final Service service : services) {
            add(new ServiceNode(service));
        }
    }

    @Override
    public Device getUserObject() {
        return (Device) super.getUserObject();
    }

    @Override
    public String toString() {
        return getUserObject().getFriendlyName();
    }

    @Override
    public String getDetailText() {
        final Device device = getUserObject();
        return "UDN:  " +
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
        return formatXml(getUserObject().getDescription());
    }

    @Override
    public void showContextMenu(final JFrame frame, final Component invoker, final int x, final int y) {
        final JPopupMenu menu = new JPopupMenu();
        final JMenuItem open = new JMenuItem("Open Device Description");
        open.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Device device = getUserObject();
                try {
                    Desktop.getDesktop().browse(new URI(device.getLocation()));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
        });
        menu.add(open);
        menu.show(invoker, x, y);
    }
}
