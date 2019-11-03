/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample

import net.mm2d.upnp.cp.*

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

    fun format(action: Action): String {
        val sb = StringBuilder()
        sb.append("Name: ")
        sb.append(action.name)
        sb.append('\n')
        val args = action.argumentList
        for (arg in args) {
            val v = arg.relatedStateVariable
            sb.append(if (arg.isInputDirection) "in:" else "out:")
            sb.append("(")
            sb.append(v.dataType)
            sb.append(")")
            sb.append(arg.name)
            sb.append('\n')
        }
        return sb.toString()
    }

    fun format(argument: Argument): String {
        val sb = StringBuilder()
        sb.append("Name: ")
        sb.append(argument.name)
        sb.append('\n')
        sb.append("Direction: ")
        sb.append(if (argument.isInputDirection) "in" else "out")
        sb.append('\n')
        sb.append('\n')
        sb.append("RelatedStateVariable:\n")
        sb.append(format(argument.relatedStateVariable))
        return sb.toString()
    }

    fun format(stateVariable: StateVariable): String {
        val sb = StringBuilder()
        sb.append("Name: ")
        sb.append(stateVariable.name)
        sb.append('\n')
        sb.append("SendEvents: ")
        sb.append(if (stateVariable.isSendEvents) "yes" else "no")
        sb.append('\n')
        sb.append("Multicast: ")
        sb.append(if (stateVariable.isMulticast) "yes" else "no")
        sb.append('\n')
        sb.append("DataType: ")
        sb.append(stateVariable.dataType)
        val list = stateVariable.allowedValueList
        if (list.isNotEmpty()) {
            sb.append('\n')
            sb.append("AllowedValue:")
            for (v in list) {
                sb.append('\n')
                sb.append('\t')
                sb.append(v)
            }
        }
        return sb.toString()
    }
}
