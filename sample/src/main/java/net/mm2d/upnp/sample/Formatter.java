/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample;

import net.mm2d.upnp.Action;
import net.mm2d.upnp.Argument;
import net.mm2d.upnp.Device;
import net.mm2d.upnp.Service;
import net.mm2d.upnp.StateVariable;

import java.util.List;

import javax.annotation.Nonnull;

public class Formatter {
    @Nonnull
    public static String format(@Nonnull final Device device) {
        return "Location: " +
                device.getLocation() +
                "\nUDN: " +
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

    @Nonnull
    public static String format(@Nonnull final Service service) {
        return "ServiceType: " +
                service.getServiceType() +
                '\n' +
                "ServiceId: " +
                service.getServiceId() +
                '\n' +
                "ScpdUrl: " +
                service.getScpdUrl() +
                '\n' +
                "ControlUrl: " +
                service.getControlUrl() +
                '\n' +
                "EventSubUrl: " +
                service.getEventSubUrl();
    }

    @Nonnull
    public static String format(@Nonnull final Action action) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Name: ");
        sb.append(action.getName());
        sb.append('\n');
        final List<Argument> args = action.getArgumentList();
        for (final Argument arg : args) {
            final StateVariable v = arg.getRelatedStateVariable();
            sb.append(arg.isInputDirection() ? "in:" : "out:");
            sb.append("(");
            sb.append(v.getDataType());
            sb.append(")");
            sb.append(arg.getName());
            sb.append('\n');
        }
        return sb.toString();
    }

    @Nonnull
    public static String format(@Nonnull final Argument argument) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Name: ");
        sb.append(argument.getName());
        sb.append('\n');
        sb.append("Direction: ");
        sb.append(argument.isInputDirection() ? "in" : "out");
        sb.append('\n');
        sb.append('\n');
        sb.append("RelatedStateVariable:\n");
        sb.append(format(argument.getRelatedStateVariable()));
        return sb.toString();
    }

    @Nonnull
    public static String format(@Nonnull final StateVariable stateVariable) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Name: ");
        sb.append(stateVariable.getName());
        sb.append('\n');
        sb.append("SendEvents: ");
        sb.append(stateVariable.isSendEvents() ? "yes" : "no");
        sb.append('\n');
        sb.append("Multicast: ");
        sb.append(stateVariable.isMulticast() ? "yes" : "no");
        sb.append('\n');
        sb.append("DataType: ");
        sb.append(stateVariable.getDataType());
        final List<String> list = stateVariable.getAllowedValueList();
        if (list.size() > 0) {
            sb.append('\n');
            sb.append("AllowedValue:");
            for (final String v : list) {
                sb.append('\n');
                sb.append('\t');
                sb.append(v);
            }
        }
        return sb.toString();
    }
}
