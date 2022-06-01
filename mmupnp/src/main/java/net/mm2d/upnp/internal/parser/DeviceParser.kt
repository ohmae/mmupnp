/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.parser

import net.mm2d.upnp.Http
import net.mm2d.upnp.Icon
import net.mm2d.upnp.SingleHttpClient
import net.mm2d.upnp.internal.impl.DeviceImpl
import net.mm2d.upnp.internal.impl.IconImpl
import net.mm2d.upnp.internal.impl.ServiceImpl
import net.mm2d.xml.node.XmlElement
import net.mm2d.xml.parser.XmlParser
import java.io.IOException

/**
 * Parser for Device.
 *
 * Download Description XML, parse it, set value to builder.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal object DeviceParser {
    /**
     * load DeviceDescription.
     *
     * Parse the Description and register it with the Builder.
     * In addition, download Icon / description of Service described internally, parses it,
     * and creates each Builder.
     *
     * @param client HttpClient
     * @param builder DeviceのBuilder
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    fun loadDescription(client: SingleHttpClient, builder: DeviceImpl.Builder) {
        val url = Http.makeUrlWithScopeId(builder.getLocation(), builder.getSsdpMessage().scopeId)
        // DIAL Application-URL
        // val response = client.download(url)
        // Logger.d { response.getHeader("Application-URL") }
        val description = client.downloadString(url)
        if (description.isEmpty()) {
            throw IOException("download error: $url")
        }
        builder.setDownloadInfo(client)
        parseDescription(builder, description)
        loadServices(client, builder)
    }

    @Throws(IOException::class)
    private fun loadServices(client: SingleHttpClient, builder: DeviceImpl.Builder) {
        builder.getServiceBuilderList().forEach {
            ServiceParser.loadDescription(client, builder, it)
        }
        builder.getEmbeddedDeviceBuilderList().forEach {
            loadServices(client, it)
        }
    }

    @Throws(IOException::class)
    internal fun parseDescription(builder: DeviceImpl.Builder, description: String) {
        builder.setDescription(description)
        val rootNode = XmlParser.parse(description) ?: throw IOException()
        val deviceNode = rootNode.childElements
            .find { it.localName == "device" } ?: throw IOException()
        parseDevice(builder, deviceNode)
    }

    private fun parseDevice(builder: DeviceImpl.Builder, deviceNode: XmlElement) {
        deviceNode.childElements.forEach {
            when (val tag = it.localName) {
                "iconList" ->
                    parseIconList(builder, it)
                "serviceList" ->
                    parseServiceList(builder, it)
                "deviceList" ->
                    parseDeviceList(builder, it)
                else -> {
                    val namespace = it.uri
                    val value = it.value
                    builder.putTag(namespace, tag, value)
                    builder.setField(tag, value)
                }
            }
        }
    }

    private fun DeviceImpl.Builder.setField(tag: String, value: String) {
        when (tag) {
            "UDN" ->
                setUdn(value)
            "UPC" ->
                setUpc(value)
            "deviceType" ->
                setDeviceType(value)
            "friendlyName" ->
                setFriendlyName(value)
            "manufacturer" ->
                setManufacture(value)
            "manufacturerURL" ->
                setManufactureUrl(value)
            "modelName" ->
                setModelName(value)
            "modelURL" ->
                setModelUrl(value)
            "modelDescription" ->
                setModelDescription(value)
            "modelNumber" ->
                setModelNumber(value)
            "serialNumber" ->
                setSerialNumber(value)
            "presentationURL" ->
                setPresentationUrl(value)
            "URLBase" ->
                setUrlBase(value)
        }
    }

    private fun parseIconList(builder: DeviceImpl.Builder, listNode: XmlElement) {
        listNode.childElements
            .filter { it.localName == "icon" }
            .forEach { builder.addIcon(parseIcon(it)) }
    }

    private fun parseIcon(iconNode: XmlElement): Icon {
        val builder = IconImpl.Builder()
        iconNode.childElements
            .forEach { builder.setField(it.localName, it.value) }
        return builder.build()
    }

    private fun IconImpl.Builder.setField(tag: String, value: String) {
        when (tag) {
            "mimetype" ->
                setMimeType(value)
            "height" ->
                setHeight(value)
            "width" ->
                setWidth(value)
            "depth" ->
                setDepth(value)
            "url" ->
                setUrl(value)
        }
    }

    private fun parseServiceList(builder: DeviceImpl.Builder, listNode: XmlElement) {
        listNode.childElements
            .filter { it.localName == "service" }
            .forEach { builder.addServiceBuilder(parseService(it)) }
    }

    private fun parseService(serviceNode: XmlElement): ServiceImpl.Builder {
        val serviceBuilder = ServiceImpl.Builder()
        serviceNode.childElements
            .forEach { serviceBuilder.setField(it.localName, it.value) }
        return serviceBuilder
    }

    private fun ServiceImpl.Builder.setField(tag: String, value: String) {
        when (tag) {
            "serviceType" ->
                setServiceType(value)
            "serviceId" ->
                setServiceId(value)
            "SCPDURL" ->
                setScpdUrl(value)
            "eventSubURL" ->
                setEventSubUrl(value)
            "controlURL" ->
                setControlUrl(value)
        }
    }

    private fun parseDeviceList(builder: DeviceImpl.Builder, listNode: XmlElement) {
        val builderList = ArrayList<DeviceImpl.Builder>()
        listNode.childElements
            .filter { it.localName == "device" }
            .forEach {
                val embeddedBuilder = builder.createEmbeddedDeviceBuilder()
                parseDevice(embeddedBuilder, it)
                builderList.add(embeddedBuilder)
            }
        builder.setEmbeddedDeviceBuilderList(builderList)
    }
}
