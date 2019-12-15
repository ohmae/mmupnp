/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.impl

import net.mm2d.log.Logger
import net.mm2d.upnp.common.Http
import net.mm2d.upnp.common.HttpClient
import net.mm2d.upnp.common.HttpRequest
import net.mm2d.upnp.common.Property
import net.mm2d.upnp.common.util.*
import net.mm2d.upnp.cp.Action
import net.mm2d.upnp.cp.Argument
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringWriter
import java.net.MalformedURLException
import java.net.URL
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

internal class ActionInvokeDelegate(
    action: ActionImpl
) {
    private val service: ServiceImpl = action.service
    private val name: String = action.name
    private val argumentMap: Map<String, Argument> = action.argumentMap
    private fun createHttpClient(): HttpClient = HttpClient.create(false)

    @Throws(IOException::class)
    fun invoke(
        argumentValues: Map<String, String?>,
        customNamespace: Map<String, String>,
        customArguments: Map<String, String>,
        returnErrorResponse: Boolean
    ): Map<String, String> {
        val arguments = argumentMap.values
            .filter { it.isInputDirection }
            .map { it.name to selectArgumentValue(it, argumentValues) } +
            customArguments.toList()
        return invoke(arguments.makeSoap(customNamespace), returnErrorResponse)
    }

    /**
     * Select the value of Argument.
     *
     * If there is a value in the input, adopt it, otherwise adopt the default value.
     * If there is neither, it will be null.
     *
     * @param argument Argument
     * @param argumentValues Argument values
     * @return Selected argument value
     */
    private fun selectArgumentValue(argument: Argument, argumentValues: Map<String, String?>): String? =
        argumentValues[argument.name] ?: argument.relatedStateVariable.defaultValue

    @Throws(IOException::class)
    private fun invoke(soap: String, returnErrorResponse: Boolean): Map<String, String> =
        invoke(soap).also {
            Logger.v { "action result:\n$it" }
            if (!returnErrorResponse && it.containsKey(Action.ERROR_CODE_KEY)) {
                throw IOException("error response: $it")
            }
        }

    /**
     * invoke this Action
     *
     * @param soap SOAP XML String to send
     * @return result
     * @throws IOException if an I/O error occurs or receive the error response.
     */
    @Throws(IOException::class)
    private fun invoke(soap: String): Map<String, String> {
        val request = makeHttpRequest(makeAbsoluteControlUrl(), soap)
        Logger.d { "action invoke:\n$request" }
        val response = createHttpClient().post(request)
        val body = response.getBody()
        Logger.d { "action receive:\n$body" }
        if (response.getStatus() == Http.Status.HTTP_INTERNAL_ERROR && !body.isNullOrEmpty()) {
            try {
                return parseErrorResponse(body)
            } catch (e: Exception) {
                throw IOException(body, e)
            }
        }
        if (response.getStatus() != Http.Status.HTTP_OK || body.isNullOrEmpty()) {
            Logger.w { "action invoke error\n$response" }
            throw IOException(response.startLine)
        }
        try {
            return parseResponse(body)
        } catch (e: Exception) {
            throw IOException(body, e)
        }
    }

    // VisibleForTesting
    @Throws(MalformedURLException::class)
    internal fun makeAbsoluteControlUrl(): URL {
        val device = service.device
        return Http.makeAbsoluteUrl(device.baseUrl, service.controlUrl, device.scopeId)
    }

    private val soapActionName: String
        get() = "\"${service.serviceType}#$name\""

    /**
     * SOAP送信のためのHttpRequestを作成する。
     *
     * @param url 接続先URL
     * @param soap SOAPの文字列
     * @return SOAP送信用HttpRequest
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    private fun makeHttpRequest(url: URL, soap: String): HttpRequest =
        HttpRequest.create().apply {
            setMethod(Http.POST)
            setUrl(url, true)
            setHeader(Http.SOAPACTION, soapActionName)
            setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
            setHeader(Http.CONNECTION, Http.CLOSE)
            setHeader(Http.CONTENT_TYPE, Http.CONTENT_TYPE_DEFAULT)
            setBody(soap, true)
        }

    /**
     * Create a SOAP Action XML string.
     *
     * @receiver Arguments
     * @param namespaces custom namespaces
     * @return SOAP Action XML string
     * @throws IOException if an I/O error occurs.
     */
    // VisibleForTesting
    @Throws(IOException::class)
    internal fun List<Pair<String, String?>>.makeSoap(namespaces: Map<String, String>): String {
        try {
            val document = XmlUtils.newDocument(true)
            document.makeUpToActionElement().also {
                it.setNamespace(namespaces)
                it.setArgument(this)
            }
            return document.formatXmlString()
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    private fun Element.setNamespace(namespace: Map<String, String>) {
        namespace.forEach {
            setAttributeNS(XMLNS_URI, XMLNS_PREFIX + it.key, it.value)
        }
    }

    /**
     * Include Action's arguments in XML
     *
     * @receiver Element of action
     * @param arguments Arguments
     */
    private fun Element.setArgument(arguments: List<Pair<String, String?>>) {
        arguments.forEach {
            appendNewElement(it.first, it.second)
        }
    }

    /**
     * Create SOAP Action XML up to ActionElement.
     *
     * @receiver document XML Document
     * @return ActionElement
     */
    private fun Document.makeUpToActionElement(): Element =
        appendNewElementNs(SOAP_NS, "s:Envelope").let {
            it.setAttributeNS(SOAP_NS, "s:encodingStyle", SOAP_STYLE)
            it.appendNewElementNs(SOAP_NS, "s:Body")
                .appendNewElementNs(service.serviceType, "u:$name")
        }

    /**
     * Convert XML Document to String.
     *
     * @param document XML Document to convert
     * @return Converted string
     * @throws TransformerException If a conversion error occurs
     */
    // VisibleForTesting
    @Throws(TransformerException::class)
    internal fun Document.formatXmlString(): String {
        val sw = StringWriter()
        TransformerFactory.newInstance().newTransformer().also {
            it.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            it.transform(DOMSource(this), StreamResult(sw))
        }
        return sw.toString()
    }

    /**
     * Parses the response of this Action.
     *
     * @param xml XML string that is the response of Action
     * @return the response of this Action. Map with argument name as key and value as value
     * @throws SAXException if an parse error occurs.
     * @throws IOException if an I/O error occurs.
     * @throws ParserConfigurationException If there is a problem with instantiation
     */
    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    private fun parseResponse(xml: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        findResponseElement(xml).childElements().forEach {
            val tag = it.localName
            val text = it.textContent
            if (argumentMap[tag] == null) {
                // Optionalな情報としてArgumentに記述されていないタグが含まれる可能性があるためログ出力に留める
                Logger.i { "invalid argument:$tag->$text" }
            }
            result[tag] = text
        }
        return result
    }

    private val responseTagName: String
        get() = "${name}Response"

    /**
     * Parses the error response of this Action, finds and returns the Element of the Response tag.
     *
     * @param xml XML string that is the response of Action
     * @return Element of the Response tag
     * @throws SAXException if an parse error occurs.
     * @throws IOException if an I/O error occurs.
     * @throws ParserConfigurationException If there is a problem with instantiation
     */
    @Throws(ParserConfigurationException::class, SAXException::class, IOException::class)
    private fun findResponseElement(xml: String): Element = findElement(xml, responseTagName)

    /**
     * Parses the error response of this Action.
     *
     * @param xml XML string that is the response of Action
     * @return error response such as 'faultcode','faultstring','UPnPError/errorCode','UPnPError/errorDescription'
     * @throws SAXException if an parse error occurs.
     * @throws IOException if an I/O error occurs.
     * @throws ParserConfigurationException If there is a problem with instantiation
     */
    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    private fun parseErrorResponse(xml: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        findFaultElement(xml).childElements().forEach {
            val tag = it.localName
            if (tag == "detail") {
                parseErrorDetail(result, it)
            } else {
                result[tag] = it.textContent
            }
        }
        if (!result.containsKey(Action.ERROR_CODE_KEY)) {
            throw IOException("no UPnPError/errorCode tag")
        }
        return result
    }

    /**
     * Parse the descendants of the detail tag of the error response.
     *
     * @param result Output destination of parse result
     * @param detailNode detail node
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    private fun parseErrorDetail(result: MutableMap<String, String>, detailNode: Node) {
        detailNode.findChildElementByLocalName("UPnPError")
            ?.childElements()
            ?.forEach {
                result["UPnPError/${it.localName}"] = it.textContent
            } ?: throw IOException("no UPnPError tag")
    }

    /**
     * Parses the error response of this Action, finds and returns the Element of the Fault tag.
     *
     * @param xml XML string that is the response of Action
     * @return Element of the Fault tag
     * @throws SAXException if an parse error occurs.
     * @throws IOException if an I/O error occurs.
     * @throws ParserConfigurationException If there is a problem with instantiation
     */
    @Throws(ParserConfigurationException::class, SAXException::class, IOException::class)
    private fun findFaultElement(xml: String): Element = findElement(xml, "Fault")

    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    private fun findElement(xml: String, tag: String): Element =
        XmlUtils.newDocument(true, xml).documentElement
            .findChildElementByLocalName("Body")
            ?.findChildElementByLocalName(tag) ?: throw IOException("no response tag")

    companion object {
        private const val XMLNS_URI = "http://www.w3.org/2000/xmlns/"
        private const val XMLNS_PREFIX = "xmlns:"
        private const val SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/"
        private const val SOAP_STYLE = "http://schemas.xmlsoap.org/soap/encoding/"
    }
}
