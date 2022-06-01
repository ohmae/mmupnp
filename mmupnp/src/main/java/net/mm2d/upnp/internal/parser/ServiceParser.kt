/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.parser

import net.mm2d.upnp.Http
import net.mm2d.upnp.SingleHttpClient
import net.mm2d.upnp.StateVariable
import net.mm2d.upnp.internal.impl.ActionImpl
import net.mm2d.upnp.internal.impl.ArgumentImpl
import net.mm2d.upnp.internal.impl.DeviceImpl
import net.mm2d.upnp.internal.impl.ServiceImpl
import net.mm2d.upnp.internal.impl.StateVariableImpl
import net.mm2d.xml.node.XmlElement
import net.mm2d.xml.parser.XmlParser
import java.io.IOException

/**
 * Parser for Service.
 *
 * Download Description XML, parse it, set value to builder.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal object ServiceParser {
    private val deviceTypesThatAllowIoException = setOf(
        // Some DIAL devices return XML, so try the download.
        // But some DIAL devices return 404, so allow it.
        "urn:dial-multiscreen-org:device:dial:1",
        // Basic devices are allowed to have no Service.
        // However, some devices have a Service but return 404, so allow it.
        "urn:schemas-upnp-org:device:Basic:1",
    )

    /**
     * Download Description from SCPDURL and parse it.
     *
     * KeepAlive, if possible.
     *
     * @param client HttpClient
     * @param deviceBuilder DeviceのBuilder
     * @param builder ServiceのBuilder
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    fun loadDescription(client: SingleHttpClient, deviceBuilder: DeviceImpl.Builder, builder: ServiceImpl.Builder) {
        val scpdUrl = builder.getScpdUrl() ?: throw IOException("scpdUrl is null")
        val baseUrl = deviceBuilder.getBaseUrl()
        val scopeId = deviceBuilder.getSsdpMessage().scopeId
        val url = Http.makeAbsoluteUrl(baseUrl, scpdUrl, scopeId)
        val description = try {
            client.downloadString(url)
        } catch (e: IOException) {
            if (!deviceTypesThatAllowIoException.contains(deviceBuilder.getDeviceType())) {
                throw e
            }
            // Allow certain "ill-behaved" devices
            ""
        }
        if (description.isEmpty()) {
            // 空であっても必須パラメータはそろっているため正常として扱う。
            return
        }
        builder.setDescription(description)
        val rootElement = XmlParser.parse(description) ?: return

        rootElement.childElements
            .filter { it.localName == "actionList" }
            .flatMap { it.childElements }
            .filter { it.localName == "action" }
            .forEach { builder.addActionBuilder(parseAction(it)) }
        rootElement.childElements
            .filter { it.localName == "serviceStateTable" }
            .flatMap { it.childElements }
            .filter { it.localName == "stateVariable" }
            .forEach { builder.addStateVariable(parseStateVariable(it)) }
    }

    private fun parseAction(element: XmlElement): ActionImpl.Builder {
        val builder = ActionImpl.Builder()
        element.childElements
            .forEach {
                when (it.localName) {
                    "name" -> builder.setName(it.value)
                    "argumentList" -> parseArgumentList(builder, it)
                }
            }
        return builder
    }

    private fun parseArgumentList(builder: ActionImpl.Builder, element: XmlElement) {
        element.childElements
            .filter { it.localName == "argument" }
            .forEach { builder.addArgumentBuilder(parseArgument(it)) }
    }

    private fun parseArgument(element: XmlElement): ArgumentImpl.Builder {
        val builder = ArgumentImpl.Builder()
        element.childElements
            .forEach { builder.setField(it.localName, it.value) }
        return builder
    }

    private fun ArgumentImpl.Builder.setField(tag: String, value: String) {
        when (tag) {
            "name" ->
                setName(value)
            "direction" ->
                setDirection(value)
            "relatedStateVariable" ->
                setRelatedStateVariableName(value)
        }
    }

    private fun parseStateVariable(element: XmlElement): StateVariable {
        val builder = StateVariableImpl.Builder()
        builder.setSendEvents(element.getAttributeValue("sendEvents"))
        builder.setMulticast(element.getAttributeValue("multicast"))
        element.childElements.forEach {
            when (it.localName) {
                "name" ->
                    builder.setName(it.value)
                "dataType" ->
                    builder.setDataType(it.value)
                "defaultValue" ->
                    builder.setDefaultValue(it.value)
                "allowedValueList" ->
                    parseAllowedValueList(builder, it)
                "allowedValueRange" ->
                    parseAllowedValueRange(builder, it)
            }
        }
        return builder.build()
    }

    private fun parseAllowedValueList(builder: StateVariableImpl.Builder, element: XmlElement) {
        element.childElements
            .filter { it.localName == "allowedValue" }
            .forEach { builder.addAllowedValue(it.value) }
    }

    private fun parseAllowedValueRange(builder: StateVariableImpl.Builder, element: XmlElement) {
        element.childElements
            .forEach { builder.setField(it.localName, it.value) }
    }

    private fun StateVariableImpl.Builder.setField(tag: String, value: String) {
        when (tag) {
            "step" ->
                setStep(value)
            "minimum" ->
                setMinimum(value)
            "maximum" ->
                setMaximum(value)
        }
    }
}
