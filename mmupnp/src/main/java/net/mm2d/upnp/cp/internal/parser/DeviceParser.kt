/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.parser

import net.mm2d.upnp.common.Http
import net.mm2d.upnp.common.HttpClient
import net.mm2d.upnp.common.util.XmlUtils
import net.mm2d.upnp.common.util.forEachElement
import net.mm2d.upnp.cp.Icon
import net.mm2d.upnp.cp.internal.impl.DeviceImpl
import net.mm2d.upnp.cp.internal.impl.IconImpl
import net.mm2d.upnp.cp.internal.impl.ServiceImpl
import org.w3c.dom.Node
import org.xml.sax.SAXException
import java.io.IOException
import java.util.*
import javax.xml.parsers.ParserConfigurationException

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
     * @throws SAXException if an parse error occurs.
     * @throws IOException if an I/O error occurs.
     * @throws ParserConfigurationException If there is a problem with instantiation
     */
    @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
    fun loadDescription(client: HttpClient, builder: DeviceImpl.Builder) {
        val url = Http.makeUrlWithScopeId(builder.getLocation(), builder.getSsdpMessage().scopeId)
        val description = client.downloadString(url)
        if (description.isEmpty()) {
            throw IOException("download error: $url")
        }
        builder.setDownloadInfo(client)
        parseDescription(builder, description)
        loadServices(client, builder)
    }

    @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
    private fun loadServices(client: HttpClient, builder: DeviceImpl.Builder) {
        builder.getServiceBuilderList().forEach {
            ServiceParser.loadDescription(client, builder, it)
        }
        builder.getEmbeddedDeviceBuilderList().forEach {
            loadServices(client, it)
        }
    }

    @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
    internal fun parseDescription(builder: DeviceImpl.Builder, description: String) {
        builder.setDescription(description)
        val doc = XmlUtils.newDocument(true, description)
        val deviceNode = XmlUtils.findChildElementByLocalName(doc.documentElement, "device") ?: throw IOException()
        parseDevice(builder, deviceNode)
    }

    private fun parseDevice(builder: DeviceImpl.Builder, deviceNode: Node) {
        deviceNode.firstChild?.forEachElement {
            when (val tag = it.localName) {
                "iconList" ->
                    parseIconList(builder, it)
                "serviceList" ->
                    parseServiceList(builder, it)
                "deviceList" ->
                    parseDeviceList(builder, it)
                else -> {
                    val namespace = it.namespaceURI
                    val value = it.textContent
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

    private fun parseIconList(builder: DeviceImpl.Builder, listNode: Node) {
        listNode.firstChild?.forEachElement {
            if (it.localName == "icon") {
                builder.addIcon(parseIcon(it))
            }
        }
    }

    private fun parseIcon(iconNode: Node): Icon {
        val builder = IconImpl.Builder()
        iconNode.firstChild?.forEachElement {
            builder.setField(it.localName, it.textContent)
        }
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

    private fun parseServiceList(builder: DeviceImpl.Builder, listNode: Node) {
        listNode.firstChild?.forEachElement {
            if (it.localName == "service") {
                builder.addServiceBuilder(parseService(it))
            }
        }
    }

    private fun parseService(serviceNode: Node): ServiceImpl.Builder {
        val serviceBuilder = ServiceImpl.Builder()
        serviceNode.firstChild?.forEachElement {
            serviceBuilder.setField(it.localName, it.textContent)
        }
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

    private fun parseDeviceList(builder: DeviceImpl.Builder, listNode: Node) {
        val builderList = ArrayList<DeviceImpl.Builder>()
        listNode.firstChild?.forEachElement {
            if (it.localName == "device") {
                val embeddedBuilder = builder.createEmbeddedDeviceBuilder()
                parseDevice(embeddedBuilder, it)
                builderList.add(embeddedBuilder)
            }
        }
        builder.setEmbeddedDeviceBuilderList(builderList)
    }
}
