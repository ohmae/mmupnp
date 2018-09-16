/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.log.Log;
import net.mm2d.util.NetworkUtils;
import net.mm2d.util.TextUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Serviceの実装
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class ServiceImpl implements Service {
    /**
     * DeviceDescriptionのパース時に使用するビルダー
     */
    static class Builder {
        private SubscribeManager mSubscribeManager;
        private Device mDevice;
        private String mServiceType;
        private String mServiceId;
        private String mScpdUrl;
        private String mControlUrl;
        private String mEventSubUrl;
        private String mDescription;
        @Nonnull
        private final List<ActionImpl.Builder> mActionBuilderList = new ArrayList<>();
        @Nonnull
        private final List<StateVariable> mStateVariables = new ArrayList<>();

        /**
         * インスタンス作成
         */
        Builder() {
        }

        /**
         * このServiceを保持するDeviceを登録する。
         *
         * @param device このServiceを保持するDevice
         * @return Builder
         */
        @Nonnull
        Builder setDevice(@Nonnull final Device device) {
            mDevice = device;
            return this;
        }

        /**
         * 購読状態マネージャを設定する。
         *
         * @param manager 購読状態マネージャ
         * @return Builder
         */
        @Nonnull
        Builder setSubscribeManager(@Nonnull final SubscribeManager manager) {
            mSubscribeManager = manager;
            return this;
        }

        /**
         * serviceTypeを登録する。
         *
         * @param serviceType serviceType
         * @return Builder
         */
        @Nonnull
        Builder setServiceType(@Nonnull final String serviceType) {
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
        Builder setServiceId(@Nonnull final String serviceId) {
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
        Builder setScpdUrl(@Nonnull final String scpdUrl) {
            mScpdUrl = scpdUrl;
            return this;
        }

        @Nullable
        String getScpdUrl() {
            return mScpdUrl;
        }

        /**
         * controlURLを登録する。
         *
         * @param controlUrl controlURL
         * @return Builder
         */
        @Nonnull
        Builder setControlUrl(@Nonnull final String controlUrl) {
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
        Builder setEventSubUrl(@Nonnull final String eventSubUrl) {
            mEventSubUrl = eventSubUrl;
            return this;
        }

        /**
         * Description XMLを登録する。
         *
         * @param description Description XML全内容
         * @return Builder
         */
        @Nonnull
        Builder setDescription(@Nonnull final String description) {
            mDescription = description;
            return this;
        }

        /**
         * ActionのBuilderを登録する。
         *
         * @param builder Serviceで定義されているActionのBuilder
         * @return Builder
         */
        @Nonnull
        Builder addActionBuilder(@Nonnull final ActionImpl.Builder builder) {
            mActionBuilderList.add(builder);
            return this;
        }

        /**
         * StateVariableのBuilderを登録する。
         *
         * @param builder Serviceで定義されているStateVariableのBuilder
         * @return Builder
         */
        @SuppressWarnings("UnusedReturnValue")
        @Nonnull
        Builder addStateVariable(@Nonnull final StateVariable builder) {
            mStateVariables.add(builder);
            return this;
        }

        /**
         * Serviceのインスタンスを作成する。
         *
         * @return Serviceのインスタンス
         * @throws IllegalStateException 必須パラメータが設定されていない場合
         */
        @Nonnull
        Service build() throws IllegalStateException {
            if (mDevice == null) {
                throw new IllegalStateException("device must be set.");
            }
            if (mSubscribeManager == null) {
                throw new IllegalStateException("subscribeManager must be set.");
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
            return new ServiceImpl(this);
        }

        public String toDumpString() {
            return "ServiceBuilder:" +
                    "\nserviceType:" + mServiceType +
                    "\nserviceId:" + mServiceId +
                    "\nSCPDURL:" + mScpdUrl +
                    "\neventSubURL:" + mEventSubUrl +
                    "\ncontrolURL:" + mControlUrl +
                    "\nDESCRIPTION:" + mDescription;
        }
    }

    private static final long DEFAULT_SUBSCRIPTION_TIMEOUT = TimeUnit.SECONDS.toMillis(300);
    @Nonnull
    private final SubscribeManager mSubscribeManager;
    @Nonnull
    private final Device mDevice;
    @Nonnull
    private final String mDescription;
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
    @Nullable
    private List<Action> mActionList;
    @Nonnull
    private final Map<String, Action> mActionMap;
    @Nullable
    private List<StateVariable> mStateVariableList;
    @Nonnull
    private final Map<String, StateVariable> mStateVariableMap;
    @Nullable
    private String mSubscriptionId;

    private ServiceImpl(@Nonnull final Builder builder) {
        mSubscribeManager = builder.mSubscribeManager;
        mDevice = builder.mDevice;
        mServiceType = builder.mServiceType;
        mServiceId = builder.mServiceId;
        mScpdUrl = builder.mScpdUrl;
        mControlUrl = builder.mControlUrl;
        mEventSubUrl = builder.mEventSubUrl;
        mDescription = builder.mDescription != null ? builder.mDescription : "";
        mStateVariableMap = buildStateVariableMap(builder.mStateVariables);
        mActionMap = buildActionMap(this, mStateVariableMap, builder.mActionBuilderList);
    }

    @Nonnull
    private static Map<String, StateVariable> buildStateVariableMap(@Nonnull final List<StateVariable> list) {
        if (list.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, StateVariable> map = new LinkedHashMap<>(list.size());
        for (final StateVariable variable : list) {
            map.put(variable.getName(), variable);
        }
        return map;
    }

    @Nonnull
    private static Map<String, Action> buildActionMap(
            @Nonnull final Service service,
            @Nonnull final Map<String, StateVariable> variableMap,
            @Nonnull final List<ActionImpl.Builder> builderList) {
        if (builderList.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, Action> mActionMap = new LinkedHashMap<>(builderList.size());
        for (final ActionImpl.Builder actionBuilder : builderList) {
            for (final ArgumentImpl.Builder argumentBuilder : actionBuilder.getArgumentBuilderList()) {
                final String name = argumentBuilder.getRelatedStateVariableName();
                if (name == null) {
                    throw new IllegalStateException("relatedStateVariable name is null");
                }
                StateVariable variable = variableMap.get(name);
                if (variable == null) {
                    // for AN-WLTU1
                    final String trimmedName = name.trim();
                    variable = variableMap.get(trimmedName);
                    if (variable == null) {
                        throw new IllegalStateException("There is no StateVariable " + name);
                    }
                    Log.w("Invalid description. relatedStateVariable name has unnecessary blanks ["
                            + name + "] on " + service.getServiceId());
                    argumentBuilder.setRelatedStateVariableName(trimmedName);
                }
                argumentBuilder.setRelatedStateVariable(variable);
            }
            final Action action = actionBuilder.setService(service).build();
            mActionMap.put(action.getName(), action);
        }
        return mActionMap;
    }

    @Override
    @Nonnull
    public Device getDevice() {
        return mDevice;
    }

    // VisibleForTesting
    @Nonnull
    URL makeAbsoluteUrl(@Nonnull final String url) throws MalformedURLException {
        return Http.makeAbsoluteUrl(mDevice.getBaseUrl(), url, mDevice.getScopeId());
    }

    @Override
    @Nonnull
    public String getServiceType() {
        return mServiceType;
    }

    @Override
    @Nonnull
    public String getServiceId() {
        return mServiceId;
    }

    @Override
    @Nonnull
    public String getScpdUrl() {
        return mScpdUrl;
    }

    @Override
    @Nonnull
    public String getControlUrl() {
        return mControlUrl;
    }

    @Override
    @Nonnull
    public String getEventSubUrl() {
        return mEventSubUrl;
    }

    @Override
    @Nonnull
    public String getDescription() {
        return mDescription;
    }

    @Override
    @Nonnull
    public List<Action> getActionList() {
        if (mActionList == null) {
            final List<Action> list = new ArrayList<>(mActionMap.values());
            mActionList = Collections.unmodifiableList(list);
        }
        return mActionList;
    }

    @Override
    @Nullable
    public Action findAction(@Nonnull final String name) {
        return mActionMap.get(name);
    }

    @Override
    @Nonnull
    public List<StateVariable> getStateVariableList() {
        if (mStateVariableList == null) {
            final List<StateVariable> list = new ArrayList<>(mStateVariableMap.values());
            mStateVariableList = Collections.unmodifiableList(list);
        }
        return mStateVariableList;
    }

    @Override
    @Nullable
    public StateVariable findStateVariable(@Nullable final String name) {
        return mStateVariableMap.get(name);
    }

    // VisibleForTesting
    @Nonnull
    String getCallback() {
        final StringBuilder sb = new StringBuilder();
        sb.append("<http://");
        final InetAddress address = mDevice.getSsdpMessage().getLocalAddress();
        final int port = mSubscribeManager.getEventPort();
        //noinspection ConstantConditions : 受信したメッセージの場合はnullではない
        sb.append(NetworkUtils.getAddressString(address, port));
        sb.append("/>");
        return sb.toString();
    }

    // VisibleForTesting
    static long parseTimeout(@Nonnull final HttpResponse response) {
        final String timeout = TextUtils.toLowerCase(response.getHeader(Http.TIMEOUT));
        if (TextUtils.isEmpty(timeout) || timeout.contains("infinite")) {
            // infiniteはUPnP2.0でdeprecated扱い、有限な値にする。
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
            Log.w(e);
        }
        return DEFAULT_SUBSCRIPTION_TIMEOUT;
    }

    // VisibleForTesting
    @Nonnull
    HttpClient createHttpClient() {
        return new HttpClient(false);
    }

    @Override
    public boolean subscribe() throws IOException {
        return subscribe(false);
    }

    @Override
    public boolean subscribe(final boolean keepRenew) throws IOException {
        if (!TextUtils.isEmpty(mSubscriptionId)) {
            if (renewSubscribeInner()) {
                mSubscribeManager.setKeepRenew(this, keepRenew);
                return true;
            }
            return false;
        }
        return subscribeInner(keepRenew);
    }

    // VisibleForTesting
    boolean subscribeInner(final boolean keepRenew) throws IOException {
        final HttpClient client = createHttpClient();
        final HttpRequest request = makeSubscribeRequest();
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK) {
            Log.w("subscribe request:" + request.toString() + "\nresponse:" + response.toString());
            return false;
        }
        final String sid = response.getHeader(Http.SID);
        final long timeout = parseTimeout(response);
        if (TextUtils.isEmpty(sid) || timeout <= 0) {
            Log.w("subscribe response:" + response.toString());
            return false;
        }
        mSubscriptionId = sid;
        mSubscribeManager.register(this, timeout, keepRenew);
        return true;
    }

    @Nonnull
    private HttpRequest makeSubscribeRequest() throws IOException {
        return new HttpRequest()
                .setMethod(Http.SUBSCRIBE)
                .setUrl(makeAbsoluteUrl(mEventSubUrl), true)
                .setHeader(Http.NT, Http.UPNP_EVENT)
                .setHeader(Http.CALLBACK, getCallback())
                .setHeader(Http.TIMEOUT, "Second-300")
                .setHeader(Http.CONTENT_LENGTH, "0");
    }

    @Override
    public boolean renewSubscribe() throws IOException {
        if (TextUtils.isEmpty(mSubscriptionId)) {
            return subscribeInner(false);
        }
        return renewSubscribeInner();
    }

    // VisibleForTesting
    boolean renewSubscribeInner() throws IOException {
        final HttpClient client = createHttpClient();
        //noinspection ConstantConditions
        final HttpRequest request = makeRenewSubscribeRequest(mSubscriptionId);
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK) {
            Log.w("renewSubscribe request:" + request.toString() + "\nresponse:" + response.toString());
            return false;
        }
        final String sid = response.getHeader(Http.SID);
        final long timeout = parseTimeout(response);
        if (!TextUtils.equals(sid, mSubscriptionId) || timeout <= 0) {
            Log.w("renewSubscribe response:" + response.toString());
            return false;
        }
        mSubscribeManager.renew(this, timeout);
        return true;
    }

    @Nonnull
    private HttpRequest makeRenewSubscribeRequest(@Nonnull final String subscriptionId) throws IOException {
        return new HttpRequest()
                .setMethod(Http.SUBSCRIBE)
                .setUrl(makeAbsoluteUrl(mEventSubUrl), true)
                .setHeader(Http.SID, subscriptionId)
                .setHeader(Http.TIMEOUT, "Second-300")
                .setHeader(Http.CONTENT_LENGTH, "0");
    }

    @Override
    public boolean unsubscribe() throws IOException {
        if (TextUtils.isEmpty(mSubscriptionId)) {
            return false;
        }
        final HttpClient client = createHttpClient();
        final HttpRequest request = makeUnsubscribeRequest(mSubscriptionId);
        final HttpResponse response = client.post(request);
        mSubscribeManager.unregister(this);
        mSubscriptionId = null;
        if (response.getStatus() != Http.Status.HTTP_OK) {
            Log.w("unsubscribe request:" + request.toString() + "\nresponse:" + response.toString());
            return false;
        }
        return true;
    }

    @Nonnull
    private HttpRequest makeUnsubscribeRequest(@Nonnull final String subscriptionId) throws IOException {
        return new HttpRequest()
                .setMethod(Http.UNSUBSCRIBE)
                .setUrl(makeAbsoluteUrl(mEventSubUrl), true)
                .setHeader(Http.SID, subscriptionId)
                .setHeader(Http.CONTENT_LENGTH, "0");
    }

    @Override
    @Nullable
    public String getSubscriptionId() {
        return mSubscriptionId;
    }

    @Override
    public int hashCode() {
        return mDevice.hashCode() + mServiceId.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Service)) {
            return false;
        }
        final Service service = (Service) obj;
        return mDevice.equals(service.getDevice()) && mServiceId.equals(service.getServiceId());
    }
}
