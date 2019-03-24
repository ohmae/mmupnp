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
import net.mm2d.upnp.internal.parser.DeviceParser
import net.mm2d.upnp.internal.parser.ServiceParser
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
 * [Action]の実装。
 *
 * @author [大前良介(OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class ActionImpl(
    override val service: ServiceImpl,
    override val name: String,
    private val argumentMap: Map<String, Argument>
) : Action {
    private val _argumentList: List<Argument> by lazy {
        argumentMap.values.toList()
    }

    override val argumentList: List<Argument>
        get() = _argumentList

    override fun findArgument(name: String): Argument? {
        return argumentMap[name]
    }

    // VisibleForTesting
    internal fun createHttpClient(): HttpClient {
        return HttpClient(false)
    }

    @Throws(IOException::class)
    override fun invokeSync(
        argumentValues: Map<String, String>,
        returnErrorResponse: Boolean
    ): Map<String, String> {
        val soap = makeSoap(emptyMap(), makeArguments(argumentValues))
        return invoke(soap, returnErrorResponse)
    }

    @Throws(IOException::class)
    override fun invokeCustomSync(
        argumentValues: Map<String, String>,
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
        argumentValues: Map<String, String>,
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
        argumentValues: Map<String, String>,
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
        argumentValues: Map<String, String>,
        returnErrorResponse: Boolean
    ): Map<String, String> {
        return suspendCoroutine { continuation ->
            invoke(argumentValues, returnErrorResponse,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) })
        }
    }

    override suspend fun invokeCustomAsync(
        argumentValues: Map<String, String>,
        customNamespace: Map<String, String>,
        customArguments: Map<String, String>,
        returnErrorResponse: Boolean
    ): Map<String, String> {
        return suspendCoroutine { continuation ->
            invokeCustom(argumentValues, customNamespace, customArguments, returnErrorResponse,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) })
        }
    }


    /**
     * 入力をもとに引数リストを作成する。
     *
     * @param argumentValues 引数への入力値
     * @return 引数リスト
     */
    private fun makeArguments(argumentValues: Map<String, String>): MutableList<Pair<String, String?>> {
        return argumentMap.values
            .filter { it.isInputDirection }
            .map { it.name to selectArgumentValue(it, argumentValues) }
            .toMutableList()
    }

    /**
     * Argumentの値を選択する。
     *
     * 入力に値があればそれを採用し、なければデフォルト値を採用する。
     * どちらもなければnullが返る。
     *
     * @param argument       Argument
     * @param argumentValues 引数への入力値
     * @return 選択されたArgumentの値
     */
    private fun selectArgumentValue(argument: Argument, argumentValues: Map<String, String>): String? {
        return argumentValues[argument.name] ?: argument.relatedStateVariable.defaultValue
    }

    /**
     * StringPairのリストに変換した引数にカスタム引数を追加する。
     *
     * @param base      引数の追加先
     * @param arguments 追加するカスタム引数
     */
    private fun appendArgument(base: MutableList<Pair<String, String?>>, arguments: Map<String, String>) {
        arguments.entries.forEach {
            base.add(it.key to it.value)
        }
    }

    @Throws(IOException::class)
    private fun invoke(soap: String, returnErrorResponse: Boolean): Map<String, String> {
        Logger.d { "action invoke:\n$soap" }
        val result = invoke(soap)
        Logger.d { "action result:\n$result" }
        if (!returnErrorResponse && result.containsKey(Action.ERROR_CODE_KEY)) {
            throw IOException("error response: $result")
        }
        return result
    }

    /**
     * Actionの実行を行う。
     *
     * @param soap 送信するSOAP XML文字列
     * @return 実行結果
     * @throws IOException 実行時の何らかの通信例外及びエラー応答があった場合
     */
    @Throws(IOException::class)
    private fun invoke(soap: String): Map<String, String> {
        val request = makeHttpRequest(makeAbsoluteControlUrl(), soap)
        val response = createHttpClient().post(request)
        val body = response.body
        if (response.status == Status.HTTP_INTERNAL_ERROR && !body.isNullOrEmpty()) {
            try {
                return parseErrorResponse(body)
            } catch (e: Exception) {
                throw IOException(body, e)
            }
        }
        if (response.status != Http.Status.HTTP_OK || body.isNullOrEmpty()) {
            Logger.w { "action invoke error\n$response" }
            throw IOException(response.startLine)
        }
        try {
            return parseResponse(body)
        } catch (e: Exception) {
            throw IOException(body, e)
        }
    }

    @Throws(MalformedURLException::class)
    fun makeAbsoluteControlUrl(): URL {
        val device = service.device
        return Http.makeAbsoluteUrl(device.baseUrl, service.controlUrl, device.scopeId)
    }

    private val soapActionName: String
        get() = "\"${service.serviceType}#$name\""

    /**
     * SOAP送信のためのHttpRequestを作成する。
     *
     * @param url  接続先URL
     * @param soap SOAPの文字列
     * @return SOAP送信用HttpRequest
     * @throws IOException 通信で問題が発生した場合
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
     * SOAP ActionのXML文字列を作成する。
     *
     * @param arguments 引数
     * @return SOAP ActionのXML文字列
     * @throws IOException 通信で問題が発生した場合
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
     * Actionの引数をXMLへ組み込む
     *
     * @param document  XML Document
     * @param action    actionのElement
     * @param arguments 引数
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
     * SOAP ActionのXMLのActionElementまでを作成する
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
     * XML Documentを文字列に変換する
     *
     * @param document 変換するXML Document
     * @return 変換された文字列
     * @throws TransformerException 変換処理に問題が発生した場合
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
     * Actionに対する応答をパースする
     *
     * @param xml 応答となるXML
     * @return Actionに対する応答、argument名をkeyとする。
     * @throws ParserConfigurationException XMLパーサのインスタンス化に問題がある場合
     * @throws SAXException                 XMLパース処理に問題がある場合
     * @throws IOException                  入力値に問題がある場合
     */
    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    private fun parseResponse(xml: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        findResponseElement(xml).firstChild.forEachElement {
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
     * Actionに対する応答をパースし、ResponseタグのElementを探して返す。
     *
     * @param xml Actionに対する応答であるXML文字列
     * @return ResponseタグのElement
     * @throws ParserConfigurationException XMLパーサのインスタンス化に問題がある場合
     * @throws SAXException                 XMLパース処理に問題がある場合
     * @throws IOException                  入力値に問題がある場合
     */
    @Throws(ParserConfigurationException::class, SAXException::class, IOException::class)
    private fun findResponseElement(xml: String): Element {
        return findElement(xml, responseTagName)
    }

    /**
     * Actionに対するエラー応答をパースする
     *
     * @param xml 応答となるXML
     * @return エラー応答の情報、'faultcode','faultstring','UPnPError/errorCode','UPnPError/errorDescription'
     * @throws ParserConfigurationException XMLパーサのインスタンス化に問題がある場合
     * @throws SAXException                 XMLパース処理に問題がある場合
     * @throws IOException                  入力値に問題がある場合
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
     * エラー応答のdetailタグ以下をパースする。
     *
     * @param detailNode detailノード
     * @return パース結果
     * @throws IOException 入力値に問題がある場合
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
     * Actionに対するエラー応答をパースし、FaultタグのElementを探して返す。
     *
     * @param xml Actionに対する応答であるXML文字列
     * @return FaultタグのElement
     * @throws ParserConfigurationException XMLパーサのインスタンス化に問題がある場合
     * @throws SAXException                 XMLパース処理に問題がある場合
     * @throws IOException                  入力値に問題がある場合
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

    /**
     * ServiceDescriptionのパース時に使用するビルダー。
     *
     * @see DeviceParser.loadDescription
     * @see ServiceParser.loadDescription
     */
    class Builder {
        private var service: ServiceImpl? = null
        private var name: String? = null
        private val argumentList: MutableList<ArgumentImpl.Builder> = mutableListOf()

        /**
         * Actionのインスタンスを作成する。
         *
         * @return Actionのインスタンス
         * @throws IllegalStateException 必須パラメータが設定されていない場合
         */
        @Throws(IllegalStateException::class)
        fun build(): Action {
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

        /**
         * Argumentのビルダーリストを返す。
         *
         * @return Argumentのビルダーリスト
         */
        fun getArgumentBuilderList(): List<ArgumentImpl.Builder> {
            return argumentList
        }

        /**
         * このActionを保持するServiceへの参照を登録。
         *
         * @param service このActionを保持するService
         */
        fun setService(service: ServiceImpl): Builder {
            this.service = service
            return this
        }

        /**
         * Action名を登録する。
         *
         * @param name Action名
         */
        fun setName(name: String): Builder {
            this.name = name
            return this
        }

        /**
         * Argumentのビルダーを登録する。
         *
         * Actionのインスタンス作成後にArgumentを登録することはできない
         *
         * @param argument Argumentのビルダー
         */
        fun addArgumentBuilder(argument: ArgumentImpl.Builder): Builder {
            argumentList.add(argument)
            return this
        }
    }

    companion object {
        private const val XMLNS_URI = "http://www.w3.org/2000/xmlns/"
        private const val XMLNS_PREFIX = "xmlns:"
        private const val SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/"
        private const val SOAP_STYLE = "http://schemas.xmlsoap.org/soap/encoding/"
    }
}
