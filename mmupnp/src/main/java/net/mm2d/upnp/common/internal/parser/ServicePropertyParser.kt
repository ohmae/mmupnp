/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.parser

import net.mm2d.upnp.common.internal.property.ActionProperty
import net.mm2d.upnp.common.internal.property.ArgumentProperty
import net.mm2d.upnp.common.internal.property.ServiceProperty
import net.mm2d.upnp.common.internal.property.StateVariableProperty
import net.mm2d.upnp.common.util.XmlUtils
import net.mm2d.upnp.common.util.asElementIterable
import net.mm2d.upnp.common.util.childElements
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.SAXException
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException

/**
 * Parser for Service.
 *
 * Download Description XML, parse it, set value to builder.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object ServicePropertyParser {
    /**
     * parse description.
     *
     * @param builder ServiceのBuilder
     * @param description String
     * @throws SAXException if an parse error occurs.
     * @throws IOException if an I/O error occurs.
     * @throws ParserConfigurationException If there is a problem with instantiation
     */
    @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
    fun parse(builder: ServiceProperty.Builder, description: String?) {
        if (description.isNullOrEmpty()) {
            // 空であっても必須パラメータはそろっているため正常として扱う。
            return
        }
        builder.description = description
        val doc = XmlUtils.newDocument(true, description)
        parseActionList(builder, doc.getElementsByTagName("action"))
        parseStateVariableList(builder, doc.getElementsByTagName("stateVariable"))
    }

    private fun parseActionList(builder: ServiceProperty.Builder, nodeList: NodeList) {
        nodeList.asElementIterable().forEach {
            builder.actionBuilderList.add(parseAction(it))
        }
    }

    private fun parseStateVariableList(builder: ServiceProperty.Builder, nodeList: NodeList) {
        nodeList.asElementIterable().forEach {
            builder.stateVariableList.add(parseStateVariable(it))
        }
    }

    private fun parseAction(element: Element): ActionProperty.Builder {
        val builder = ActionProperty.Builder()
        element.childElements().forEach {
            when (it.localName) {
                "name" -> builder.name = it.textContent
                "argumentList" -> it.childElements().forEach { child ->
                    if (child.localName == "argument") {
                        builder.argumentBuilderList.add(parseArgument(child))
                    }
                }
            }
        }
        return builder
    }

    private fun parseArgument(element: Element): ArgumentProperty.Builder {
        val builder = ArgumentProperty.Builder()
        element.childElements().forEach {
            builder.setField(it.localName, it.textContent)
        }
        return builder
    }

    private fun ArgumentProperty.Builder.setField(tag: String, value: String) {
        when (tag) {
            "name" ->
                name = value
            "direction" ->
                isInputDirection = value.equals("in", ignoreCase = true)
            "relatedStateVariable" ->
                relatedStateVariableName = value
        }
    }

    private fun parseStateVariable(element: Element): StateVariableProperty {
        val builder = StateVariableProperty.Builder()
        builder.isSendEvents = !element.getAttribute("sendEvents").equals("no", ignoreCase = true)
        builder.isMulticast = element.getAttribute("multicast").equals("yes", ignoreCase = true)
        element.childElements().forEach {
            when (it.localName) {
                "name" ->
                    builder.name = it.textContent
                "dataType" ->
                    builder.dataType = it.textContent
                "defaultValue" ->
                    builder.defaultValue = it.textContent
                "allowedValueList" ->
                    parseAllowedValueList(builder, it)
                "allowedValueRange" ->
                    parseAllowedValueRange(builder, it)
            }
        }
        return builder.build()
    }

    private fun parseAllowedValueList(builder: StateVariableProperty.Builder, element: Element) {
        element.childElements()
            .filter { "allowedValue" == it.localName }
            .forEach { builder.allowedValueList.add(it.textContent) }
    }

    private fun parseAllowedValueRange(builder: StateVariableProperty.Builder, element: Element) {
        element.childElements().forEach { childElement ->
            childElement.localName?.let {
                builder.setField(it, childElement.textContent)
            }
        }
    }

    private fun StateVariableProperty.Builder.setField(tag: String, value: String) {
        when (tag) {
            "step" ->
                step = value
            "minimum" ->
                minimum = value
            "maximum" ->
                maximum = value
        }
    }
}
