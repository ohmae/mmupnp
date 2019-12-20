/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.parser

import net.mm2d.upnp.common.Http
import net.mm2d.upnp.common.HttpClient
import net.mm2d.upnp.common.internal.parser.DevicePropertyParser
import net.mm2d.upnp.common.internal.parser.ServicePropertyParser
import net.mm2d.upnp.common.internal.property.DeviceProperty
import net.mm2d.upnp.common.internal.property.ServiceProperty
import net.mm2d.upnp.cp.internal.impl.DeviceImpl
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
        DevicePropertyParser.parseDescription(builder.propertyBuilder, description)
        loadServices(client, builder, builder.propertyBuilder)
    }

    @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
    private fun loadServices(client: HttpClient, builder: DeviceImpl.Builder, propertyBuilder: DeviceProperty.Builder) {
        propertyBuilder.serviceBuilderList.forEach {
            loadDescription(client, builder, it)
        }
        propertyBuilder.deviceBuilderList.forEach {
            loadServices(client, builder, it)
        }
    }

    /**
     * Download Description from SCPDURL and parse it.
     *
     * KeepAlive, if possible.
     *
     * @param client HttpClient
     * @param deviceBuilder DeviceのBuilder
     * @param builder ServiceのBuilder
     * @throws SAXException if an parse error occurs.
     * @throws IOException if an I/O error occurs.
     * @throws ParserConfigurationException If there is a problem with instantiation
     */
    @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
    fun loadDescription(client: HttpClient, deviceBuilder: DeviceImpl.Builder, builder: ServiceProperty.Builder) {
        val scpdUrl = builder.scpdUrl ?: throw IOException("scpdUrl is null")
        // Treat as empty if "/ssdp/notfound". If try to download, "404 Not found" will be returned.
        // This may be Google's DIAL device.
        if (scpdUrl == "/ssdp/notfound") return
        val baseUrl = deviceBuilder.getBaseUrl()
        val scopeId = deviceBuilder.getSsdpMessage().scopeId
        val url = Http.makeAbsoluteUrl(baseUrl, scpdUrl, scopeId)
        val description = client.downloadString(url)
        ServicePropertyParser.parseDescription(builder, description)
    }
}
