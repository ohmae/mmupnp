/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.Log;
import net.mm2d.util.TextUtils;
import net.mm2d.util.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InterfaceAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Serviceを表すクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class Service {
    private static final String TAG = Service.class.getSimpleName();

    /**
     * DeviceDescriptionのパース時に使用するビルダー
     *
     * @see Device#loadDescription(IconFilter)
     */
    public static class Builder {
        private Device mDevice;
        private String mServiceType;
        private String mServiceId;
        private String mScpdUrl;
        private String mControlUrl;
        private String mEventSubUrl;

        /**
         * インスタンス作成
         */
        public Builder() {
        }

        /**
         * このServiceを保持するDeviceを登録する。
         *
         * @param device このServiceを保持するDevice
         * @return Builder
         */
        @Nonnull
        public Builder setDevice(@Nonnull Device device) {
            mDevice = device;
            return this;
        }

        /**
         * serviceTypeを登録する。
         *
         * @param serviceType serviceType
         * @return Builder
         */
        @Nonnull
        public Builder setServiceType(@Nonnull String serviceType) {
            mServiceType = serviceType;
            return this;
        }

        /**
         * serviceIdを登録する
         *
         * @param serviceId serviceId
         * @return Builder
         */
        @Nonnull
        public Builder setServiceId(@Nonnull String serviceId) {
            mServiceId = serviceId;
            return this;
        }

        /**
         * SCPDURLを登録する
         *
         * @param scpdUrl ScpdURL
         * @return Builder
         */
        @Nonnull
        public Builder setScpdUrl(@Nonnull String scpdUrl) {
            mScpdUrl = scpdUrl;
            return this;
        }

        /**
         * controlURLを登録する。
         *
         * @param controlUrl controlURL
         * @return Builder
         */
        @Nonnull
        public Builder setControlUrl(@Nonnull String controlUrl) {
            mControlUrl = controlUrl;
            return this;
        }

        /**
         * eventSubURLを登録する。
         *
         * @param eventSubUrl eventSubURL
         * @return Builder
         */
        @Nonnull
        public Builder setEventSubUrl(@Nonnull String eventSubUrl) {
            mEventSubUrl = eventSubUrl;
            return this;
        }

        /**
         * Serviceのインスタンスを作成する。
         *
         * @return Serviceのインスタンス
         * @throws IllegalStateException 必須パラメータが設定されていない場合
         */
        @Nonnull
        public Service build() throws IllegalStateException {
            if (mDevice == null) {
                throw new IllegalStateException("device must be set.");
            }
            if (mServiceType == null) {
                throw new IllegalStateException("serviceType must be set.");
            }
            if (mServiceId == null) {
                throw new IllegalStateException("serviceId must be set.");
            }
            if (mScpdUrl == null) {
                throw new IllegalStateException("SCPDURL must be set.");
            }
            if (mControlUrl == null) {
                throw new IllegalStateException("controlURL must be set.");
            }
            if (mEventSubUrl == null) {
                throw new IllegalStateException("eventSubURL must be set.");
            }
            return new Service(this);
        }
    }

    private static final long DEFAULT_SUBSCRIPTION_TIMEOUT = TimeUnit.SECONDS.toMillis(300);
    @Nonnull
    private final ControlPoint mControlPoint;
    @Nonnull
    private final Device mDevice;
    private String mDescription;
    @Nonnull
    private final String mServiceType;
    @Nonnull
    private final String mServiceId;
    @Nonnull
    private final String mScpdUrl;
    @Nonnull
    private final String mControlUrl;
    @Nonnull
    private final String mEventSubUrl;
    private List<Action> mActionList;
    @Nonnull
    private final Map<String, Action> mActionMap;
    private List<StateVariable> mStateVariableList;
    @Nonnull
    private final Map<String, StateVariable> mStateVariableMap;
    private long mSubscriptionStart;
    private long mSubscriptionTimeout;
    private long mSubscriptionExpiryTime;
    @Nullable
    private String mSubscriptionId;
    @Nonnull
    private HttpClientFactory mHttpClientFactory = new HttpClientFactory();

    private Service(@Nonnull Builder builder) {
        mDevice = builder.mDevice;
        mControlPoint = mDevice.getControlPoint();
        mServiceType = builder.mServiceType;
        mServiceId = builder.mServiceId;
        mScpdUrl = builder.mScpdUrl;
        mControlUrl = builder.mControlUrl;
        mEventSubUrl = builder.mEventSubUrl;
        mActionMap = new LinkedHashMap<>();
        mStateVariableMap = new LinkedHashMap<>();
    }

    /**
     * このServiceを保持するDeviceを返す。
     *
     * @return このServiceを保持するDevice
     */
    @Nonnull
    public Device getDevice() {
        return mDevice;
    }

    /**
     * URL関連プロパティの値からURLに変換する。
     *
     * @param url URLプロパティ値
     * @return URLオブジェクト
     * @throws MalformedURLException 不正なURL
     * @see Device#getAbsoluteUrl(String)
     */
    @Nonnull
    URL getAbsoluteUrl(@Nonnull String url) throws MalformedURLException {
        return mDevice.getAbsoluteUrl(url);
    }

    /**
     * serviceTypeを返す。
     *
     * @return serviceType
     */
    @Nonnull
    public String getServiceType() {
        return mServiceType;
    }

    /**
     * serviceIdを返す。
     *
     * @return serviceId
     */
    @Nonnull
    public String getServiceId() {
        return mServiceId;
    }

    /**
     * SCPDURLを返す。
     *
     * @return SCPDURL
     */
    @Nonnull
    public String getScpdUrl() {
        return mScpdUrl;
    }

    /**
     * controlURLを返す。
     *
     * @return controlURL
     */
    @Nonnull
    public String getControlUrl() {
        return mControlUrl;
    }

    /**
     * eventSubURLを返す。
     *
     * @return eventSubURL
     */
    @Nonnull
    public String getEventSubUrl() {
        return mEventSubUrl;
    }

    /**
     * ServiceDescriptionのXMLを返す。
     *
     * @return ServiceDescription
     */
    @Nullable
    public String getDescription() {
        return mDescription;
    }

    /**
     * このサービスが保持する全Actionのリストを返す。
     *
     * <p>リストは変更不可。
     *
     * @return 全Actionのリスト
     */
    @Nonnull
    public List<Action> getActionList() {
        if (mActionList == null) {
            final List<Action> list = new ArrayList<>(mActionMap.values());
            mActionList = Collections.unmodifiableList(list);
        }
        return mActionList;
    }

    /**
     * 名前から該当するActionを探す。
     *
     * <p>見つからない場合はnullが返る。
     *
     * @param name Action名
     * @return 該当するAction、見つからない場合null
     */
    @Nullable
    public Action findAction(@Nonnull String name) {
        return mActionMap.get(name);
    }

    /**
     * 全StateVariableのリストを返す。
     *
     * @return 全StateVariableのリスト
     */
    @Nonnull
    public List<StateVariable> getStateVariableList() {
        if (mStateVariableList == null) {
            final List<StateVariable> list = new ArrayList<>(mStateVariableMap.values());
            mStateVariableList = Collections.unmodifiableList(list);
        }
        return mStateVariableList;
    }

    /**
     * 名前から該当するStateVariableを探す。
     *
     * <p>見つからない場合はnullが返る。
     *
     * @param name StateVariable名
     * @return 該当するStateVariable、見つからない場合null
     */
    @Nullable
    public StateVariable findStateVariable(@Nullable String name) {
        return mStateVariableMap.get(name);
    }

    /**
     * SCPDURLからDescriptionを取得し、パースする。
     *
     * <p>可能であればKeepAliveを行う。
     *
     * @param client 通信に使用するHttpClient
     * @throws IOException                  通信エラー
     * @throws SAXException                 XMLパースエラー
     * @throws ParserConfigurationException 実装が使用できないかインスタンス化できない
     */
    void loadDescription(@Nonnull HttpClient client)
            throws IOException, SAXException, ParserConfigurationException {
        final URL url = getAbsoluteUrl(mScpdUrl);
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.GET);
        request.setUrl(url, true);
        request.setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE);
        request.setHeader(Http.CONNECTION, Http.KEEP_ALIVE);
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK || TextUtils.isEmpty(response.getBody())) {
            Log.i(TAG, "request:" + request.toString() + "\nresponse:" + response.toString());
            throw new IOException(response.getStartLine());
        }
        mDescription = response.getBody();
        parseDescription(mDescription);
    }

    private void parseDescription(@Nonnull String xml)
            throws IOException, SAXException, ParserConfigurationException {
        final Document doc = XmlUtils.newDocument(true, xml);
        final List<Action.Builder> actionList = parseActionList(doc.getElementsByTagName("action"));
        parseStateVariableList(doc.getElementsByTagName("stateVariable"));
        for (final Action.Builder builder : actionList) {
            for (final Argument.Builder b : builder.getArgumentBuilderList()) {
                final String name = b.getRelatedStateVariableName();
                final StateVariable v = mStateVariableMap.get(name);
                b.setRelatedStateVariable(v);
            }
            final Action a = builder.build();
            mActionMap.put(a.getName(), a);
        }
    }

    @Nonnull
    private List<Action.Builder> parseActionList(@Nonnull NodeList nodeList) {
        final List<Action.Builder> list = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            list.add(parseAction((Element) nodeList.item(i)));
        }
        return list;
    }

    private void parseStateVariableList(@Nonnull NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            final StateVariable.Builder builder =
                    parseStateVariable(this, (Element) nodeList.item(i));
            final StateVariable variable = builder.build();
            mStateVariableMap.put(variable.getName(), variable);
        }
    }

    @Nonnull
    private Action.Builder parseAction(@Nonnull Element element) {
        final Action.Builder builder = new Action.Builder();
        builder.setService(this);
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if (TextUtils.equals(tag, "name")) {
                builder.setName(node.getTextContent());
            } else if (TextUtils.equals(tag, "argumentList")) {
                for (Node c = node.getFirstChild(); c != null; c = c.getNextSibling()) {
                    if (c.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    if (TextUtils.equals(c.getLocalName(), "argument")) {
                        builder.addArgumentBuilder(parseArgument((Element) c));
                    }
                }
            }
        }
        return builder;
    }

    @Nonnull
    private Argument.Builder parseArgument(@Nonnull Element element) {
        final Argument.Builder builder = new Argument.Builder();
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if (tag == null) {
                continue;
            }
            final String text = node.getTextContent();
            switch (tag) {
                case "name":
                    builder.setName(text);
                    break;
                case "direction":
                    builder.setDirection(text);
                    break;
                case "relatedStateVariable":
                    builder.setRelatedStateVariableName(text);
                    break;
                default:
                    break;
            }
        }
        return builder;
    }

    @Nonnull
    private static StateVariable.Builder parseStateVariable(
            @Nonnull Service service, @Nonnull Element element) {
        final StateVariable.Builder builder = new StateVariable.Builder();
        builder.setService(service);
        builder.setSendEvents(element.getAttribute("sendEvents"));
        builder.setMulticast(element.getAttribute("multicast"));
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if (tag == null) {
                continue;
            }
            switch (tag) {
                case "name":
                    builder.setName(node.getTextContent());
                    break;
                case "dataType":
                    builder.setDataType(node.getTextContent());
                    break;
                case "defaultValue":
                    builder.setDefaultValue(node.getTextContent());
                    break;
                case "allowedValueList":
                    parseAllowedValueList(builder, (Element) node);
                    break;
                case "allowedValueRange":
                    parseAllowedValueRange(builder, (Element) node);
                default:
                    break;
            }
        }
        return builder;
    }

    private static void parseAllowedValueList(
            @Nonnull StateVariable.Builder builder, @Nonnull Element element) {
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if ("allowedValue".equals(node.getLocalName())) {
                builder.addAllowedValue(node.getTextContent());
            }
        }
    }

    private static void parseAllowedValueRange(
            @Nonnull StateVariable.Builder builder, @Nonnull Element element) {
        Node node = element.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String tag = node.getLocalName();
            if (tag == null) {
                continue;
            }
            final String text = node.getTextContent();
            switch (tag) {
                case "step":
                    builder.setStep(text);
                    break;
                case "minimum":
                    builder.setMinimum(text);
                    break;
                case "maximum":
                    builder.setMaximum(text);
                    break;
                default:
                    break;
            }
        }
    }

    @Nonnull
    private String getCallback() {
        final StringBuilder sb = new StringBuilder();
        sb.append('<');
        sb.append("http://");
        final SsdpMessage ssdp = mDevice.getSsdpMessage();
        final InterfaceAddress ifa = ssdp.getInterfaceAddress();
        sb.append(ifa.getAddress().getHostAddress());
        final int port = mControlPoint.getEventPort();
        if (port != Http.DEFAULT_PORT) {
            sb.append(':');
            sb.append(String.valueOf(port));
        }
        sb.append('/');
        try {
            sb.append(URLEncoder.encode(mDevice.getUdn(), "UTF-8"));
            sb.append('/');
            sb.append(URLEncoder.encode(mServiceId, "UTF-8"));
        } catch (UnsupportedEncodingException ignored) {
        }
        sb.append('>');
        return sb.toString();
    }

    private static long parseTimeout(@Nonnull HttpResponse response) {
        final String timeout = TextUtils.toLowerCase(response.getHeader(Http.TIMEOUT));
        if (TextUtils.isEmpty(timeout)) {
            return DEFAULT_SUBSCRIPTION_TIMEOUT;
        }
        if (timeout.contains("infinite")) { // UPnP2.0でdeprecated扱い、有限な値にする。
            return DEFAULT_SUBSCRIPTION_TIMEOUT;
        }
        final String prefix = "second-";
        final int pos = timeout.indexOf(prefix);
        if (pos < 0) {
            return DEFAULT_SUBSCRIPTION_TIMEOUT;
        }
        final String secondSection = timeout.substring(pos + prefix.length());
        try {
            final int second = Integer.parseInt(secondSection);
            return TimeUnit.SECONDS.toMillis(second);
        } catch (final NumberFormatException e) {
            Log.w(TAG, e);
        }
        return DEFAULT_SUBSCRIPTION_TIMEOUT;
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
     * Subscribeの実行
     *
     * @return 成功時true
     * @throws IOException 通信エラー
     */
    public boolean subscribe() throws IOException {
        return subscribe(false);
    }

    /**
     * Subscribeの実行
     *
     * @param keepRenew trueを指定すると成功後、Expire前に定期的にrenewを行う。
     * @return 成功時true
     * @throws IOException 通信エラー
     */
    public boolean subscribe(boolean keepRenew) throws IOException {
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.SUBSCRIBE);
        final URL url = getAbsoluteUrl(mEventSubUrl);
        request.setUrl(url, true);
        request.setHeader(Http.NT, Http.UPNP_EVENT);
        request.setHeader(Http.CALLBACK, getCallback());
        request.setHeader(Http.TIMEOUT, "Second-300");
        request.setHeader(Http.CONTENT_LENGTH, "0");
        final HttpClient client = createHttpClient();
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK) {
            Log.w(TAG, "subscribe request:" + request.toString() + "\nresponse:" + response.toString());
            return false;
        }
        final String sid = response.getHeader(Http.SID);
        final long timeout = parseTimeout(response);
        if (TextUtils.isEmpty(sid) || timeout == 0) {
            Log.w(TAG, "subscribe response:" + response.toString());
            return false;
        }
        mSubscriptionId = sid;
        mSubscriptionStart = System.currentTimeMillis();
        mSubscriptionTimeout = timeout;
        mSubscriptionExpiryTime = mSubscriptionStart + mSubscriptionTimeout;
        mControlPoint.registerSubscribeService(this, keepRenew);
        return true;
    }

    /**
     * RenewSubscribeを実行する
     *
     * @return 成功時true
     * @throws IOException 通信エラー
     */
    boolean renewSubscribe() throws IOException {
        if (TextUtils.isEmpty(mEventSubUrl) || TextUtils.isEmpty(mSubscriptionId)) {
            return false;
        }
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.SUBSCRIBE);
        final URL url = getAbsoluteUrl(mEventSubUrl);
        request.setUrl(url, true);
        request.setHeader(Http.SID, mSubscriptionId);
        request.setHeader(Http.TIMEOUT, "Second-300");
        request.setHeader(Http.CONTENT_LENGTH, "0");
        final HttpClient client = createHttpClient();
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK) {
            Log.w(TAG, "renewSubscribe request:" + request.toString() + "\nresponse:" + response.toString());
            return false;
        }
        final String sid = response.getHeader(Http.SID);
        final long timeout = parseTimeout(response);
        if (!TextUtils.equals(sid, mSubscriptionId) || timeout == 0) {
            Log.w(TAG, "renewSubscribe response:" + response.toString());
            return false;
        }
        mSubscriptionStart = System.currentTimeMillis();
        mSubscriptionTimeout = timeout;
        mSubscriptionExpiryTime = mSubscriptionStart + mSubscriptionTimeout;
        return true;
    }

    /**
     * Unsubscribeを実行する
     *
     * @return 成功時true
     * @throws IOException 通信エラー
     */
    public boolean unsubscribe() throws IOException {
        if (TextUtils.isEmpty(mEventSubUrl) || TextUtils.isEmpty(mSubscriptionId)) {
            return false;
        }
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.UNSUBSCRIBE);
        final URL url = getAbsoluteUrl(mEventSubUrl);
        request.setUrl(url, true);
        request.setHeader(Http.SID, mSubscriptionId);
        request.setHeader(Http.CONTENT_LENGTH, "0");
        final HttpClient client = new HttpClient(false);
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK) {
            Log.w(TAG, "unsubscribe request:" + request.toString() + "\nresponse:" + response.toString());
            return false;
        }
        mControlPoint.unregisterSubscribeService(this);
        mSubscriptionId = null;
        mSubscriptionStart = 0;
        mSubscriptionTimeout = 0;
        mSubscriptionExpiryTime = 0;
        return true;
    }

    /**
     * Subscribeの期限切れ通知
     */
    void expired() {
        mSubscriptionId = null;
        mSubscriptionStart = 0;
        mSubscriptionTimeout = 0;
        mSubscriptionExpiryTime = 0;
    }

    /**
     * SID(SubscriptionID)を返す。
     *
     * @return SubscriptionID
     */
    @Nullable
    public String getSubscriptionId() {
        return mSubscriptionId;
    }

    /**
     * Subscriptionの開始時刻
     *
     * @return Subscriptionの開始時刻
     */
    public long getSubscriptionStart() {
        return mSubscriptionStart;
    }

    /**
     * Subscriptionの有効期間
     *
     * @return Subscriptionの有効期間
     */
    public long getSubscriptionTimeout() {
        return mSubscriptionTimeout;
    }

    /**
     * Subscriptionの有効期限
     *
     * @return Subscriptionの有効期限
     */
    public long getSubscriptionExpiryTime() {
        return mSubscriptionExpiryTime;
    }

    @Override
    public int hashCode() {
        return mDevice.hashCode() + mServiceId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Service)) {
            return false;
        }
        Service service = (Service) obj;
        return mDevice.equals(service.getDevice()) && mServiceId.equals(service.getServiceId());
    }
}
