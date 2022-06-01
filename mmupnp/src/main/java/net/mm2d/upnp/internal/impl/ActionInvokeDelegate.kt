/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import net.mm2d.upnp.Action
import net.mm2d.upnp.Argument
import net.mm2d.upnp.Http
import net.mm2d.upnp.Property
import net.mm2d.upnp.log.Logger
import net.mm2d.xml.dsl.buildXml
import net.mm2d.xml.node.XmlElement
import net.mm2d.xml.parser.XmlParser
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

internal class ActionInvokeDelegate(
    action: ActionImpl
) {
    private val service: ServiceImpl = action.service
    private val name: String = action.name
    private val argumentMap: Map<String, Argument> = action.argumentMap
    private fun createHttpClient(): HttpClient = HttpClient(CIO)

    suspend fun invoke(
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
    private suspend fun invoke(soap: String, returnErrorResponse: Boolean): Map<String, String> =
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
    private suspend fun invoke(soap: String): Map<String, String> {
        val request = makeHttpRequest(makeAbsoluteControlUrl(), soap)
        Logger.d { "action invoke:\n$request" }
        val response = createHttpClient().request(request)
        val body = response.body<String>()
        Logger.d { "action receive:\n$body" }
        if (response.status == HttpStatusCode.InternalServerError && body.isNotEmpty()) {
            try {
                return parseErrorResponse(body)
            } catch (e: Exception) {
                throw IOException(body, e)
            }
        }
        if (response.status != HttpStatusCode.OK || body.isEmpty()) {
            Logger.w { "action invoke error\n$response" }
            throw IOException(response.toString())
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
    private fun makeHttpRequest(url: URL, soap: String): HttpRequestBuilder =
        HttpRequestBuilder().apply {
            method = HttpMethod.Post
            url(url)
            header(Http.SOAPACTION, soapActionName)
            header(Http.USER_AGENT, Property.USER_AGENT_VALUE)
            header(Http.CONNECTION, Http.CLOSE)
            setBody(TextContent(soap, ContentType.Text.Xml))
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
    internal fun List<Pair<String, String?>>.makeSoap(namespaces: Map<String, String>): String =
        try {
            buildXml {
                ("s:Envelope" ns SOAP_NS)(
                    "s:encodingStyle" ns SOAP_NS eq SOAP_STYLE
                ) {
                    ("s:Body" ns SOAP_NS) {
                        ("u:$name" ns service.serviceType)(
                            *namespaces.map {
                                XMLNS_PREFIX + it.key ns XMLNS_URI eq it.value
                            }.toTypedArray()
                        ) {
                            forEach {
                                it.first { it.second }
                            }
                        }
                    }
                }
            }.buildString()
        } catch (e: Exception) {
            throw IOException(e)
        }


    /**
     * Parses the response of this Action.
     *
     * @param xml XML string that is the response of Action
     * @return the response of this Action. Map with argument name as key and value as value
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    private fun parseResponse(xml: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        findResponseElement(xml).childElements.forEach {
            val tag = it.localName
            val text = it.value
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
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    private fun findResponseElement(xml: String): XmlElement =
        findElement(xml, responseTagName)

    /**
     * Parses the error response of this Action.
     *
     * @param xml XML string that is the response of Action
     * @return error response such as 'faultcode','faultstring','UPnPError/errorCode','UPnPError/errorDescription'
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    private fun parseErrorResponse(xml: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        findFaultElement(xml).childElements.forEach {
            val tag = it.localName
            if (tag == "detail") {
                parseErrorDetail(result, it)
            } else {
                result[tag] = it.value
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
    private fun parseErrorDetail(result: MutableMap<String, String>, detailNode: XmlElement) {
        detailNode.childElements
            .find { it.localName == "UPnPError" }
            ?.let {
                it.childElements.forEach { item ->
                    result["UPnPError/${item.localName}"] = item.value
                }
            } ?: throw IOException("no UPnPError tag")
    }

    /**
     * Parses the error response of this Action, finds and returns the Element of the Fault tag.
     *
     * @param xml XML string that is the response of Action
     * @return Element of the Fault tag
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    private fun findFaultElement(xml: String): XmlElement =
        findElement(xml, "Fault")

    @Throws(IOException::class)
    private fun findElement(xml: String, tag: String): XmlElement {
        val root = XmlParser.parse(xml) ?: throw IOException("no response tag")
        return root.childElements
            .filter { it.localName == "Body" }
            .flatMap { it.childElements }
            .find { it.localName == tag }
            ?: throw IOException("no response tag")
    }

    companion object {
        private const val XMLNS_URI = "http://www.w3.org/2000/xmlns/"
        private const val XMLNS_PREFIX = "xmlns:"
        private const val SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/"
        private const val SOAP_STYLE = "http://schemas.xmlsoap.org/soap/encoding/"
    }
}
