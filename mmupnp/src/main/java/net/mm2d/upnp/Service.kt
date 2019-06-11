/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

/**
 * Interface of UPnP Service.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface Service {
    /**
     * Device that is owner of this Service.
     */
    val device: Device

    /**
     * The value of serviceType.
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
     */
    val serviceType: String

    /**
     * The value of serviceId.
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
     */
    val serviceId: String

    /**
     * The value of SCPDURL.
     *
     * Required. URL for service description. (See clause 2.5, “Service description” below.) shall be relative to
     * the URL at which the device description is located in accordance with clause 5 of RFC 3986. Specified by
     * UPnP vendor. Single URL.
     */
    val scpdUrl: String

    /**
     * The value of controlURL.
     *
     * Required. URL for control (see clause 3, "Control"). shall be relative to the URL at which the device
     * description is located in accordance with clause 5 of RFC 3986. Specified by UPnP vendor. Single URL.
     */
    val controlUrl: String

    /**
     * The value of eventSubURL.
     *
     * Required. URL for eventing (see clause 4, "Eventing"). shall be relative to the URL at which the device
     * description is located in accordance with clause 5 of RFC 3986. shall be unique within the device;
     * any two services shall not have the same URL for eventing. If the service has no evented variables,
     * this element shall be present but shall be empty (i.e., &lt;eventSubURL\&gt;&lt;/eventSubURL&gt;.)
     * Specified by UPnP vendor. Single URL.
     */
    val eventSubUrl: String

    /**
     * XML String of ServiceDescription
     */
    val description: String

    /**
     * List of all Actions held by this service.
     *
     * This list is unmodifiable.
     *
     * @return List of all Actions
     */
    val actionList: List<Action>

    /**
     * List of all StateVariable held by this service.
     *
     * This list is unmodifiable.
     *
     * @return List of all StateVariable
     */
    val stateVariableList: List<StateVariable>

    /**
     * SID (SubscriptionID)
     */
    val subscriptionId: String?

    /**
     * Find the Action by name.
     *
     * null If it can not be found.
     *
     * @param name name of Action
     * @return Action, null If it can not be found.
     */
    fun findAction(name: String): Action?

    /**
     * Find the StateVariable by name.
     *
     * null If it can not be found.
     *
     * @param name name of StateVariable
     * @return StateVariable, null If it can not be found.
     */
    fun findStateVariable(name: String?): StateVariable?

    /**
     * Invoke subscribe synchronously.
     *
     * @param keepRenew true: renew will be performed periodically before Expire.
     * @return true: success, false: otherwise
     */
    fun subscribeSync(keepRenew: Boolean = false): Boolean

    /**
     * Invoke renew subscribe synchronously.
     *
     * @return true: success, false: otherwise
     */
    fun renewSubscribeSync(): Boolean

    /**
     * Invoke unsubscribe synchronously.
     *
     * @return true: success, false: otherwise
     */
    fun unsubscribeSync(): Boolean

    /**
     * Invoke subscribe asynchronously. The result is received by callback.
     *
     * @param keepRenew true: renew will be performed periodically before Expire.
     * @param callback Callback to notify the result. Executed in callback thread.
     * @see ControlPointFactory.create
     */
    fun subscribe(keepRenew: Boolean = false, callback: ((Boolean) -> Unit)? = null)

    /**
     * Invoke renew subscribe asynchronously. The result is received by callback.
     *
     * @param callback Callback to notify the result. Executed in callback thread.
     * @see ControlPointFactory.create
     */
    fun renewSubscribe(callback: ((Boolean) -> Unit)? = null)

    /**
     * Invoke unsubscribe asynchronously. The result is received by callback.
     *
     * @param callback Callback to notify the result. Executed in callback thread.
     * @see ControlPointFactory.create
     */
    fun unsubscribe(callback: ((Boolean) -> Unit)? = null)

    /**
     * Invoke subscribe asynchronously. Suspends the invoking coroutine until the result is received.
     *
     * @param keepRenew true: renew will be performed periodically before Expire.
     * @see ControlPointFactory.create
     */
    suspend fun subscribeAsync(keepRenew: Boolean = false): Boolean

    /**
     * Invoke renew subscribe asynchronously. Suspends the invoking coroutine until the result is received.
     *
     * @see ControlPointFactory.create
     */
    suspend fun renewSubscribeAsync(): Boolean

    /**
     * Invoke unsubscribe asynchronously. Suspends the invoking coroutine until the result is received.
     *
     * @see ControlPointFactory.create
     */
    suspend fun unsubscribeAsync(): Boolean
}
