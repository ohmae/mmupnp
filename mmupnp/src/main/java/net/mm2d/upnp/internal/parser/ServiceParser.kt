/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.parser

import net.mm2d.upnp.Http
import net.mm2d.upnp.HttpClient
import net.mm2d.upnp.StateVariable
import net.mm2d.upnp.internal.impl.*
import net.mm2d.upnp.util.XmlUtils
import net.mm2d.upnp.util.forEach
import net.mm2d.upnp.util.forEachElement
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.SAXException
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException

/**
 * Serviceのパース処理。
 *
 * Description XMLのダウンロード、パース、Builderへの値の設定をstatic methodで定義。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal object ServiceParser {
    /**
     * SCPDURLからDescriptionを取得し、パースする。
     *
     * 可能であればKeepAliveを行う。
     *
     * @param client        通信に使用するHttpClient
     * @param deviceBuilder DeviceのBuilder
     * @param builder       ServiceのBuilder
     * @throws IOException                  通信エラー
     * @throws SAXException                 XMLパースエラー
     * @throws ParserConfigurationException 実装が使用できないかインスタンス化できない
     */
    @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
    fun loadDescription(client: HttpClient, deviceBuilder: DeviceImpl.Builder, builder: ServiceImpl.Builder) {
        val scpdUrl = builder.getScpdUrl() ?: throw IOException("scpdUrl is null")
        val baseUrl = deviceBuilder.getBaseUrl()
        val scopeId = deviceBuilder.getSsdpMessage().scopeId
        val url = Http.makeAbsoluteUrl(baseUrl, scpdUrl, scopeId)
        val description = client.downloadString(url)
        if (description.isEmpty()) {
            // 空であっても必須パラメータはそろっているため正常として扱う。
            return
        }
        builder.setDescription(description)
        val doc = XmlUtils.newDocument(true, description)
        parseActionList(builder, doc.getElementsByTagName("action"))
        parseStateVariableList(builder, doc.getElementsByTagName("stateVariable"))
    }

    private fun parseActionList(builder: ServiceImpl.Builder, nodeList: NodeList) {
        nodeList.forEach {
            builder.addActionBuilder(parseAction(it as Element))
        }
    }

    private fun parseStateVariableList(builder: ServiceImpl.Builder, nodeList: NodeList) {
        nodeList.forEach {
            builder.addStateVariable(parseStateVariable(it as Element))
        }
    }

    private fun parseAction(element: Element): ActionImpl.Builder {
        val builder = ActionImpl.Builder()
        element.firstChild.forEachElement {
            when (it.localName) {
                "name" -> builder.setName(it.textContent)
                "argumentList" -> {
                    it.firstChild.forEachElement { child ->
                        if (child.localName == "argument") {
                            builder.addArgumentBuilder(parseArgument(child))
                        }
                    }
                }
            }
        }
        return builder
    }

    private fun parseArgument(element: Element): ArgumentImpl.Builder {
        val builder = ArgumentImpl.Builder()
        element.firstChild.forEachElement {
            builder.setField(it.localName, it.textContent)
        }
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

    private fun parseStateVariable(element: Element): StateVariable {
        val builder = StateVariableImpl.Builder()
        builder.setSendEvents(element.getAttribute("sendEvents"))
        builder.setMulticast(element.getAttribute("multicast"))
        element.firstChild.forEachElement {
            when (it.localName) {
                "name" ->
                    builder.setName(it.textContent)
                "dataType" ->
                    builder.setDataType(it.textContent)
                "defaultValue" ->
                    builder.setDefaultValue(it.textContent)
                "allowedValueList" ->
                    parseAllowedValueList(builder, it)
                "allowedValueRange" ->
                    parseAllowedValueRange(builder, it)
            }
        }
        return builder.build()
    }

    private fun parseAllowedValueList(builder: StateVariableImpl.Builder, element: Element) {
        element.firstChild.forEachElement {
            if ("allowedValue" == it.localName) {
                builder.addAllowedValue(it.textContent)
            }
        }
    }

    private fun parseAllowedValueRange(builder: StateVariableImpl.Builder, element: Element) {
        element.firstChild.forEachElement {
            val tag = it.localName
            if (!tag.isNullOrEmpty()) {
                builder.setField(tag, it.textContent)
            }
        }
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
