/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.log.Logger
import net.mm2d.upnp.*
import net.mm2d.upnp.Http.Status
import net.mm2d.upnp.util.XmlUtils
import net.mm2d.upnp.util.findChildElementByLocalName
import net.mm2d.upnp.util.forEachElement
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Implements for [Action].
 *
 * @author [大前良介(OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class ActionImpl(
    override val service: ServiceImpl,
    override val name: String,
    private val argumentMap: Map<String, Argument>
) : Action {
    override val argumentList: List<Argument> by lazy {
        argumentMap.values.toList()
    }

    override fun findArgument(name: String): Argument? {
        return argumentMap[name]
    }

    // VisibleForTesting
    internal fun createHttpClient(): HttpClient {
        return HttpClient(false)
    }

    @Throws(IOException::class)
    override fun invokeSync(
        argumentValues: Map<String, String?>,
        returnErrorResponse: Boolean
    ): Map<String, String> {
        val soap = makeSoap(emptyMap(), makeArguments(argumentValues))
        return invoke(soap, returnErrorResponse)
    }

    @Throws(IOException::class)
    override fun invokeCustomSync(
        argumentValues: Map<String, String?>,
        customNamespace: Map<String, String>,
        customArguments: Map<String, String>,
        returnErrorResponse: Boolean
    ): Map<String, String> {
        val arguments = makeArguments(argumentValues)
        appendArgument(arguments, customArguments)
        val soap = makeSoap(customNamespace, arguments)
        return invoke(soap, returnErrorResponse)
    }

    override fun invoke(
        argumentValues: Map<String, String?>,
        returnErrorResponse: Boolean,
        onResult: ((Map<String, String>) -> Unit)?,
        onError: ((IOException) -> Unit)?
    ) {
        val executors = service.device.controlPoint.taskExecutors
        executors.io {
            try {
                val result = invokeSync(argumentValues, returnErrorResponse)
                onResult?.let { executors.callback { it(result) } }
            } catch (e: IOException) {
                onError?.let { executors.callback { it(e) } }
            }
        }
    }

    override fun invokeCustom(
        argumentValues: Map<String, String?>,
        customNamespace: Map<String, String>,
        customArguments: Map<String, String>,
        returnErrorResponse: Boolean,
        onResult: ((Map<String, String>) -> Unit)?,
        onError: ((IOException) -> Unit)?
    ) {
        val executors = service.device.controlPoint.taskExecutors
        executors.io {
            try {
                val result = invokeCustomSync(argumentValues, customNamespace, customArguments, returnErrorResponse)
                onResult?.let { executors.callback { it(result) } }
            } catch (e: IOException) {
                onError?.let { executors.callback { it(e) } }
            }
        }
    }

    override suspend fun invokeAsync(
        argumentValues: Map<String, String?>,
        returnErrorResponse: Boolean
    ): Map<String, String> {
        return suspendCoroutine { continuation ->
            service.device.controlPoint.taskExecutors.io {
                try {
                    val result = invokeSync(argumentValues, returnErrorResponse)
                    continuation.resume(result)
                } catch (e: IOException) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    override suspend fun invokeCustomAsync(
        argumentValues: Map<String, String?>,
        customNamespace: Map<String, String>,
        customArguments: Map<String, String>,
        returnErrorResponse: Boolean
    ): Map<String, String> {
        return suspendCoroutine { continuation ->
            service.device.controlPoint.taskExecutors.io {
                try {
                    val result = invokeCustomSync(argumentValues, customNamespace, customArguments, returnErrorResponse)
                    continuation.resume(result)
                } catch (e: IOException) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    /**
     * Create an argument list.
     *
     * @param argumentValues Argument values
     * @return Argument list
     */
    private fun makeArguments(argumentValues: Map<String, String?>): MutableList<Pair<String, String?>> {
        return argumentMap.values
            .filter { it.isInputDirection }
            .map { it.name to selectArgumentValue(it, argumentValues) }
            .toMutableList()
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
    private fun selectArgumentValue(argument: Argument, argumentValues: Map<String, String?>): String? {
        return argumentValues[argument.name] ?: argument.relatedStateVariable.defaultValue
    }

    /**
     * Append custom arguments.
     *
     * @param base append target
     * @param arguments custom arguments to append.
     */
    private fun appendArgument(base: MutableList<Pair<String, String?>>, arguments: Map<String, String>) {
        arguments.entries.forEach {
            base.add(it.key to it.value)
        }
    }

    @Throws(IOException::class)
    private fun invoke(soap: String, returnErrorResponse: Boolean): Map<String, String> {
        val result = invoke(soap)
        Logger.v { "action result:\n$result" }
        if (!returnErrorResponse && result.containsKey(Action.ERROR_CODE_KEY)) {
            throw IOException("error response: $result")
        }
        return result
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
        if (response.getStatus() == Status.HTTP_INTERNAL_ERROR && !body.isNullOrEmpty()) {
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
    private fun makeHttpRequest(url: URL, soap: String): HttpRequest {
        return HttpRequest.create().apply {
            setMethod(Http.POST)
            setUrl(url, true)
            setHeader(Http.SOAPACTION, soapActionName)
            setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
            setHeader(Http.CONNECTION, Http.CLOSE)
            setHeader(Http.CONTENT_TYPE, Http.CONTENT_TYPE_DEFAULT)
            setBody(soap, true)
        }
    }

    /**
     * Create a SOAP Action XML string.
     *
     * @param arguments Arguments
     * @return SOAP Action XML string
     * @throws IOException if an I/O error occurs.
     */
    // VisibleForTesting
    @Throws(IOException::class)
    internal fun makeSoap(namespaces: Map<String, String>, arguments: List<Pair<String, String?>>): String {
        try {
            val document = XmlUtils.newDocument(true)
            val action = makeUpToActionElement(document)
            setNamespace(action, namespaces)
            setArgument(document, action, arguments)
            return formatXmlString(document)
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    private fun setNamespace(action: Element, namespace: Map<String, String>) {
        namespace.entries.forEach {
            action.setAttributeNS(XMLNS_URI, XMLNS_PREFIX + it.key, it.value)
        }
    }

    /**
     * Include Action's arguments in XML
     *
     * @param document XML Document
     * @param action Element of action
     * @param arguments Arguments
     */
    private fun setArgument(document: Document, action: Element, arguments: List<Pair<String, String?>>) {
        arguments.forEach { pair ->
            document.createElement(pair.first)?.let { param ->
                pair.second?.let {
                    param.textContent = it
                }
                action.appendChild(param)
            }
        }
    }

    /**
     * Create SOAP Action XML up to ActionElement.
     *
     * @param document XML Document
     * @return ActionElement
     */
    private fun makeUpToActionElement(document: Document): Element {
        val envelope = document.createElementNS(SOAP_NS, "s:Envelope")
        document.appendChild(envelope)
        document.createAttributeNS(SOAP_NS, "s:encodingStyle").also {
            it.nodeValue = SOAP_STYLE
            envelope.setAttributeNode(it)
        }
        val body = document.createElementNS(SOAP_NS, "s:Body").also {
            envelope.appendChild(it)
        }
        return document.createElementNS(service.serviceType, "u:$name").also {
            body.appendChild(it)
        }
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
    internal fun formatXmlString(document: Document): String {
        val sw = StringWriter()
        TransformerFactory.newInstance().newTransformer().also {
            it.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            it.transform(DOMSource(document), StreamResult(sw))
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
        findResponseElement(xml).firstChild?.forEachElement {
            val tag = it.localName
            val text = it.textContent
            if (findArgument(tag) == null) {
                // Optionalな情報としてArgumentに記述されていないタグが含まれる可能性があるためログ出力に留める
                Logger.d { "invalid argument:$tag->$text" }
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
    private fun findResponseElement(xml: String): Element {
        return findElement(xml, responseTagName)
    }

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
        findFaultElement(xml).firstChild?.forEachElement {
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
            ?.firstChild
            ?.forEachElement {
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
    private fun findFaultElement(xml: String): Element {
        return findElement(xml, "Fault")
    }

    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    private fun findElement(xml: String, tag: String): Element {
        return XmlUtils.newDocument(true, xml).documentElement
            .findChildElementByLocalName("Body")
            ?.findChildElementByLocalName(tag) ?: throw IOException("no response tag")
    }

    class Builder {
        private var service: ServiceImpl? = null
        private var name: String? = null
        private val argumentList: MutableList<ArgumentImpl.Builder> = mutableListOf()

        @Throws(IllegalStateException::class)
        fun build(): ActionImpl {
            val service = service
                ?: throw IllegalStateException("service must be set.")
            val name = name
                ?: throw IllegalStateException("name must be set.")
            return ActionImpl(
                service = service,
                name = name,
                argumentMap = argumentList
                    .map { it.build() }
                    .map { it.name to it }
                    .toMap()
            )
        }

        fun getArgumentBuilderList(): List<ArgumentImpl.Builder> {
            return argumentList
        }

        fun setService(service: ServiceImpl): Builder = apply {
            this.service = service
        }

        fun setName(name: String): Builder = apply {
            this.name = name
        }

        // Actionのインスタンス作成後にArgumentを登録することはできない
        fun addArgumentBuilder(argument: ArgumentImpl.Builder): Builder = apply {
            argumentList.add(argument)
        }
    }

    companion object {
        private const val XMLNS_URI = "http://www.w3.org/2000/xmlns/"
        private const val XMLNS_PREFIX = "xmlns:"
        private const val SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/"
        private const val SOAP_STYLE = "http://schemas.xmlsoap.org/soap/encoding/"
    }
}
