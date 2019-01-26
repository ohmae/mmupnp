/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl;

import net.mm2d.upnp.Action;
import net.mm2d.upnp.ControlPoint;
import net.mm2d.upnp.Device;
import net.mm2d.upnp.HttpClient;
import net.mm2d.upnp.Icon;
import net.mm2d.upnp.IconFilter;
import net.mm2d.upnp.Service;
import net.mm2d.upnp.SsdpMessage;
import net.mm2d.upnp.internal.manager.SubscribeManager;
import net.mm2d.upnp.internal.message.PinnedSsdpMessage;

import java.io.IOException;
import java.net.InetAddress;
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

/**
 * Deviceの実装。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class DeviceImpl implements Device {

    /**
     * DeviceのBuilder。
     *
     * <p>XMLファイルの読み込み処理もBuilderに対して行う。
     */
    public static class Builder {
        @Nonnull
        private final ControlPoint mControlPoint;
        @Nonnull
        private final SubscribeManager mSubscribeManager;
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
        private final List<Icon> mIconList = new ArrayList<>();
        @Nonnull
        private final List<ServiceImpl.Builder> mServiceBuilderList = new ArrayList<>();
        @Nonnull
        private volatile List<DeviceImpl.Builder> mDeviceBuilderList = Collections.emptyList();
        @Nonnull
        private final Map<String, Map<String, String>> mTagMap;

        /**
         * インスタンスを作成する。
         *
         * @param controlPoint     ControlPoint
         * @param subscribeManager 購読状態マネージャ
         * @param message          SSDPパケット
         */
        public Builder(
                @Nonnull final ControlPoint controlPoint,
                @Nonnull final SubscribeManager subscribeManager,
                @Nonnull final SsdpMessage message) {
            mControlPoint = controlPoint;
            mSubscribeManager = subscribeManager;
            mLocation = getLocationWithException(message);
            mSsdpMessage = message;
            mTagMap = new LinkedHashMap<>();
            mTagMap.put("", new HashMap<>());
        }

        /**
         * EmbeddedDevice用のBuilderを作成する。
         *
         * @return EmbeddedDevice用のBuilder
         */
        public Builder createEmbeddedDeviceBuilder() {
            final Builder builder = new Builder(mControlPoint, mSubscribeManager, mSsdpMessage);
            builder.setDescription(mDescription);
            builder.setUrlBase(mUrlBase);
            return builder;
        }

        /**
         * Descriptionのダウンロード完了時にダウンロードに使用したHttpClientを渡す。
         *
         * @param client Descriptionのダウンロードに使用したHttpClient
         */
        public void setDownloadInfo(@Nonnull final HttpClient client) {
            if (mSsdpMessage instanceof PinnedSsdpMessage) {
                final InetAddress address = client.getLocalAddress();
                if (address != null) {
                    ((PinnedSsdpMessage) mSsdpMessage).setLocalAddress(address);
                } else {
                    throw new IllegalStateException("HttpClient is not connected yet.");
                }
            }
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
        String getUuid() {
            return mSsdpMessage.getUuid();
        }

        /**
         * 最新のSSDPパケットを返す。
         *
         * @return 最新のSSDPパケット
         */
        @Nonnull
        public SsdpMessage getSsdpMessage() {
            return mSsdpMessage;
        }

        /**
         * SSDPパケットを登録する。
         *
         * @param message SSDPパケット
         */
        void updateSsdpMessage(@Nonnull final SsdpMessage message) {
            if (mSsdpMessage instanceof PinnedSsdpMessage) {
                return;
            }
            mLocation = getLocationWithException(message);
            mSsdpMessage = message;
            for (final Builder builder : mDeviceBuilderList) {
                builder.updateSsdpMessage(message);
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
         * <p>URLBaseは1.1以降Deprecated
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
         * <p>URLBaseの値が存在する場合はURLBase、存在しない場合はLocationの値を利用する。
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
         * @param icon Icon
         * @return Builder
         */
        @Nonnull
        public Builder addIcon(@Nonnull final Icon icon) {
            mIconList.add(icon);
            return this;
        }

        /**
         * ServiceのBuilderを登録する。
         *
         * @param builder ServiceのBuilder
         * @return Builder
         */
        @Nonnull
        public Builder addServiceBuilder(@Nonnull final ServiceImpl.Builder builder) {
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
        public List<ServiceImpl.Builder> getServiceBuilderList() {
            return mServiceBuilderList;
        }

        /**
         * Embedded DeviceのBuilderのリストを返す。
         *
         * @return Embedded DeviceのBuilderのリスト
         */
        @Nonnull
        public List<DeviceImpl.Builder> getEmbeddedDeviceBuilderList() {
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
        @SuppressWarnings("Java8MapApi")
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
        public DeviceImpl build() {
            return build(null);
        }

        /**
         * Deviceのインスタンスを作成する。
         *
         * @param parent 親Device、EmbeddedDeviceの場合に指定
         * @return Deviceのインスタンス
         */
        @Nonnull
        DeviceImpl build(@Nullable final Device parent) {
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
            if (parent == null) {
                // Sometime Embedded devices have different UUIDs. So, do not check when embedded device
                if (mSsdpMessage instanceof PinnedSsdpMessage) {
                    ((PinnedSsdpMessage) mSsdpMessage).setUuid(mUdn);
                } else if (!mUdn.equals(getUuid())) {
                    throw new IllegalStateException("uuid and udn does not match! uuid=" + getUuid() + " udn=" + mUdn);
                }
            }
            return new DeviceImpl(parent, this);
        }

        public String toDumpString() {
            final StringBuilder sb = new StringBuilder()
                    .append("DeviceBuilder")
                    .append("\nSSDP:").append(mSsdpMessage.toString())
                    .append("\nDESCRIPTION:").append(mDescription);
            if (mServiceBuilderList.size() != 0) {
                sb.append("\n");
                for (final ServiceImpl.Builder serviceBuilder : mServiceBuilderList) {
                    sb.append(serviceBuilder.toDumpString());
                }
            }
            return sb.toString();
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
    private DeviceImpl(
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
        mIconList = builder.mIconList.isEmpty() ? Collections.emptyList() : new ArrayList<>(builder.mIconList);
        mServiceList = buildServiceList(this, builder.mSubscribeManager, builder.mServiceBuilderList);
        mDeviceList = buildDeviceList(this, builder.mDeviceBuilderList);
    }

    @Nonnull
    private static List<Service> buildServiceList(
            @Nonnull final Device device,
            @Nonnull final SubscribeManager manager,
            @Nonnull final List<ServiceImpl.Builder> builderList) {
        if (builderList.isEmpty()) {
            return Collections.emptyList();
        }
        final List<Service> list = new ArrayList<>(builderList.size());
        for (final ServiceImpl.Builder builder : builderList) {
            final Service service = builder
                    .setDevice(device)
                    .setSubscribeManager(manager)
                    .build();
            list.add(service);
        }
        return list;
    }

    @Nonnull
    private static List<Device> buildDeviceList(
            @Nonnull final Device parent,
            @Nonnull final List<DeviceImpl.Builder> builderList) {
        if (builderList.isEmpty()) {
            return Collections.emptyList();
        }
        final List<Device> list = new ArrayList<>(builderList.size());
        for (final DeviceImpl.Builder builder : builderList) {
            list.add(builder.build(parent));
        }
        return list;
    }

    @Nonnull
    private static String getLocationWithException(@Nonnull final SsdpMessage ssdpMessage)
            throws IllegalArgumentException {
        final String location = ssdpMessage.getLocation();
        if (location == null) {
            throw new IllegalArgumentException();
        }
        return location;
    }

    @Override
    public void loadIconBinary(
            @Nonnull final HttpClient client,
            @Nonnull final IconFilter filter) {
        if (mIconList.isEmpty()) {
            return;
        }
        final List<Icon> loadList = filter.filter(mIconList);
        for (final Icon icon : loadList) {
            if (icon instanceof IconImpl) {
                try {
                    ((IconImpl) icon).loadBinary(client, getBaseUrl(), getScopeId());
                } catch (final IOException ignored) {
                }
            }
        }
    }

    @Override
    @Nonnull
    public ControlPoint getControlPoint() {
        return mControlPoint;
    }

    @Override
    public void updateSsdpMessage(@Nonnull final SsdpMessage message) {
        if (isPinned()) {
            return;
        }
        if (!isEmbeddedDevice()) {
            final String uuid = message.getUuid();
            if (!getUdn().equals(uuid)) {
                throw new IllegalArgumentException("uuid and udn does not match! uuid=" + uuid + " udn=" + mUdn);
            }
        }
        mLocation = getLocationWithException(message);
        mSsdpMessage = message;
        for (final Device device : mDeviceList) {
            device.updateSsdpMessage(message);
        }
    }

    @Override
    @Nonnull
    public SsdpMessage getSsdpMessage() {
        return mSsdpMessage;
    }

    @Override
    public long getExpireTime() {
        return mSsdpMessage.getExpireTime();
    }

    @Override
    @Nonnull
    public String getDescription() {
        return mDescription;
    }

    @Override
    public int getScopeId() {
        return mSsdpMessage.getScopeId();
    }

    @Override
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

    @Override
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

    @Override
    @Nonnull
    public String getLocation() {
        return mLocation;
    }

    @Override
    @Nonnull
    public String getBaseUrl() {
        if (mUrlBase != null) {
            return mUrlBase;
        }
        return mLocation;
    }

    @Override
    @Nonnull
    public String getIpAddress() {
        try {
            final URL url = new URL(getLocation());
            return url.getHost();
        } catch (final MalformedURLException e) {
            return "";
        }
    }

    @Override
    @Nonnull
    public String getUdn() {
        return mUdn;
    }

    @Override
    @Nullable
    public String getUpc() {
        return mUpc;
    }

    @Override
    @Nonnull
    public String getDeviceType() {
        return mDeviceType;
    }

    @Override
    @Nonnull
    public String getFriendlyName() {
        return mFriendlyName;
    }

    @Override
    @Nullable
    public String getManufacture() {
        return mManufacture;
    }

    @Override
    @Nullable
    public String getManufactureUrl() {
        return mManufactureUrl;
    }

    @Override
    @Nonnull
    public String getModelName() {
        return mModelName;
    }

    @Override
    @Nullable
    public String getModelUrl() {
        return mModelUrl;
    }

    @Override
    @Nullable
    public String getModelDescription() {
        return mModelDescription;
    }

    @Override
    @Nullable
    public String getModelNumber() {
        return mModelNumber;
    }

    @Override
    @Nullable
    public String getSerialNumber() {
        return mSerialNumber;
    }

    @Override
    @Nullable
    public String getPresentationUrl() {
        return mPresentationUrl;
    }

    @Override
    @Nonnull
    public List<Icon> getIconList() {
        return Collections.unmodifiableList(mIconList);
    }

    @Override
    @Nonnull
    public List<Service> getServiceList() {
        return Collections.unmodifiableList(mServiceList);
    }

    @Override
    @Nullable
    public Service findServiceById(@Nonnull final String id) {
        for (final Service service : mServiceList) {
            if (service.getServiceId().equals(id)) {
                return service;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public Service findServiceByType(@Nonnull final String type) {
        for (final Service service : mServiceList) {
            if (service.getServiceType().equals(type)) {
                return service;
            }
        }
        return null;
    }

    @Override
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

    @Override
    public boolean isEmbeddedDevice() {
        return mParent != null;
    }

    @Override
    @Nullable
    public Device getParent() {
        return mParent;
    }

    @Override
    @Nonnull
    public List<Device> getDeviceList() {
        return Collections.unmodifiableList(mDeviceList);
    }

    @Override
    @Nullable
    public Device findDeviceByType(@Nonnull final String deviceType) {
        for (final Device device : mDeviceList) {
            if (deviceType.equals(device.getDeviceType())) {
                return device;
            }
        }
        return null;
    }

    @Override
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

    @Override
    public boolean isPinned() {
        return mSsdpMessage instanceof PinnedSsdpMessage;
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
