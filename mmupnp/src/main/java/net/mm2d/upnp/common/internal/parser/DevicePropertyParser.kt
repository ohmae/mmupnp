/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.parser

import net.mm2d.upnp.common.internal.property.DeviceProperty
import net.mm2d.upnp.common.internal.property.IconProperty
import net.mm2d.upnp.common.internal.property.ServiceProperty
import net.mm2d.upnp.common.util.XmlUtils
import net.mm2d.upnp.common.util.asIterable
import net.mm2d.upnp.common.util.childElements
import net.mm2d.upnp.common.util.findChildElementByLocalName
import org.w3c.dom.Node
import org.xml.sax.SAXException
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException

/**
 * Parser for Device.
 *
 * Download Description XML, parse it, set value to builder.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object DevicePropertyParser {
    /**
     * parse DeviceDescription.
     *
     * Parse the Description and register it with the Builder.
     *
     * @param builder Device Builder
     * @throws SAXException if an parse error occurs.
     * @throws IOException if an I/O error occurs.
     * @throws ParserConfigurationException If there is a problem with instantiation
     */
    @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
    fun parseDescription(builder: DeviceProperty.Builder, description: String) {
        builder.description = description
        val doc = XmlUtils.newDocument(true, description)
        val deviceNode = doc.documentElement.findChildElementByLocalName("device") ?: throw IOException()
        builder.parseDevice(deviceNode)
    }

    private fun DeviceProperty.Builder.parseDevice(node: Node) {
        node.childElements().forEach {
            when (val tag = it.localName) {
                "iconList" ->
                    parseIconList(this, it)
                "serviceList" ->
                    parseServiceList(this, it)
                "deviceList" ->
                    parseDeviceList(this, it)
                else -> {
                    val namespace = it.namespaceURI
                    val value = it.textContent
                    putTag(namespace, tag, value)
                    setField(tag, value)
                }
            }
        }
    }

    private fun DeviceProperty.Builder.setField(tag: String, value: String) {
        when (tag) {
            "UDN" ->
                udn = value
            "UPC" ->
                upc = value
            "deviceType" ->
                deviceType = value
            "friendlyName" ->
                friendlyName = value
            "manufacturer" ->
                manufacture = value
            "manufacturerURL" ->
                manufactureUrl = value
            "modelName" ->
                modelName = value
            "modelURL" ->
                modelUrl = value
            "modelDescription" ->
                modelDescription = value
            "modelNumber" ->
                modelNumber = value
            "serialNumber" ->
                serialNumber = value
            "presentationURL" ->
                presentationUrl = value
            "URLBase" ->
                urlBase = value
        }
    }

    private fun parseIconList(builder: DeviceProperty.Builder, listNode: Node) {
        listNode.childElements().forEach {
            if (it.localName == "icon") {
                builder.iconList.add(parseIcon(it))
            }
        }
    }

    private fun parseIcon(iconNode: Node): IconProperty {
        val builder = IconProperty.Builder()
        iconNode.childElements().forEach {
            builder.setField(it.localName, it.textContent)
        }
        return builder.build()
    }

    private fun IconProperty.Builder.setField(tag: String, value: String) {
        when (tag) {
            "mimetype" ->
                mimeType = value
            "height" ->
                height = value.toIntOrNull() ?: 0
            "width" ->
                width = value.toIntOrNull() ?: 0
            "depth" ->
                depth = value.toIntOrNull() ?: 0
            "url" ->
                url = value
        }
    }

    private fun parseServiceList(builder: DeviceProperty.Builder, listNode: Node) {
        listNode.childElements().forEach {
            if (it.localName == "service") {
                builder.serviceBuilderList.add(parseService(it))
            }
        }
    }

    private fun parseService(serviceNode: Node): ServiceProperty.Builder {
        val serviceBuilder = ServiceProperty.Builder()
        serviceNode.childElements().forEach {
            serviceBuilder.setField(it.localName, it.textContent)
        }
        return serviceBuilder
    }

    private fun ServiceProperty.Builder.setField(tag: String, value: String) {
        when (tag) {
            "serviceType" ->
                serviceType = value
            "serviceId" ->
                serviceId = value
            "SCPDURL" ->
                scpdUrl = value
            "eventSubURL" ->
                eventSubUrl = value
            "controlURL" ->
                controlUrl = value
        }
    }

    private fun parseDeviceList(builder: DeviceProperty.Builder, listNode: Node) {
        listNode.childNodes
            .asIterable()
            .filter { it.localName == "device" }
            .map { node ->
                builder.createDeviceBuilder().also {
                    it.parseDevice(node)
                }
            }
            .also { builder.deviceBuilderList.addAll(it) }
    }
}
