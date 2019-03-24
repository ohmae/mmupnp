/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

/**
 * Serviceを表すインターフェース。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface Service {
    /**
     * このServiceを保持するDeviceを返す。
     *
     * @return このServiceを保持するDevice
     */
    val device: Device

    /**
     * serviceTypeを返す。
     *
     * Required. UPnP service type. Shall not contain a hash character (#, 23 Hex in UTF-8). Single URI.
     *
     *  * For standard service types defined by a UPnP Forum working committee, shall begin with
     * "urn:schemas-upnp-org:service:" followed by the standardized service type suffix, colon, and an integer service
     * version i.e. urn:schemas-upnp-org:device:serviceType:ver.
     * The highest supported version of the service type shall be specified.
     *  * For non-standard service types specified by UPnP vendors, shall begin with "urn:", followed by a
     * Vendor Domain Name, followed by ":service:", followed by a service type suffix, colon,
     * and an integer service version, i.e., "urn:domain-name:service:serviceType:ver".
     * Period characters in the Vendor Domain Name shall be replaced with hyphens in accordance with RFC 2141.
     * The highest supported version of the service type shall be specified.
     *
     * The service type suffix defined by a UPnP Forum working committee or specified by a UPnP vendor shall be
     * &lt;= 64 characters, not counting the version suffix and separating colon.
     *
     * @return serviceType
     */
    val serviceType: String

    /**
     * serviceIdを返す。
     *
     * Required. Service identifier. Shall be unique within this device description. Single URI.
     *
     * - For standard services defined by a UPnP Forum working committee, shall begin with "urn:upnp-org:serviceId:"
     * followed by a service ID suffix i.e. urn:upnp-org:serviceId:serviceID.
     * If this instance of the specified service type (i.e. the &lt;serviceType&gt; element above) corresponds to one of
     * the services defined by the specified device type (i.e. the &lt;deviceType&gt; element above), then the value of
     * the service ID suffix shall be the service ID defined by the device type for this instance of the service.
     * Otherwise, the value of the service ID suffix is vendor defined. (Note that upnp-org is used instead of
     * schemas-upnp- org in this case because an XML schema is not defined for each service ID.)
     * - For non-standard services specified by UPnP vendors, shall begin with “urn:”, followed by a Vendor Domain
     * Name, followed by ":serviceId:", followed by a service ID suffix, i.e., "urn:domain- name:serviceId:serviceID".
     * If this instance of the specified service type (i.e. the &lt;serviceType&gt; element above) corresponds to one of
     * the services defined by the specified device type (i.e. the &lt;deviceType&gt; element above), then the value of
     * the service ID suffix shall be the service ID defined by the device type for this instance of the service.
     * Period characters in the Vendor Domain Name shall be replaced with hyphens in accordance with RFC 2141.
     *
     * The service ID suffix defined by a UPnP Forum working committee or specified by a UPnP vendor shall be &lt;= 64
     * characters.
     *
     * @return serviceId
     */
    val serviceId: String

    /**
     * SCPDURLを返す。
     *
     * Required. URL for service description. (See clause 2.5, “Service description” below.) shall be relative to
     * the URL at which the device description is located in accordance with clause 5 of RFC 3986. Specified by
     * UPnP vendor. Single URL.
     *
     * @return SCPDURL
     */
    val scpdUrl: String

    /**
     * controlURLを返す。
     *
     * Required. URL for control (see clause 3, "Control"). shall be relative to the URL at which the device
     * description is located in accordance with clause 5 of RFC 3986. Specified by UPnP vendor. Single URL.
     *
     * @return controlURL
     */
    val controlUrl: String

    /**
     * eventSubURLを返す。
     *
     * Required. URL for eventing (see clause 4, "Eventing"). shall be relative to the URL at which the device
     * description is located in accordance with clause 5 of RFC 3986. shall be unique within the device;
     * any two services shall not have the same URL for eventing. If the service has no evented variables,
     * this element shall be present but shall be empty (i.e., &lt;eventSubURL\&gt;&lt;/eventSubURL&gt;.)
     * Specified by UPnP vendor. Single URL.
     *
     * @return eventSubURL
     */
    val eventSubUrl: String

    /**
     * ServiceDescriptionのXMLを返す。
     *
     * @return ServiceDescription
     */
    val description: String

    /**
     * このサービスが保持する全Actionのリストを返す。
     *
     * リストは変更不可。
     *
     * @return 全Actionのリスト
     */
    val actionList: List<Action>

    /**
     * 全StateVariableのリストを返す。
     *
     * @return 全StateVariableのリスト
     */
    val stateVariableList: List<StateVariable>

    /**
     * SID(SubscriptionID)を返す。
     *
     * @return SubscriptionID
     */
    val subscriptionId: String?

    /**
     * 名前から該当するActionを探す。
     *
     * 見つからない場合はnullが返る。
     *
     * @param name Action名
     * @return 該当するAction、見つからない場合null
     */
    fun findAction(name: String): Action?

    /**
     * 名前から該当するStateVariableを探す。
     *
     * 見つからない場合はnullが返る。
     *
     * @param name StateVariable名
     * @return 該当するStateVariable、見つからない場合null
     */
    fun findStateVariable(name: String?): StateVariable?

    /**
     * Subscribeの同期実行
     *
     * @param keepRenew trueを指定すると成功後、Expire前に定期的にrenewを行う。
     * @return 成功時true
     */
    fun subscribeSync(keepRenew: Boolean = false): Boolean

    /**
     * RenewSubscribeを同期実行する
     *
     * @return 成功時true
     */
    fun renewSubscribeSync(): Boolean

    /**
     * Unsubscribeを同期実行する
     *
     * @return 成功時true
     */
    fun unsubscribeSync(): Boolean

    /**
     * Subscribeの非同期実行
     *
     * @param keepRenew trueを指定すると成功後、Expire前に定期的にrenewを行う。
     * @param callback  結果を通知するコールバック。callbackスレッドで実行される。
     * @see ControlPointFactory.create
     */
    fun subscribe(keepRenew: Boolean = false, callback: ((Boolean) -> Unit)? = null)

    /**
     * RenewSubscribeを非同期実行する
     *
     * @param callback  結果を通知するコールバック。callbackスレッドで実行される。
     * @see ControlPointFactory.create
     */
    fun renewSubscribe(callback: ((Boolean) -> Unit)? = null)

    /**
     * Unsubscribeを非同期実行する
     *
     * @param callback  結果を通知するコールバック。callbackスレッドで実行される。
     * @see ControlPointFactory.create
     */
    fun unsubscribe(callback: ((Boolean) -> Unit)? = null)

    /**
     * Subscribeの非同期実行
     *
     * @param keepRenew trueを指定すると成功後、Expire前に定期的にrenewを行う。
     * @see ControlPointFactory.create
     */
    suspend fun subscribeAsync(keepRenew: Boolean = false): Boolean

    /**
     * RenewSubscribeを非同期実行する
     *
     * @see ControlPointFactory.create
     */
    suspend fun renewSubscribeAsync(): Boolean

    /**
     * Unsubscribeを非同期実行する
     *
     * @see ControlPointFactory.create
     */
    suspend fun unsubscribeAsync(): Boolean
}
