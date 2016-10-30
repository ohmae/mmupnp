/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import net.mm2d.util.Log;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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
    /**
     * ServiceDescriptionのパース時に使用するビルダー
     *
     * @see Device#loadDescription()
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
         */
        public void serService(@NotNull Service service) {
            mService = service;
        }

        /**
         * Action名を登録する。
         *
         * @param name Action名
         */
        public void setName(@NotNull String name) {
            mName = name;
        }

        /**
         * Argumentのビルダーを登録する。
         *
         * Actionのインスタンス作成後にArgumentを登録することはできない
         *
         * @param argument Argumentのビルダー
         */
        public void addArgumentBuilder(@NotNull Argument.Builder argument) {
            mArgumentList.add(argument);
        }

        /**
         * Argumentのビルダーリストを返す。
         *
         * @return Argumentのビルダーリスト
         */
        @NotNull
        public List<Argument.Builder> getArgumentBuilderList() {
            return mArgumentList;
        }

        /**
         * Actionのインスタンスを作成する。
         *
         * @return Actionのインスタンス
         * @throws IllegalStateException 必須パラメータが設定されていない場合
         */
        @NotNull
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

    private static final String TAG = "Action";
    private final Service mService;
    private final String mName;
    private List<Argument> mArgumentList;
    private final Map<String, Argument> mArgumentMap;
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String SOAP_STYLE = "http://schemas.xmlsoap.org/soap/encoding/";

    private Action(@NotNull Builder builder) {
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
    @NotNull
    public Service getService() {
        return mService;
    }

    /**
     * Action名を返す。
     *
     * @return Action名
     */
    @NotNull
    public String getName() {
        return mName;
    }

    /**
     * Argumentリストを返す。
     *
     * リストは変更不可であり、
     * 変更しようとするとUnsupportedOperationExceptionが発生する。
     *
     * @return Argumentリスト
     */
    @NotNull
    public List<Argument> getArgumentList() {
        if (mArgumentList == null) {
            mArgumentList = Collections.unmodifiableList(new ArrayList<>(mArgumentMap.values()));
        }
        return mArgumentList;
    }

    @NotNull
    private String getSoapActionName() {
        return '"' + mService.getServiceType() + '#' + mName + '"';
    }

    /**
     * Actionを実行する。
     *
     * 実行引数及び実行結果はArgument名をkeyとし、値をvalueとしたMapでやり取りする。
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
    @NotNull
    public Map<String, String> invoke(@NotNull Map<String, String> arguments)
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
        final HttpClient client = new HttpClient(false);
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK) {
            Log.w(TAG, response.toString());
            throw new IOException(response.getStartLine());
        }
        try {
            return parseResponse(response.getBody());
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException(response.getBody());
        }
    }

    @NotNull
    private String makeSoap(@NotNull Map<String, String> arguments) throws IOException {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document doc = db.newDocument();
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
                | ParserConfigurationException
                | TransformerFactoryConfigurationError
                | TransformerException e) {
            throw new IOException();
        }
    }

    @Nullable
    private Element findChildElementByName(@NotNull Node node, @NotNull String name) {
        Node child = node.getFirstChild();
        for (; child != null; child = child.getNextSibling()) {
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (name.equals(child.getLocalName())) {
                return (Element) child;
            }
        }
        return null;
    }

    @NotNull
    private Map<String, String> parseResponse(@NotNull String xml)
            throws IOException, SAXException, ParserConfigurationException {
        final String responseTag = mName + "Response";
        final Map<String, String> result = new HashMap<>();
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        final DocumentBuilder db = dbf.newDocumentBuilder();
        final Document doc = db.parse(new InputSource(new StringReader(xml)));
        final Element envelope = doc.getDocumentElement();
        final Element body = findChildElementByName(envelope, "Body");
        if (body == null) {
            Log.w(TAG, "no body tag");
            throw new IOException("no body tag");
        }
        final Element response = findChildElementByName(body, responseTag);
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
