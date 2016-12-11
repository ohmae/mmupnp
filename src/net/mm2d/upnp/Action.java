/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.Log;
import net.mm2d.util.XmlUtils;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Actionを表現するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class Action {
    private static final String TAG = Action.class.getSimpleName();

    /**
     * ServiceDescriptionのパース時に使用するビルダー
     *
     * @see Device#loadDescription(IconFilter)
     * @see Service#loadDescription(HttpClient)
     */
    public static class Builder {
        private Service mService;
        private String mName;
        private final List<Argument.Builder> mArgumentList;

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
        public Builder setService(@Nonnull Service service) {
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
        public Builder setName(@Nonnull String name) {
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
        public Builder addArgumentBuilder(@Nonnull Argument.Builder argument) {
            mArgumentList.add(argument);
            return this;
        }

        /**
         * Argumentのビルダーリストを返す。
         *
         * @return Argumentのビルダーリスト
         */
        @Nonnull
        public List<Argument.Builder> getArgumentBuilderList() {
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
            return new Action(this);
        }
    }

    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String SOAP_STYLE = "http://schemas.xmlsoap.org/soap/encoding/";
    @Nonnull
    private final Service mService;
    @Nonnull
    private final String mName;
    @Nonnull
    private final Map<String, Argument> mArgumentMap;
    private List<Argument> mArgumentList;
    @Nonnull
    private HttpClientFactory mHttpClientFactory = new HttpClientFactory();

    private Action(@Nonnull Builder builder) {
        mService = builder.mService;
        mName = builder.mName;
        mArgumentMap = new LinkedHashMap<>(builder.mArgumentList.size());
        for (final Argument.Builder b : builder.mArgumentList) {
            b.setAction(this);
            final Argument argument = b.build();
            mArgumentMap.put(argument.getName(), argument);
        }
    }

    /**
     * このActionを保持するServiceを返す。
     *
     * @return このActionを保持するService
     */
    @Nonnull
    public Service getService() {
        return mService;
    }

    /**
     * Action名を返す。
     *
     * @return Action名
     */
    @Nonnull
    public String getName() {
        return mName;
    }

    /**
     * Argumentリストを返す。
     *
     * <p>リストは変更不可であり、
     * 変更しようとするとUnsupportedOperationExceptionが発生する。
     *
     * @return Argumentリスト
     */
    @Nonnull
    public List<Argument> getArgumentList() {
        if (mArgumentList == null) {
            mArgumentList = Collections.unmodifiableList(new ArrayList<>(mArgumentMap.values()));
        }
        return mArgumentList;
    }

    @Nonnull
    private String getSoapActionName() {
        return '"' + mService.getServiceType() + '#' + mName + '"';
    }

    /**
     * HttpClientのファクトリークラスを変更する。
     *
     * @param factory ファクトリークラス
     */
    void setHttpClientFactory(@Nonnull HttpClientFactory factory) {
        mHttpClientFactory = factory;
    }

    @Nonnull
    private HttpClient createHttpClient() {
        return mHttpClientFactory.createHttpClient(false);
    }

    /**
     * Actionを実行する。
     *
     * <p>実行引数及び実行結果はArgument名をkeyとし、値をvalueとしたMapでやり取りする。
     * 値はすべてStringで表現する。
     * Argument(StateVariable)のDataTypeに応じた値チェックは行われない。
     * 引数に不足があった場合、StateVariableにデフォルト値が定義されている場合に限り、その値が反映される。
     * デフォルト値が定義されていない場合は、DataTypeに違反していても空として扱う。
     * 実行後エラー応答があった場合のパースには未対応であり、IOExceptionが発生するのみ。
     *
     * @param arguments 引数
     * @return 実行結果
     * @throws IOException 実行時の何らかの通信例外及びエラー応答があった場合
     */
    @Nonnull
    public Map<String, String> invoke(@Nonnull Map<String, String> arguments)
            throws IOException {
        final String soap = makeSoap(arguments);
        final URL url = mService.getAbsoluteUrl(mService.getControlUrl());
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.POST);
        request.setUrl(url, true);
        request.setHeader(Http.SOAPACTION, getSoapActionName());
        request.setHeader(Http.USER_AGENT, Http.USER_AGENT_VALUE);
        request.setHeader(Http.CONNECTION, Http.CLOSE);
        request.setHeader(Http.CONTENT_TYPE, Http.CONTENT_TYPE_DEFAULT);
        request.setBody(soap, true);
        final HttpClient client = createHttpClient();
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK || response.getBody() == null) {
            Log.w(TAG, response.toString());
            throw new IOException(response.getStartLine());
        }
        try {
            return parseResponse(response.getBody());
        } catch (final SAXException | ParserConfigurationException e) {
            throw new IOException(response.getBody(), e);
        }
    }

    @Nonnull
    private String makeSoap(@Nonnull Map<String, String> arguments) throws IOException {
        try {
            final Document doc = XmlUtils.newDocument();
            final Element envelope = doc.createElementNS(SOAP_NS, "s:Envelope");
            doc.appendChild(envelope);
            final Attr style = doc.createAttributeNS(SOAP_NS, "s:encodingStyle");
            style.setNodeValue(SOAP_STYLE);
            envelope.setAttributeNode(style);
            final Element body = doc.createElementNS(SOAP_NS, "s:Body");
            envelope.appendChild(body);
            final Element action = doc.createElementNS(mService.getServiceType(), "u:" + mName);
            body.appendChild(action);
            for (final Entry<String, Argument> entry : mArgumentMap.entrySet()) {
                final Argument arg = entry.getValue();
                if (arg.isInputDirection()) {
                    final Element param = doc.createElement(arg.getName());
                    String value = arguments.get(arg.getName());
                    if (value == null) {
                        value = arg.getRelatedStateVariable().getDefaultValue();
                    }
                    if (value != null) {
                        param.setTextContent(value);
                    }
                    action.appendChild(param);
                }
            }
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            final StringWriter sw = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (DOMException
                | TransformerFactoryConfigurationError
                | TransformerException
                | ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    @Nonnull
    private Map<String, String> parseResponse(@Nonnull String xml)
            throws IOException, SAXException, ParserConfigurationException {
        final Map<String, String> result = new HashMap<>();
        final String responseTag = mName + "Response";
        final Document doc = XmlUtils.newDocument(xml);
        final Element envelope = doc.getDocumentElement();
        final Element body = XmlUtils.findChildElementByLocalName(envelope, "Body");
        if (body == null) {
            Log.w(TAG, "no body tag");
            throw new IOException("no body tag");
        }
        final Element response = XmlUtils.findChildElementByLocalName(body, responseTag);
        if (response == null) {
            Log.w(TAG, "no response tag");
            throw new IOException("no response tag");
        }
        Node node = response.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            final String text = node.getTextContent();
            final Argument arg = mArgumentMap.get(tag);
            if (arg == null) {
                Log.d(TAG, "invalid argument:" + tag + "->" + text);
                continue;
            }
            result.put(tag, text);
        }
        return result;
    }
}
