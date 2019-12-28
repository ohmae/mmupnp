/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da.internal.parser

import net.mm2d.upnp.common.internal.parser.DevicePropertyParser
import net.mm2d.upnp.common.internal.parser.ServicePropertyParser
import net.mm2d.upnp.common.internal.property.DeviceProperty
import net.mm2d.upnp.da.internal.impl.DaDeviceImpl
import org.xml.sax.SAXException
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException

object DeviceParser {
    @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
    fun parse(deviceDescription: String, serviceDescriptionMap: Map<String, String>): DaDeviceImpl {
        val devicePropertyBuilder = DeviceProperty.Builder()
        DevicePropertyParser.parse(devicePropertyBuilder, deviceDescription)
        parseServices(devicePropertyBuilder, serviceDescriptionMap)
        return DaDeviceImpl.create(devicePropertyBuilder.build())
    }

    @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
    private fun parseServices(
        builder: DeviceProperty.Builder,
        serviceDescriptionMap: Map<String, String>
    ) {
        builder.serviceBuilderList.forEach {
            ServicePropertyParser.parse(it, serviceDescriptionMap[it.serviceId])
        }
        builder.deviceBuilderList.forEach {
            parseServices(it, serviceDescriptionMap)
        }
    }
}
