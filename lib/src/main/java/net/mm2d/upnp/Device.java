/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * UPnP Deviceを表現するクラス
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class Device {
    /**
     * DeviceのBuilder。
     *
     * XMLファイルの読み込み処理もBuilderに対して行う。
     */
    public static class Builder {
        @Nonnull
        private final ControlPoint mControlPoint;
        @Nonnull
        private SsdpMessage mSsdpMessage;
        @Nonnull
        private String mLocation;
        private String mDescription;
        private String mUdn;
        private String mUpc;
        private String mDeviceType;
        private String mFriendlyName;
        private String mManufacture;
        private String mManufactureUrl;
        private String mModelName;
        private String mModelUrl;
        private String mModelDescription;
        private String mModelNumber;
        private String mSerialNumber;
        private String mPresentationUrl;
        private String mUrlBase;
        @Nonnull
        private final List<Icon.Builder> mIconBuilderList = new ArrayList<>();
        @Nonnull
        private final List<Service.Builder> mServiceBuilderList = new ArrayList<>();
        @Nonnull
        private volatile List<Device.Builder> mDeviceBuilderList = Collections.emptyList();
        @Nonnull
        private final Map<String, Map<String, String>> mTagMap;

        /**
         * インスタンスを作成する。
         *
         * @param controlPoint ControlPoint
         * @param ssdpMessage  SSDPパケット
         */
        public Builder(
                @Nonnull final ControlPoint controlPoint,
                @Nonnull final SsdpMessage ssdpMessage) {
            mControlPoint = controlPoint;
            final String location = ssdpMessage.getLocation();
            if (location == null) {
                throw new IllegalArgumentException();
            }
            mLocation = location;
            mSsdpMessage = ssdpMessage;
            mTagMap = new LinkedHashMap<>();
            mTagMap.put("", new HashMap<String, String>());
        }

        /**
         * EmbeddedDevice用のBuilderを作成する。
         *
         * @return EmbeddedDevice用のBuilder
         */
        public Builder createEmbeddedDeviceBuilder() {
            final Builder builder = new Builder(mControlPoint, mSsdpMessage);
            builder.setDescription(mDescription);
            builder.setUrlBase(mUrlBase);
            return builder;
        }

        /**
         * SSDPに記述されたLocationの値を返す。
         *
         * @return SSDPに記述されたLocationの値
         */
        @Nonnull
        public String getLocation() {
            return mLocation;
        }

        /**
         * SSDPに記述されたUUIDを返す。
         *
         * @return SSDPに記述されたUUID
         */
        @Nonnull
        public String getUuid() {
            return mSsdpMessage.getUuid();
        }

        /**
         * 最新のSSDPパケットを返す。
         *
         * @return 最新のSSDPパケット
         */
        @Nonnull
        SsdpMessage getSsdpMessage() {
            return mSsdpMessage;
        }

        /**
         * SSDPパケットを登録する。
         *
         * @param ssdpMessage SSDPパケット
         */
        public void updateSsdpMessage(@Nonnull final SsdpMessage ssdpMessage) {
            final String location = ssdpMessage.getLocation();
            if (location == null) {
                throw new IllegalArgumentException();
            }
            mLocation = location;
            mSsdpMessage = ssdpMessage;
            for (final Builder builder : mDeviceBuilderList) {
                builder.updateSsdpMessage(ssdpMessage);
            }
        }

        /**
         * パース前のDescriptionXMLを登録する。
         *
         * @param description DescriptionXML
         * @return Builder
         */
        @Nonnull
        public Builder setDescription(@Nonnull final String description) {
            mDescription = description;
            return this;
        }

        /**
         * UDNの値を登録する。
         *
         * @param udn UDN
         * @return Builder
         */
        @Nonnull
        public Builder setUdn(@Nonnull final String udn) {
            mUdn = udn;
            return this;
        }

        /**
         * UPCの値を登録する。
         *
         * @param upc UPC
         * @return Builder
         */
        @Nonnull
        public Builder setUpc(@Nonnull final String upc) {
            mUpc = upc;
            return this;
        }

        /**
         * DeviceTypeの値を登録する。
         *
         * @param deviceType DeviceType
         * @return Builder
         */
        @Nonnull
        public Builder setDeviceType(@Nonnull final String deviceType) {
            mDeviceType = deviceType;
            return this;
        }

        /**
         * FriendlyNameの値を登録する。
         *
         * @param friendlyName FriendlyName
         * @return Builder
         */
        @Nonnull
        public Builder setFriendlyName(@Nonnull final String friendlyName) {
            mFriendlyName = friendlyName;
            return this;
        }

        /**
         * Manufactureの値を登録する。
         *
         * @param manufacture Manufacture
         * @return Builder
         */
        @Nonnull
        public Builder setManufacture(@Nonnull final String manufacture) {
            mManufacture = manufacture;
            return this;
        }

        /**
         * ManufactureUrlの値を登録する。
         *
         * @param manufactureUrl ManufactureUrl
         * @return Builder
         */
        @Nonnull
        public Builder setManufactureUrl(@Nonnull final String manufactureUrl) {
            mManufactureUrl = manufactureUrl;
            return this;
        }

        /**
         * ModelNameの値を登録する。
         *
         * @param modelName ModelName
         * @return Builder
         */
        @Nonnull
        public Builder setModelName(@Nonnull final String modelName) {
            mModelName = modelName;
            return this;
        }

        /**
         * ModelUrlの値を登録する。
         *
         * @param modelUrl ModelUrl
         * @return Builder
         */
        @Nonnull
        public Builder setModelUrl(@Nonnull final String modelUrl) {
            mModelUrl = modelUrl;
            return this;
        }

        /**
         * ModelDescriptionの値を登録する。
         *
         * @param modelDescription ModelDescription
         * @return Builder
         */
        @Nonnull
        public Builder setModelDescription(@Nonnull final String modelDescription) {
            mModelDescription = modelDescription;
            return this;
        }

        /**
         * ModelNumberの値を登録する。
         *
         * @param modelNumber ModelNumber
         * @return Builder
         */
        @Nonnull
        public Builder setModelNumber(@Nonnull final String modelNumber) {
            mModelNumber = modelNumber;
            return this;
        }

        /**
         * SerialNumberの値を登録する。
         *
         * @param serialNumber SerialNumber
         * @return Builder
         */
        @Nonnull
        public Builder setSerialNumber(@Nonnull final String serialNumber) {
            mSerialNumber = serialNumber;
            return this;
        }

        /**
         * PresentationUrlの値を登録する。
         *
         * @param presentationUrl PresentationUrl
         * @return Builder
         */
        @Nonnull
        public Builder setPresentationUrl(@Nonnull final String presentationUrl) {
            mPresentationUrl = presentationUrl;
            return this;
        }

        /**
         * URLBaseの値を登録する。
         *
         * URLBaseは1.1以降Deprecated
         *
         * @param urlBase URLBaseの値
         * @return Builder
         */
        @Nonnull
        public Builder setUrlBase(final String urlBase) {
            mUrlBase = urlBase;
            return this;
        }

        /**
         * URLのベースとして使用する値を返す。
         *
         * URLBaseの値が存在する場合はURLBase、存在しない場合はLocationの値を利用する。
         *
         * @return URLのベースとして使用する値
         */
        @Nonnull
        public String getBaseUrl() {
            if (mUrlBase != null) {
                return mUrlBase;
            }
            return mLocation;
        }

        /**
         * IconのBuilderを登録する。
         *
         * @param builder IconのBuilder
         * @return Builder
         */
        @Nonnull
        public Builder addIconBuilder(@Nonnull final Icon.Builder builder) {
            mIconBuilderList.add(builder);
            return this;
        }

        /**
         * ServiceのBuilderを登録する。
         *
         * @param builder ServiceのBuilder
         * @return Builder
         */
        @Nonnull
        public Builder addServiceBuilder(@Nonnull final Service.Builder builder) {
            mServiceBuilderList.add(builder);
            return this;
        }

        /**
         * Embedded DeviceのBuilderを登録する。
         *
         * @param builderList Embedded DeviceのBuilderリスト
         * @return Builder
         */
        @Nonnull
        public Builder setEmbeddedDeviceBuilderList(@Nonnull final List<Builder> builderList) {
            mDeviceBuilderList = builderList;
            return this;
        }

        /**
         * ServiceのBuilderのリストを返す。
         *
         * @return ServiceのBuilderのリスト
         */
        @Nonnull
        public List<Service.Builder> getServiceBuilderList() {
            return mServiceBuilderList;
        }

        /**
         * Embedded DeviceのBuilderのリストを返す。
         *
         * @return Embedded DeviceのBuilderのリスト
         */
        @Nonnull
        public List<Device.Builder> getEmbeddedDeviceBuilderList() {
            return mDeviceBuilderList;
        }

        /**
         * XMLタグの情報を登録する。
         *
         * <p>DeviceDescriptionにはAttributeは使用されていないため
         * Attributeには非対応
         *
         * @param namespace namespace uri
         * @param tag       タグ名
         * @param value     タグの値
         * @return Builder
         */
        @Nonnull
        public Builder putTag(
                @Nullable final String namespace,
                @Nonnull final String tag,
                @Nonnull final String value) {
            final String namespaceUri = namespace == null ? "" : namespace;
            Map<String, String> map = mTagMap.get(namespaceUri);
            if (map == null) {
                map = new HashMap<>();
                mTagMap.put(namespaceUri, map);
            }
            map.put(tag, value);
            return this;
        }

        /**
         * Deviceのインスタンスを作成する。
         *
         * @return Deviceのインスタンス
         */
        @Nonnull
        public Device build() {
            return build(null);
        }

        /**
         * Deviceのインスタンスを作成する。
         *
         * @param parent 親Device、EmbeddedDeviceの場合に指定
         * @return Deviceのインスタンス
         */
        @Nonnull
        Device build(@Nullable final Device parent) {
            if (mDescription == null) {
                throw new IllegalStateException("description must be set.");
            }
            if (mDeviceType == null) {
                throw new IllegalStateException("deviceType must be set.");
            }
            if (mFriendlyName == null) {
                throw new IllegalStateException("friendlyName must be set.");
            }
            if (mManufacture == null) {
                throw new IllegalStateException("manufacturer must be set.");
            }
            if (mModelName == null) {
                throw new IllegalStateException("modelName must be set.");
            }
            if (mUdn == null) {
                throw new IllegalStateException("UDN must be set.");
            }
            // Sometime Embedded devices have different UUIDs. So, do not check when embedded device
            if (parent == null && !mUdn.equals(getUuid())) {
                throw new IllegalStateException("uuid and udn does not match! uuid=" + getUuid() + " udn=" + mUdn);
            }
            return new Device(parent, this);
        }
    }

    @Nullable
    private final Device mParent;
    @Nonnull
    private final ControlPoint mControlPoint;
    @Nonnull
    private SsdpMessage mSsdpMessage;
    @Nonnull
    private String mLocation;
    @Nonnull
    private final String mDescription;
    @Nonnull
    private final String mUdn;
    @Nullable
    private final String mUpc;
    @Nonnull
    private final String mDeviceType;
    @Nonnull
    private final String mFriendlyName;
    @Nullable
    private final String mManufacture;
    @Nullable
    private final String mManufactureUrl;
    @Nonnull
    private final String mModelName;
    @Nullable
    private final String mModelUrl;
    @Nullable
    private final String mModelDescription;
    @Nullable
    private final String mModelNumber;
    @Nullable
    private final String mSerialNumber;
    @Nullable
    private final String mPresentationUrl;
    @Nullable
    private final String mUrlBase;
    @Nonnull
    private final Map<String, Map<String, String>> mTagMap;
    @Nonnull
    private final List<Icon> mIconList;
    @Nonnull
    private final List<Service> mServiceList;
    @Nonnull
    private final List<Device> mDeviceList;

    /**
     * ControlPointに紐付いたインスタンスを作成。
     *
     * @param parent  親Device、EmbeddedDeviceの場合に指定
     * @param builder ビルダー
     */
    private Device(
            @Nullable final Device parent,
            @Nonnull final Builder builder) {
        mParent = parent;
        mControlPoint = builder.mControlPoint;
        mSsdpMessage = builder.mSsdpMessage;
        mLocation = builder.mLocation;
        mUdn = builder.mUdn;
        mUpc = builder.mUpc;
        mDeviceType = builder.mDeviceType;
        mFriendlyName = builder.mFriendlyName;
        mManufacture = builder.mManufacture;
        mManufactureUrl = builder.mManufactureUrl;
        mModelName = builder.mModelName;
        mModelUrl = builder.mModelUrl;
        mModelDescription = builder.mModelDescription;
        mModelNumber = builder.mModelNumber;
        mSerialNumber = builder.mSerialNumber;
        mPresentationUrl = builder.mPresentationUrl;
        mUrlBase = builder.mUrlBase;
        mDescription = builder.mDescription;
        mTagMap = builder.mTagMap;
        mIconList = buildIconList(this, builder.mIconBuilderList);
        mServiceList = buildServiceList(this, builder.mServiceBuilderList);
        mDeviceList = buildDeviceList(this, builder.mDeviceBuilderList);
    }

    @Nonnull
    private static List<Icon> buildIconList(
            @Nonnull final Device device,
            @Nonnull final List<Icon.Builder> builderList) {
        if (builderList.isEmpty()) {
            return Collections.emptyList();
        }
        final List<Icon> list = new ArrayList<>(builderList.size());
        for (final Icon.Builder builder : builderList) {
            list.add(builder.setDevice(device).build());
        }
        return list;
    }

    @Nonnull
    private static List<Service> buildServiceList(
            @Nonnull final Device device,
            @Nonnull final List<Service.Builder> builderList) {
        if (builderList.isEmpty()) {
            return Collections.emptyList();
        }
        final List<Service> list = new ArrayList<>(builderList.size());
        for (final Service.Builder builder : builderList) {
            list.add(builder.setDevice(device).build());
        }
        return list;
    }

    @Nonnull
    private static List<Device> buildDeviceList(
            @Nonnull final Device parent,
            @Nonnull final List<Device.Builder> builderList) {
        if (builderList.isEmpty()) {
            return Collections.emptyList();
        }
        final List<Device> list = new ArrayList<>(builderList.size());
        for (final Device.Builder builder : builderList) {
            list.add(builder.build(parent));
        }
        return list;
    }

    /**
     * Iconのバイナリーデータを読み込む
     *
     * @param client 通信に使用するHttpClient
     * @param filter 読み込むIconを選別するFilter
     */
    void loadIconBinary(
            @Nonnull final HttpClient client,
            @Nonnull final IconFilter filter) {
        if (mIconList.isEmpty()) {
            return;
        }
        final List<Icon> loadList = filter.filter(mIconList);
        for (final Icon icon : loadList) {
            try {
                icon.loadBinary(client);
            } catch (final IOException ignored) {
            }
        }
    }

    /**
     * 紐付いたControlPointを返す。
     *
     * @return 紐付いたControlPoint
     */
    @Nonnull
    public ControlPoint getControlPoint() {
        return mControlPoint;
    }

    /**
     * SSDPパケットを設定する。
     *
     * <p>同一性はSSDPパケットのUUIDで判断し
     * SSDPパケット受信ごとに更新される
     *
     * @param message SSDPパケット
     */
    void updateSsdpMessage(@Nonnull final SsdpMessage message) {
        if (!isEmbeddedDevice()) {
            final String uuid = message.getUuid();
            if (!getUdn().equals(uuid)) {
                throw new IllegalArgumentException("uuid and udn does not match! uuid=" + uuid + " udn=" + mUdn);
            }
        }
        final String location = message.getLocation();
        if (location == null) {
            throw new IllegalArgumentException();
        }
        mLocation = location;
        mSsdpMessage = message;
        for (final Device device : mDeviceList) {
            device.updateSsdpMessage(message);
        }
    }

    /**
     * 最新のSSDPパケットを返す。
     *
     * @return 最新のSSDPパケット
     */
    @Nonnull
    SsdpMessage getSsdpMessage() {
        return mSsdpMessage;
    }

    /**
     * 更新がなければ無効となる時間[ms]を返す。
     *
     * @return 更新がなければ無効となる時間[ms]
     */
    public long getExpireTime() {
        return mSsdpMessage.getExpireTime();
    }

    /**
     * DeviceDescriptionのXML文字列を返す。
     *
     * @return DeviceDescriptionのXML文字列
     */
    @Nonnull
    public String getDescription() {
        return mDescription;
    }

    /**
     * 受信したインターフェースのScopeIDを返す。
     *
     * @return ScopeID、設定されていない場合(IPv4含む)は0
     */
    public int getScopeId() {
        return mSsdpMessage.getScopeId();
    }

    /**
     * 必要に応じてURLにこのデバイスのScopeIdを付加する。
     *
     * <p>SSDPメッセージがIPv6インターフェース経由で通知されており、
     * そのインターフェースのScopeIDが設定されている場合、
     * かつURLのhost部がIPv6リテラルである場合にのみ付加する。
     *
     * @param url 元となるURL
     * @return ScopeIDが付加されたURL
     * @throws MalformedURLException 不正なURL
     */
    public URL appendScopeIdIfNeed(@Nonnull final String url) throws MalformedURLException {
        return Http.makeUrlWithScopeId(url, getScopeId());
    }

    /**
     * URL情報を正規化して返す。
     *
     * @param url URLパス情報
     * @return 正規化したURL
     * @throws MalformedURLException 不正なURL
     * @see #getAbsoluteUrl(String, String)
     */
    @Nonnull
    URL getAbsoluteUrl(@Nonnull final String url) throws MalformedURLException {
        return appendScopeIdIfNeed(getAbsoluteUrl(getBaseUrl(), url));
    }

    /**
     * URL情報を正規化して返す。
     *
     * <p>URLとして渡される情報は以下のバリエーションがある
     * <ul>
     * <li>"http://"から始まる完全なURL
     * <li>"/"から始まる絶対パス
     * <li>"/"以外から始まる相対パス
     * </ul>
     *
     * <p>一方、baseUrlの情報としては以下のバリエーションを考慮する
     * <ul>
     * <li>"http://10.0.0.1:1000/" ホスト名のみ
     * <li>"http://10.0.0.1:1000" ホスト名のみだが末尾の"/"がない。
     * <li>"http://10.0.0.1:1000/hoge/fuga" ファイル名で終わる
     * <li>"http://10.0.0.1:1000/hoge/fuga/" ディレクトリ名で終わる
     * <li>"http://10.0.0.1:1000/hoge/fuga?a=foo&b=bar" 上記に対してクエリーが付いている
     * </ul>
     *
     * <p>URLが"http://"から始まっていればそのまま利用する。
     * "/"から始まっていればbaseUrlホストの絶対パスとして
     * baseUrlの"://"以降の最初の"/"までと結合する。
     * それ以外の場合はLocationからの相対パスであり、
     * baseUrlから抽出したディレクトリ名と結合する。
     *
     * <p>またscopeIdの付与も行う。
     *
     * @param baseUrl URLパスのベースとなる値
     * @param url     URLパス情報
     * @param scopeId スコープID
     * @return 正規化したURL
     * @throws MalformedURLException 不正なURL
     */
    @Nonnull
    static URL getAbsoluteUrl(
            @Nonnull final String baseUrl,
            @Nonnull final String url,
            final int scopeId)
            throws MalformedURLException {
        return Http.makeUrlWithScopeId(getAbsoluteUrl(baseUrl, url), scopeId);
    }

    @Nonnull
    private static String getAbsoluteUrl(
            @Nonnull final String baseUrl,
            @Nonnull final String url) {
        if (Http.isHttpUrl(url)) {
            return url;
        }
        final String base = Http.removeQuery(baseUrl);
        if (url.startsWith("/")) {
            return Http.makeUrlWithAbsolutePath(base, url);
        }
        return Http.makeUrlWithRelativePath(base, url);
    }

    /**
     * Descriptionに記述されていたタグの値を取得する。
     *
     * <p>個別にメソッドが用意されているものも取得できるが、個別メソッドの利用を推奨。
     * 標準外のタグについても取得できるが、属性値の取得方法は提供されない。
     * また同一タグが複数記述されている場合は最後に記述されていた値が取得される。
     * タグ名にネームスペースプレフィックスは含まない。
     * 複数のネームスペースがある場合は最初に見つかったネームスペースのタグが返される。
     * 値が存在しない場合nullが返る。
     *
     * @param name タグ名
     * @return タグの値
     */
    @Nullable
    public String getValue(@Nonnull final String name) {
        for (final Entry<String, Map<String, String>> entry : mTagMap.entrySet()) {
            final String value = entry.getValue().get(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Descriptionに記述されていたタグの値を取得する。
     *
     * <p>個別にメソッドが用意されているものも取得できるが、個別メソッドの利用を推奨。
     * 標準外のタグについても取得できるが、属性値の取得方法は提供されない、
     * また同一タグが複数記述されている場合は最後に記述されていた値が取得される。
     * ネームスペースはプレフィックスではなく、URIを指定する。
     * 値が存在しない場合nullが返る。
     *
     * @param namespace ネームスペース（URI）
     * @param name      タグ名
     * @return タグの値
     */
    @Nullable
    public String getValueWithNamespace(
            @Nonnull final String namespace,
            @Nonnull final String name) {
        final Map<String, String> map = mTagMap.get(namespace);
        if (map == null) {
            return null;
        }
        return map.get(name);
    }

    /**
     * SSDPパケットに記述されているLocationヘッダの値を返す。
     *
     * @return Locationヘッダの値
     */
    @Nonnull
    public String getLocation() {
        return mLocation;
    }

    /**
     * URLのベースとして使用する値を返す。
     *
     * URLBaseの値が存在する場合はURLBase、存在しない場合はLocationの値を利用する。
     *
     * @return URLのベースとして使用する値
     */
    @Nonnull
    public String getBaseUrl() {
        if (mUrlBase != null) {
            return mUrlBase;
        }
        return mLocation;
    }

    /**
     * Locationに記述のIPアドレスを返す。
     *
     * <p>記述に異常がある場合は空文字が返る。
     *
     * @return IPアドレス
     */
    @Nonnull
    public String getIpAddress() {
        try {
            final URL url = new URL(getLocation());
            return url.getHost();
        } catch (final MalformedURLException e) {
            return "";
        }
    }

    /**
     * UDNタグの値を返す。
     *
     * <p>Required. Unique Device Name. Universally-unique identifier for the device, whether root or embedded.
     * shall be the same over time for a specific device instance (i.e., shall survive reboots).
     * shall match the field value of the NT header field in device discovery messages. shall match the prefix of
     * the USN header field in all discovery messages. (Clause 1, "Discovery" explains the NT and USN header fields.)
     * shall begin with "uuid:" followed by a UUID suffix specified by a UPnP vendor. See clause 1.1.4,
     * "UUID format and recommended generation algorithms" for the MANDATORY UUID format.
     *
     * @return UDNタグの値
     */
    @Nonnull
    public String getUdn() {
        return mUdn;
    }

    /**
     * UPCタグの値を返す。
     *
     * <p>値が存在しない場合nullが返る。
     *
     * <p>Allowed. Universal Product Code. 12-digit, all-numeric code that identifies the consumer package.
     * Managed by the Uniform Code Council. Specified by UPnP vendor. Single UPC.
     *
     * @return UPCタグの値
     */
    @Nullable
    public String getUpc() {
        return mUpc;
    }

    /**
     * deviceTypeタグの値を返す。
     *
     * <p>Required. UPnP device type. Single URI.
     * <ul>
     * <li>For standard devices defined by a UPnP Forum working committee, shall begin with "urn:schemas-upnp-org:device:"
     * followed by the standardized device type suffix, a colon, and an integer device version i.e.
     * urn:schemas-upnp-org:device:deviceType:ver.
     * The highest supported version of the device type shall be specified.
     * <li>For non-standard devices specified by UPnP vendors, shall begin with "urn:", followed by a Vendor Domain Name,
     * followed by ":device:", followed by a device type suffix, colon, and an integer version, i.e.,
     * "urn:domain-name:device:deviceType:ver".
     * Period characters in the Vendor Domain Name shall be replaced with hyphens in accordance with RFC 2141.
     * The highest supported version of the device type shall be specified.
     * </ul>
     * <p>The device type suffix defined by a UPnP Forum working committee or specified by a UPnP vendor shall be &lt;= 64
     * chars, not counting the version suffix and separating colon.
     *
     * @return deviceTypeタグの値
     */
    @Nonnull
    public String getDeviceType() {
        return mDeviceType;
    }

    /**
     * friendlyNameタグの値を返す。
     *
     * <p>Required. Short description for end user. Is allowed to be localized (see ACCEPT- LANGUAGE and
     * CONTENT-LANGUAGE header fields). Specified by UPnP vendor. String. Should be &lt; 64 characters.
     *
     * @return friendlyNameタグの値
     */
    @Nonnull
    public String getFriendlyName() {
        return mFriendlyName;
    }

    /**
     * manufacturerタグの値を返す。
     *
     * <p>値が存在しない場合nullが返る。
     *
     * <p>Required. Manufacturer's name. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header
     * fields). Specified by UPnP vendor. String. Should be &lt; 64 characters.
     *
     * @return manufacturerタグの値
     */
    @Nullable
    public String getManufacture() {
        return mManufacture;
    }

    /**
     * manufacturerURLタグの値を返す。
     *
     * <p>値が存在しない場合nullが返る。
     *
     * <p>Allowed. Web site for Manufacturer. Is allowed to have a different value depending on language requested
     * (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields). Specified by UPnP vendor. Single URL.
     *
     * @return manufacturerURLタグの値
     */
    @Nullable
    public String getManufactureUrl() {
        return mManufactureUrl;
    }

    /**
     * modelNameタグの値を返す。
     *
     * <p>Required. Model name. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT- LANGUAGE header fields).
     * Specified by UPnP vendor. String. Should be &lt; 32 characters.
     *
     * @return modelNameタグの値
     */
    @Nonnull
    public String getModelName() {
        return mModelName;
    }

    /**
     * modelURLタグの値を返す。
     *
     * <p>値が存在しない場合nullが返る。
     *
     * <p>Allowed. Web site for model. Is allowed to have a different value depending on language requested (see
     * ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields). Specified by UPnP vendor. Single URL.
     *
     * @return modelURLタグの値
     */
    @Nullable
    public String getModelUrl() {
        return mModelUrl;
    }

    /**
     * modelDescriptionタグの値を返す。
     *
     * <p>値が存在しない場合nullが返る。
     *
     * <p>Recommended. Long description for end user. Is allowed to be localized (see ACCEPT- LANGUAGE and
     * CONTENT-LANGUAGE header fields). Specified by UPnP vendor. String. Should be &lt; 128 characters.
     *
     * @return modelDescriptionタグの値
     */
    @Nullable
    public String getModelDescription() {
        return mModelDescription;
    }

    /**
     * modelNumberタグの値を返す。
     *
     * <p>値が存在しない場合nullが返る。
     *
     * <p>Recommended. Model number. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields).
     * Specified by UPnP vendor. String. Should be &lt; 32 characters.
     *
     * @return modelNumberタグの値
     */
    @Nullable
    public String getModelNumber() {
        return mModelNumber;
    }

    /**
     * serialNumberタグの値を返す。
     *
     * <p>値が存在しない場合nullが返る。
     *
     * <p>Recommended. Serial number. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields).
     * Specified by UPnP vendor. String. Should be &lt; 64 characters.
     *
     * @return serialNumberタグの値
     */
    @Nullable
    public String getSerialNumber() {
        return mSerialNumber;
    }

    /**
     * presentationURLタグの値を返す。
     *
     * <p>値が存在しない場合nullが返る。
     *
     * <p>Recommended. URL to presentation for device (see clause 5, “Presentation”). shall be relative to the URL at
     * which the device description is located in accordance with the rules specified in clause 5 of RFC 3986.
     * Specified by UPnP vendor. Single URL.
     *
     * @return presentationURLタグの値
     */
    @Nullable
    public String getPresentationUrl() {
        return mPresentationUrl;
    }

    /**
     * Iconのリストを返す。
     *
     * @return Iconのリスト
     * @see Icon
     */
    @Nonnull
    public List<Icon> getIconList() {
        return Collections.unmodifiableList(mIconList);
    }

    /**
     * Serviceのリストを返す。
     *
     * @return Serviceのリスト
     * @see Service
     */
    @Nonnull
    public List<Service> getServiceList() {
        return Collections.unmodifiableList(mServiceList);
    }

    /**
     * 指定文字列とServiceIDが合致するサービスを返す。
     *
     * <p>見つからない場合はnullを返す。
     *
     * @param id サーチするID
     * @return 見つかったService
     * @see Service
     */
    @Nullable
    public Service findServiceById(@Nonnull final String id) {
        for (final Service service : mServiceList) {
            if (service.getServiceId().equals(id)) {
                return service;
            }
        }
        return null;
    }

    /**
     * 指定文字列とServiceTypeが合致するサービスを返す。
     *
     * <p>見つからない場合はnullを返す。
     *
     * @param type サーチするType
     * @return 見つかったService
     * @see Service
     */
    @Nullable
    public Service findServiceByType(@Nonnull final String type) {
        for (final Service service : mServiceList) {
            if (service.getServiceType().equals(type)) {
                return service;
            }
        }
        return null;
    }

    /**
     * 指定文字列の名前を持つActionを返す。
     *
     * <p>全サービスに対して検索をかけ、最初に見つかったものを返す。
     * 見つからない場合はnullを返す。
     *
     * <p>特定のサービスのActionを取得したい場合は、
     * {@link #findServiceById(String)} もしくは
     * {@link #findServiceByType(String)} を使用してServiceを取得した後、
     * {@link Service#findAction(String)} を使用する。
     *
     * @param name Action名
     * @return 見つかったAction
     * @see Service
     * @see Action
     */
    @Nullable
    public Action findAction(@Nonnull final String name) {
        for (final Service service : mServiceList) {
            final Action action = service.findAction(name);
            if (action != null) {
                return action;
            }
        }
        return null;
    }

    /**
     * Embedded Deviceであることを返す。
     *
     * @return Embedded Deviceの時true
     */
    public boolean isEmbeddedDevice() {
        return mParent != null;
    }

    /**
     * Embedded Deviceの場合に親のDeviceを返す。
     *
     * @return 親のDevice、root deviceの時null
     */
    @Nullable
    public Device getParent() {
        return mParent;
    }

    /**
     * Embedded Deviceのリストを返す。
     *
     * @return Embedded Deviceのリスト
     */
    @Nonnull
    public List<Device> getDeviceList() {
        return Collections.unmodifiableList(mDeviceList);
    }

    /**
     * 指定DeviceTypeのEmbedded Deviceを探す。
     *
     * <p>見つからない場合nullが返る。
     *
     * @param deviceType DeviceType
     * @return Embedded Device
     */
    @Nullable
    public Device findDeviceByType(@Nonnull final String deviceType) {
        for (final Device device : mDeviceList) {
            if (deviceType.equals(device.getDeviceType())) {
                return device;
            }
        }
        return null;
    }

    /**
     * 指定DeviceTypeのEmbedded Deviceを再帰的に探す。
     *
     * <p>孫要素以降についても再帰的に探索する。
     * 同一のDeviceTypeが存在する場合は記述順が先にあるものが優先される。
     * <p>見つからない場合nullが返る。
     *
     * @param deviceType DeviceType
     * @return Embedded Device
     */
    @Nullable
    public Device findDeviceByTypeRecursively(@Nonnull final String deviceType) {
        for (final Device device : mDeviceList) {
            if (deviceType.equals(device.getDeviceType())) {
                return device;
            }
            final Device result = device.findDeviceByTypeRecursively(deviceType);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Nonnull
    Set<String> getEmbeddedDeviceUdnSet() {
        if (mDeviceList.isEmpty()) {
            return Collections.emptySet();
        }
        final Set<String> set = new HashSet<>();
        for (final Device device : mDeviceList) {
            set.add(device.getUdn());
            set.addAll(device.getEmbeddedDeviceUdnSet());
        }
        return set;
    }

    @Override
    public int hashCode() {
        return mUdn.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Device)) {
            return false;
        }
        final Device d = (Device) obj;
        return mUdn.equals(d.getUdn());
    }

    @Override
    @Nonnull
    public String toString() {
        return mFriendlyName;
    }
}
