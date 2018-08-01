/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Serviceを表すインターフェース。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public interface Service {
    /**
     * このServiceを保持するDeviceを返す。
     *
     * @return このServiceを保持するDevice
     */
    @Nonnull
    Device getDevice();

    /**
     * serviceTypeを返す。
     *
     * <p>Required. UPnP service type. Shall not contain a hash character (#, 23 Hex in UTF-8). Single URI.
     * <ul>
     * <li>For standard service types defined by a UPnP Forum working committee, shall begin with
     * "urn:schemas-upnp-org:service:" followed by the standardized service type suffix, colon, and an integer service
     * version i.e. urn:schemas-upnp-org:device:serviceType:ver.
     * The highest supported version of the service type shall be specified.
     * <li>For non-standard service types specified by UPnP vendors, shall begin with "urn:", followed by a
     * Vendor Domain Name, followed by ":service:", followed by a service type suffix, colon,
     * and an integer service version, i.e., "urn:domain-name:service:serviceType:ver".
     * Period characters in the Vendor Domain Name shall be replaced with hyphens in accordance with RFC 2141.
     * The highest supported version of the service type shall be specified.
     * </ul>
     * <p>The service type suffix defined by a UPnP Forum working committee or specified by a UPnP vendor shall be
     * &lt;= 64 characters, not counting the version suffix and separating colon.
     *
     * @return serviceType
     */
    @Nonnull
    String getServiceType();

    /**
     * serviceIdを返す。
     *
     * <p>Required. Service identifier. Shall be unique within this device description. Single URI.
     * <ul>
     * <li>For standard services defined by a UPnP Forum working committee, shall begin with "urn:upnp-org:serviceId:"
     * followed by a service ID suffix i.e. urn:upnp-org:serviceId:serviceID.
     * If this instance of the specified service type (i.e. the &lt;serviceType&gt; element above) corresponds to one of
     * the services defined by the specified device type (i.e. the &lt;deviceType&gt; element above), then the value of
     * the service ID suffix shall be the service ID defined by the device type for this instance of the service.
     * Otherwise, the value of the service ID suffix is vendor defined. (Note that upnp-org is used instead of
     * schemas-upnp- org in this case because an XML schema is not defined for each service ID.)
     * <li>For non-standard services specified by UPnP vendors, shall begin with “urn:”, followed by a Vendor Domain
     * Name, followed by ":serviceId:", followed by a service ID suffix, i.e., "urn:domain- name:serviceId:serviceID".
     * If this instance of the specified service type (i.e. the &lt;serviceType&gt; element above) corresponds to one of
     * the services defined by the specified device type (i.e. the &lt;deviceType&gt; element above), then the value of
     * the service ID suffix shall be the service ID defined by the device type for this instance of the service.
     * Period characters in the Vendor Domain Name shall be replaced with hyphens in accordance with RFC 2141.
     * </ul>
     * <p>The service ID suffix defined by a UPnP Forum working committee or specified by a UPnP vendor shall be &lt;= 64
     * characters.
     *
     * @return serviceId
     */
    @Nonnull
    String getServiceId();

    /**
     * SCPDURLを返す。
     *
     * <p>Required. URL for service description. (See clause 2.5, “Service description” below.) shall be relative to
     * the URL at which the device description is located in accordance with clause 5 of RFC 3986. Specified by
     * UPnP vendor. Single URL.
     *
     * @return SCPDURL
     */
    @Nonnull
    String getScpdUrl();

    /**
     * controlURLを返す。
     *
     * <p>Required. URL for control (see clause 3, "Control"). shall be relative to the URL at which the device
     * description is located in accordance with clause 5 of RFC 3986. Specified by UPnP vendor. Single URL.
     *
     * @return controlURL
     */
    @Nonnull
    String getControlUrl();

    /**
     * eventSubURLを返す。
     *
     * <p>Required. URL for eventing (see clause 4, "Eventing"). shall be relative to the URL at which the device
     * description is located in accordance with clause 5 of RFC 3986. shall be unique within the device;
     * any two services shall not have the same URL for eventing. If the service has no evented variables,
     * this element shall be present but shall be empty (i.e., &lt;eventSubURL\&gt;&lt;/eventSubURL&gt;.)
     * Specified by UPnP vendor. Single URL.
     *
     * @return eventSubURL
     */
    @Nonnull
    String getEventSubUrl();

    /**
     * ServiceDescriptionのXMLを返す。
     *
     * @return ServiceDescription
     */
    @Nonnull
    String getDescription();

    /**
     * このサービスが保持する全Actionのリストを返す。
     *
     * <p>リストは変更不可。
     *
     * @return 全Actionのリスト
     */
    @Nonnull
    List<Action> getActionList();

    /**
     * 名前から該当するActionを探す。
     *
     * <p>見つからない場合はnullが返る。
     *
     * @param name Action名
     * @return 該当するAction、見つからない場合null
     */
    @Nullable
    Action findAction(@Nonnull String name);

    /**
     * 全StateVariableのリストを返す。
     *
     * @return 全StateVariableのリスト
     */
    @Nonnull
    List<StateVariable> getStateVariableList();

    /**
     * 名前から該当するStateVariableを探す。
     *
     * <p>見つからない場合はnullが返る。
     *
     * @param name StateVariable名
     * @return 該当するStateVariable、見つからない場合null
     */
    @Nullable
    StateVariable findStateVariable(@Nullable String name);

    /**
     * Subscribeの実行
     *
     * @return 成功時true
     * @throws IOException 通信エラー
     */
    boolean subscribe() throws IOException;

    /**
     * Subscribeの実行
     *
     * @param keepRenew trueを指定すると成功後、Expire前に定期的にrenewを行う。
     * @return 成功時true
     * @throws IOException 通信エラー
     */
    boolean subscribe(boolean keepRenew) throws IOException;

    /**
     * RenewSubscribeを実行する
     *
     * @return 成功時true
     * @throws IOException 通信エラー
     */
    boolean renewSubscribe() throws IOException;

    /**
     * Unsubscribeを実行する
     *
     * @return 成功時true
     * @throws IOException 通信エラー
     */
    boolean unsubscribe() throws IOException;

    /**
     * Subscribeの期限切れ通知
     */
    void expired();

    /**
     * SID(SubscriptionID)を返す。
     *
     * @return SubscriptionID
     */
    @Nullable
    String getSubscriptionId();

    /**
     * Subscriptionの開始時刻
     *
     * @return Subscriptionの開始時刻
     */
    long getSubscriptionStart();

    /**
     * Subscriptionの有効期間
     *
     * @return Subscriptionの有効期間
     */
    long getSubscriptionTimeout();

    /**
     * Subscriptionの有効期限
     *
     * @return Subscriptionの有効期限
     */
    long getSubscriptionExpiryTime();

}
