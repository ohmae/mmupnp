/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl;

import net.mm2d.log.Logger;
import net.mm2d.upnp.Action;
import net.mm2d.upnp.Argument;
import net.mm2d.upnp.Device;
import net.mm2d.upnp.Http;
import net.mm2d.upnp.Http.Status;
import net.mm2d.upnp.HttpClient;
import net.mm2d.upnp.HttpRequest;
import net.mm2d.upnp.HttpResponse;
import net.mm2d.upnp.Property;
import net.mm2d.upnp.internal.parser.DeviceParser;
import net.mm2d.upnp.internal.parser.ServiceParser;
import net.mm2d.upnp.util.StringPair;
import net.mm2d.upnp.util.TextUtils;
import net.mm2d.upnp.util.XmlUtils;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Actionの実装
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class ActionImpl implements Action {
    /**
     * ServiceDescriptionのパース時に使用するビルダー
     *
     * @see DeviceParser#loadDescription(HttpClient, DeviceImpl.Builder)
     * @see ServiceParser#loadDescription(HttpClient, DeviceImpl.Builder, ServiceImpl.Builder)
     */
    public static class Builder {
        private ServiceImpl mService;
        private String mName;
        @Nonnull
        private final List<ArgumentImpl.Builder> mArgumentList;

        /**
         * インスタンス作成。
         */
        public Builder() {
            mArgumentList = new ArrayList<>();
        }

        /**
         * このActionを保持するServiceへの参照を登録。
         *
         * @param service このActionを保持するService
         * @return Builder
         */
        @Nonnull
        public Builder setService(@Nonnull final ServiceImpl service) {
            mService = service;
            return this;
        }

        /**
         * Action名を登録する。
         *
         * @param name Action名
         * @return Builder
         */
        @Nonnull
        public Builder setName(@Nonnull final String name) {
            mName = name;
            return this;
        }

        /**
         * Argumentのビルダーを登録する。
         *
         * <p>Actionのインスタンス作成後にArgumentを登録することはできない
         *
         * @param argument Argumentのビルダー
         * @return Builder
         */
        @Nonnull
        public Builder addArgumentBuilder(@Nonnull final ArgumentImpl.Builder argument) {
            mArgumentList.add(argument);
            return this;
        }

        /**
         * Argumentのビルダーリストを返す。
         *
         * @return Argumentのビルダーリスト
         */
        @Nonnull
        public List<ArgumentImpl.Builder> getArgumentBuilderList() {
            return mArgumentList;
        }

        /**
         * Actionのインスタンスを作成する。
         *
         * @return Actionのインスタンス
         * @throws IllegalStateException 必須パラメータが設定されていない場合
         */
        @Nonnull
        public Action build() throws IllegalStateException {
            if (mService == null) {
                throw new IllegalStateException("service must be set.");
            }
            if (mName == null) {
                throw new IllegalStateException("name must be set.");
            }
            return new ActionImpl(this);
        }
    }

    private static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";
    private static final String XMLNS_PREFIX = "xmlns:";
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String SOAP_STYLE = "http://schemas.xmlsoap.org/soap/encoding/";
    @Nonnull
    private final ServiceImpl mService;
    @Nonnull
    private final String mName;
    @Nonnull
    private final Map<String, Argument> mArgumentMap;
    @Nullable
    private List<Argument> mArgumentList;

    private ActionImpl(@Nonnull final Builder builder) {
        mService = builder.mService;
        mName = builder.mName;
        mArgumentMap = new LinkedHashMap<>(builder.mArgumentList.size());
        for (final ArgumentImpl.Builder argumentBuilder : builder.mArgumentList) {
            final Argument argument = argumentBuilder.build();
            mArgumentMap.put(argument.getName(), argument);
        }
    }

    @Override
    @Nonnull
    public ServiceImpl getService() {
        return mService;
    }

    @Override
    @Nonnull
    public String getName() {
        return mName;
    }

    @Override
    @Nonnull
    public List<Argument> getArgumentList() {
        if (mArgumentList == null) {
            mArgumentList = Collections.unmodifiableList(new ArrayList<>(mArgumentMap.values()));
        }
        return mArgumentList;
    }

    @Override
    @Nullable
    public Argument findArgument(@Nonnull final String name) {
        return mArgumentMap.get(name);
    }

    @Nonnull
    private String getSoapActionName() {
        return '"' + mService.getServiceType() + '#' + mName + '"';
    }

    @Nonnull
    private String getResponseTagName() {
        return mName + "Response";
    }

    // VisibleForTesting
    @Nonnull
    HttpClient createHttpClient() {
        return new HttpClient(false);
    }

    @Override
    @Nonnull
    public Map<String, String> invoke(@Nonnull final Map<String, String> argumentValues)
            throws IOException {
        return invoke(argumentValues, false);
    }

    @Override
    @Nonnull
    public Map<String, String> invoke(
            @Nonnull final Map<String, String> argumentValues,
            final boolean returnErrorResponse)
            throws IOException {
        final List<StringPair> arguments = makeArguments(argumentValues);
        final String soap = makeSoap(null, arguments);
        return invokeInner(soap, returnErrorResponse);
    }

    @Override
    @Nonnull
    public Map<String, String> invokeCustom(
            @Nonnull final Map<String, String> argumentValues,
            @Nullable final Map<String, String> customNamespace,
            @Nonnull final Map<String, String> customArguments)
            throws IOException {
        return invokeCustom(argumentValues, customNamespace, customArguments, false);
    }

    @Override
    @Nonnull
    public Map<String, String> invokeCustom(
            @Nonnull final Map<String, String> argumentValues,
            @Nullable final Map<String, String> customNamespace,
            @Nonnull final Map<String, String> customArguments,
            final boolean returnErrorResponse)
            throws IOException {
        final List<StringPair> arguments = makeArguments(argumentValues);
        appendArgument(arguments, customArguments);
        final String soap = makeSoap(customNamespace, arguments);
        return invokeInner(soap, returnErrorResponse);
    }

    /**
     * 入力をもとに引数リストを作成する。
     *
     * @param argumentValues 引数への入力値
     * @return 引数リスト
     */
    @Nonnull
    private List<StringPair> makeArguments(@Nonnull final Map<String, String> argumentValues) {
        final List<StringPair> list = new ArrayList<>();
        for (final Entry<String, Argument> entry : mArgumentMap.entrySet()) {
            final Argument argument = entry.getValue();
            if (!argument.isInputDirection()) {
                continue;
            }
            list.add(new StringPair(argument.getName(), selectArgumentValue(argument, argumentValues)));
        }
        return list;
    }

    /**
     * StringPairのリストに変換した引数にカスタム引数を追加する。
     *
     * @param base      引数の追加先
     * @param arguments 追加するカスタム引数
     */
    private void appendArgument(
            @Nonnull final List<StringPair> base,
            @Nonnull final Map<String, String> arguments) {
        for (final Entry<String, String> entry : arguments.entrySet()) {
            base.add(new StringPair(entry.getKey(), entry.getValue()));
        }
    }

    /**
     * Argumentの値を選択する。
     *
     * <p>入力に値があればそれを採用し、なければデフォルト値を採用する。
     * どちらもなければnullが返る。
     *
     * @param argument       Argument
     * @param argumentValues 引数への入力値
     * @return 選択されたArgumentの値
     */
    @Nullable
    private static String selectArgumentValue(
            @Nonnull final Argument argument,
            @Nonnull final Map<String, String> argumentValues) {
        final String value = argumentValues.get(argument.getName());
        if (value != null) {
            return value;
        }
        return argument.getRelatedStateVariable().getDefaultValue();
    }

    @Nonnull
    private Map<String, String> invokeInner(
            @Nonnull final String soap,
            final boolean returnErrorResponse)
            throws IOException {
        Logger.d(() -> "action invoke:\n" + soap);
        final Map<String, String> result = invokeInner(soap);
        Logger.d(() -> "action result:\n" + result);
        if (!returnErrorResponse && result.containsKey(ERROR_CODE_KEY)) {
            throw new IOException("error response: " + result);
        }
        return result;
    }

    /**
     * Actionの実行を行う。
     *
     * @param soap 送信するSOAP XML文字列
     * @return 実行結果
     * @throws IOException 実行時の何らかの通信例外及びエラー応答があった場合
     */
    @Nonnull
    private Map<String, String> invokeInner(@Nonnull final String soap)
            throws IOException {
        final URL url = makeAbsoluteControlUrl();
        final HttpRequest request = makeHttpRequest(url, soap);
        final HttpClient client = createHttpClient();
        final HttpResponse response = client.post(request);
        final String body = response.getBody();
        if (response.getStatus() == Status.HTTP_INTERNAL_ERROR && !TextUtils.isEmpty(body)) {
            try {
                return parseErrorResponse(body);
            } catch (final SAXException | ParserConfigurationException e) {
                throw new IOException(body, e);
            }
        }
        if (response.getStatus() != Http.Status.HTTP_OK || TextUtils.isEmpty(body)) {
            Logger.w(() -> "action invoke error\n" + response);
            throw new IOException(response.getStartLine());
        }
        try {
            return parseResponse(body);
        } catch (final SAXException | ParserConfigurationException e) {
            throw new IOException(body, e);
        }
    }

    // VisibleForTesting
    @Nonnull
    URL makeAbsoluteControlUrl() throws MalformedURLException {
        final Device device = mService.getDevice();
        return Http.makeAbsoluteUrl(device.getBaseUrl(), mService.getControlUrl(), device.getScopeId());
    }

    /**
     * SOAP送信のためのHttpRequestを作成する。
     *
     * @param url  接続先URL
     * @param soap SOAPの文字列
     * @return SOAP送信用HttpRequest
     * @throws IOException 通信で問題が発生した場合
     */
    @Nonnull
    private HttpRequest makeHttpRequest(
            @Nonnull final URL url,
            @Nonnull final String soap)
            throws IOException {
        final HttpRequest request = HttpRequest.create();
        request.setMethod(Http.POST);
        request.setUrl(url, true);
        request.setHeader(Http.SOAPACTION, getSoapActionName());
        request.setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE);
        request.setHeader(Http.CONNECTION, Http.CLOSE);
        request.setHeader(Http.CONTENT_TYPE, Http.CONTENT_TYPE_DEFAULT);
        request.setBody(soap, true);
        return request;
    }

    /**
     * SOAP ActionのXML文字列を作成する。
     *
     * @param arguments 引数
     * @return SOAP ActionのXML文字列
     * @throws IOException 通信で問題が発生した場合
     */
    // VisibleForTesting
    @Nonnull
    String makeSoap(
            @Nullable final Map<String, String> namespaces,
            @Nonnull final List<StringPair> arguments)
            throws IOException {
        try {
            final Document document = XmlUtils.newDocument(true);
            final Element action = makeUpToActionElement(document);
            setNamespace(action, namespaces);
            setArgument(document, action, arguments);
            return formatXmlString(document);
        } catch (final DOMException
                | TransformerFactoryConfigurationError
                | TransformerException
                | ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    private static void setNamespace(
            @Nonnull final Element action,
            @Nullable final Map<String, String> namespace) {
        if (namespace == null) {
            return;
        }
        for (final Entry<String, String> entry : namespace.entrySet()) {
            action.setAttributeNS(XMLNS_URI, XMLNS_PREFIX + entry.getKey(), entry.getValue());
        }
    }

    /**
     * SOAP ActionのXMLのActionElementまでを作成する
     *
     * @param document XML Document
     * @return ActionElement
     */
    @Nonnull
    private Element makeUpToActionElement(@Nonnull final Document document) {
        final Element envelope = document.createElementNS(SOAP_NS, "s:Envelope");
        document.appendChild(envelope);
        final Attr style = document.createAttributeNS(SOAP_NS, "s:encodingStyle");
        style.setNodeValue(SOAP_STYLE);
        envelope.setAttributeNode(style);
        final Element body = document.createElementNS(SOAP_NS, "s:Body");
        envelope.appendChild(body);
        final Element action = document.createElementNS(mService.getServiceType(), "u:" + mName);
        body.appendChild(action);
        return action;
    }

    /**
     * Actionの引数をXMLへ組み込む
     *
     * @param document  XML Document
     * @param action    actionのElement
     * @param arguments 引数
     */
    private static void setArgument(
            @Nonnull final Document document,
            @Nonnull final Element action,
            @Nonnull final List<StringPair> arguments) {
        for (final StringPair pair : arguments) {
            final Element param = document.createElement(pair.getKey());
            final String value = pair.getValue();
            if (value != null) {
                param.setTextContent(value);
            }
            action.appendChild(param);
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
    @Nonnull
    String formatXmlString(@Nonnull final Document document)
            throws TransformerException {
        final TransformerFactory tf = TransformerFactory.newInstance();
        final Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        final StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(sw));
        return sw.toString();
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
    @Nonnull
    private Map<String, String> parseResponse(@Nonnull final String xml)
            throws ParserConfigurationException, IOException, SAXException {
        final Map<String, String> result = new HashMap<>();
        Node node = findResponseElement(xml).getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            final String text = node.getTextContent();
            if (findArgument(tag) == null) {
                // Optionalな情報としてArgumentに記述されていないタグが含まれる可能性があるためログ出力に留める
                Logger.d(() -> "invalid argument:" + tag + "->" + text);
            }
            result.put(tag, text);
        }
        return result;
    }

    /**
     * Actionに対する応答をパースし、ResponseタグのElementを探して返す。
     *
     * @param xml Actionに対する応答であるXML文字列
     * @return ResponseタグのElement
     * @throws ParserConfigurationException XMLパーサのインスタンス化に問題がある場合
     * @throws SAXException                 XMLパース処理に問題がある場合
     * @throws IOException                  入力値に問題がある場合
     */
    @Nonnull
    private Element findResponseElement(@Nonnull final String xml)
            throws ParserConfigurationException, SAXException, IOException {
        return findElement(xml, getResponseTagName());
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
    @Nonnull
    private Map<String, String> parseErrorResponse(@Nonnull final String xml)
            throws ParserConfigurationException, IOException, SAXException {
        final Map<String, String> result = new HashMap<>();
        Node node = findFaultElement(xml).getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if (tag.equals("detail")) {
                result.putAll(parseErrorDetail(node));
                continue;
            }
            result.put(tag, node.getTextContent());
        }
        if (!result.containsKey(ERROR_CODE_KEY)) {
            throw new IOException("no UPnPError/errorCode tag");
        }
        return result;
    }

    /**
     * エラー応答のdetailタグ以下をパースする。
     *
     * @param detailNode detailノード
     * @return パース結果
     * @throws IOException 入力値に問題がある場合
     */
    @Nonnull
    private Map<String, String> parseErrorDetail(@Nonnull final Node detailNode) throws IOException {
        final Map<String, String> result = new HashMap<>();
        final Element error = XmlUtils.findChildElementByLocalName(detailNode, "UPnPError");
        if (error == null) {
            throw new IOException("no UPnPError tag");
        }
        for (Node node = error.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            result.put("UPnPError/" + node.getLocalName(), node.getTextContent());
        }
        return result;
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
    @Nonnull
    private Element findFaultElement(@Nonnull final String xml)
            throws ParserConfigurationException, SAXException, IOException {
        return findElement(xml, "Fault");
    }

    @Nonnull
    private Element findElement(
            @Nonnull final String xml,
            @Nonnull final String tag)
            throws IOException, ParserConfigurationException, SAXException {
        final Document doc = XmlUtils.newDocument(true, xml);
        final Element envelope = doc.getDocumentElement();
        final Element body = XmlUtils.findChildElementByLocalName(envelope, "Body");
        if (body == null) {
            throw new IOException("no body tag");
        }
        final Element element = XmlUtils.findChildElementByLocalName(body, tag);
        if (element == null) {
            throw new IOException("no response tag");
        }
        return element;
    }
}
