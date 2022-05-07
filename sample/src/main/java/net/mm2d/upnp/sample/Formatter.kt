/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample

import net.mm2d.upnp.Action
import net.mm2d.upnp.Argument
import net.mm2d.upnp.Device
import net.mm2d.upnp.Service
import net.mm2d.upnp.StateVariable

object Formatter {
    fun format(device: Device): String {
        return "Location: ${device.location}\n" +
            "UDN: ${device.udn}\n" +
            "DeviceType: ${device.deviceType}\n" +
            "FriendlyName: ${device.friendlyName}\n" +
            "Manufacture: ${device.manufacture}\n" +
            "ManufactureUrl: ${device.manufactureUrl}\n" +
            "ModelName: ${device.modelName}\n" +
            "ModelUrl: ${device.modelUrl}\n" +
            "ModelDescription: ${device.modelDescription}\n" +
            "ModelNumber: ${device.modelNumber}\n" +
            "SerialNumber: ${device.serialNumber}\n" +
            "PresentationUrl: ${device.presentationUrl}"
    }

    fun format(service: Service): String {
        return "ServiceType: ${service.serviceType}\n" +
            "ServiceId: ${service.serviceId}\n" +
            "ScpdUrl: ${service.scpdUrl}\n" +
            "ControlUrl: ${service.controlUrl}\n" +
            "EventSubUrl: ${service.eventSubUrl}"
    }

    fun format(action: Action): String = buildString {
        append("Name: ")
        append(action.name)
        append('\n')
        val args = action.argumentList
        for (arg in args) {
            val v = arg.relatedStateVariable
            append(if (arg.isInputDirection) "in:" else "out:")
            append("(")
            append(v.dataType)
            append(")")
            append(arg.name)
            append('\n')
        }
    }

    fun format(argument: Argument): String = buildString {
        append("Name: ")
        append(argument.name)
        append('\n')
        append("Direction: ")
        append(if (argument.isInputDirection) "in" else "out")
        append('\n')
        append('\n')
        append("RelatedStateVariable:\n")
        append(format(argument.relatedStateVariable))
    }

    fun format(stateVariable: StateVariable): String = buildString {
        append("Name: ")
        append(stateVariable.name)
        append('\n')
        append("SendEvents: ")
        append(if (stateVariable.isSendEvents) "yes" else "no")
        append('\n')
        append("Multicast: ")
        append(if (stateVariable.isMulticast) "yes" else "no")
        append('\n')
        append("DataType: ")
        append(stateVariable.dataType)
        val list = stateVariable.allowedValueList
        if (list.isNotEmpty()) {
            append('\n')
            append("AllowedValue:")
            for (v in list) {
                append('\n')
                append('\t')
                append(v)
            }
        }
    }
}
