/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

/**
 * Interface of UPnP Device.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface Device {
    /**
     * ControlPoint that is owner of this Device.
     */
    val controlPoint: ControlPoint

    /**
     * The latest SSDP packet.
     */
    val ssdpMessage: SsdpMessage

    /**
     * The time to expire (ms) if there is no update.
     */
    val expireTime: Long

    /**
     * XML string of DeviceDescription.
     */
    val description: String

    /**
     * ScopeID of the received interface. 0 if not set (including IPv4).
     */
    val scopeId: Int

    /**
     * Location described in SSDP packet
     */
    val location: String

    /**
     * Base URL.
     *
     * If URLBase exists, URLBase is used, otherwise Location is used.
     */
    val baseUrl: String

    /**
     * IP address described in Location
     *
     * If there is an error in the description, this is empty string.
     */
    val ipAddress: String

    /**
     * Value of UDN tag.
     *
     * Required. Unique Device Name. Universally-unique identifier for the device, whether root or embedded.
     * shall be the same over time for a specific device instance (i.e., shall survive reboots).
     * shall match the field value of the NT header field in device discovery messages. shall match the prefix of
     * the USN header field in all discovery messages. (Clause 1, "Discovery" explains the NT and USN header fields.)
     * shall begin with "uuid:" followed by a UUID suffix specified by a UPnP vendor. See clause 1.1.4,
     * "UUID format and recommended generation algorithms" for the MANDATORY UUID format.
     */
    val udn: String

    /**
     * Value of UPC tag.
     *
     * null if value does not exist
     *
     * Allowed. Universal Product Code. 12-digit, all-numeric code that identifies the consumer package.
     * Managed by the Uniform Code Council. Specified by UPnP vendor. Single UPC.
     */
    val upc: String?

    /**
     * Value of deviceType tag.
     *
     * Required. UPnP device type. Single URI.
     *
     *  * For standard devices defined by a UPnP Forum working committee, shall begin with "urn:schemas-upnp-org:device:"
     * followed by the standardized device type suffix, a colon, and an integer device version i.e.
     * urn:schemas-upnp-org:device:deviceType:ver.
     * The highest supported version of the device type shall be specified.
     *  * For non-standard devices specified by UPnP vendors, shall begin with "urn:", followed by a Vendor Domain Name,
     * followed by ":device:", followed by a device type suffix, colon, and an integer version, i.e.,
     * "urn:domain-name:device:deviceType:ver".
     * Period characters in the Vendor Domain Name shall be replaced with hyphens in accordance with RFC 2141.
     * The highest supported version of the device type shall be specified.
     *
     * The device type suffix defined by a UPnP Forum working committee or specified by a UPnP vendor shall be &lt;= 64
     * chars, not counting the version suffix and separating colon.
     */
    val deviceType: String

    /**
     * Value of friendlyName tag.
     *
     * Required. Short description for end user. Is allowed to be localized (see ACCEPT- LANGUAGE and
     * CONTENT-LANGUAGE header fields). Specified by UPnP vendor. String. Should be &lt; 64 characters.
     */
    val friendlyName: String

    /**
     * Value of manufacturer tag.
     *
     * null if value does not exist.
     *
     * Required. Manufacturer's name. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header
     * fields). Specified by UPnP vendor. String. Should be &lt; 64 characters.
     */
    val manufacture: String?

    /**
     * Value of manufacturerURL tag.
     *
     * null if value does not exist.
     *
     * Allowed. Web site for Manufacturer. Is allowed to have a different value depending on language requested
     * (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields). Specified by UPnP vendor. Single URL.
     */
    val manufactureUrl: String?

    /**
     * Value of modelName tag.
     *
     * Required. Model name. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT- LANGUAGE header fields).
     * Specified by UPnP vendor. String. Should be &lt; 32 characters.
     */
    val modelName: String

    /**
     * Value of modelURL tag.
     *
     * null if value does not exist.
     *
     * Allowed. Web site for model. Is allowed to have a different value depending on language requested (see
     * ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields). Specified by UPnP vendor. Single URL.
     */
    val modelUrl: String?

    /**
     * Value of modelDescription tag.
     *
     * null if value does not exist.
     *
     * Recommended. Long description for end user. Is allowed to be localized (see ACCEPT- LANGUAGE and
     * CONTENT-LANGUAGE header fields). Specified by UPnP vendor. String. Should be &lt; 128 characters.
     */
    val modelDescription: String?

    /**
     * Value of modelNumber tag.
     *
     * null if value does not exist.
     *
     * Recommended. Model number. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields).
     * Specified by UPnP vendor. String. Should be &lt; 32 characters.
     */
    val modelNumber: String?

    /**
     * Value of serialNumber tag.
     *
     * null if value does not exist.
     *
     * Recommended. Serial number. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields).
     * Specified by UPnP vendor. String. Should be &lt; 64 characters.
     */
    val serialNumber: String?

    /**
     * Value of presentationURL tag.
     *
     * null if value does not exist.
     *
     * Recommended. URL to presentation for device (see clause 5, “Presentation”). shall be relative to the URL at
     * which the device description is located in accordance with the rules specified in clause 5 of RFC 3986.
     * Specified by UPnP vendor. Single URL.
     */
    val presentationUrl: String?

    /**
     * Icon list.
     *
     * @see Icon
     */
    val iconList: List<Icon>

    /**
     * Service list.
     *
     * @see Service
     */
    val serviceList: List<Service>

    /**
     * Whether this is Embedded Device.
     */
    val isEmbeddedDevice: Boolean

    /**
     * The parent Device if this is Embedded Device. null if this is root device
     */
    val parent: Device?

    /**
     * Embedded Device list.
     */
    val deviceList: List<Device>

    /**
     * Whether this is Pinned device.
     */
    val isPinned: Boolean

    /**
     * Load the binary data of Icon.
     *
     * @param client HttpClient
     * @param filter Filter which selects Icon to load
     */
    fun loadIconBinary(client: SingleHttpClient, filter: IconFilter)

    /**
     * update SSDP packet
     *
     * Updated each time an SSDP packet is received
     *
     * @param message SSDP packet
     */
    fun updateSsdpMessage(message: SsdpMessage)

    /**
     * Get the value of the tag described in the Description.
     *
     * Non-standard tags can also be obtained, but no method for obtaining attribute values is provided.
     * when the same tag is described more than once, the value described at the end is returned.
     * Tag names do not include namespace prefixes.
     * If there are multiple namespaces, the tag of the first namespace found is returned.
     * If the value does not exist, null is returned.
     *
     * @param name tag name
     * @return tag value
     */
    fun getValue(name: String): String?

    /**
     * Get the value of the tag described in the Description.
     *
     * Non-standard tags can also be obtained, but no method for obtaining attribute values is provided.
     * when the same tag is described more than once, the value described at the end is returned.
     * A namespace is not a prefix but a URI.
     * If the value does not exist, null is returned.
     *
     * @param namespace tag namespace (URI)
     * @param name tag name
     * @return tag value
     */
    fun getValueWithNamespace(
        namespace: String,
        name: String
    ): String?

    /**
     * Find Service by ServiceID.
     *
     * If it can not be found, it returns null.
     *
     * @param id ServiceID
     * @return Service
     * @see Service
     */
    fun findServiceById(id: String): Service?

    /**
     * Find Service by ServiceType.
     *
     * If it can not be found, it returns null.
     *
     * @param type ServiceType
     * @return Service
     * @see Service
     */
    fun findServiceByType(type: String): Service?

    /**
     * Find Action by Action name.
     *
     * Search from all services and return the first one found.
     * If it can not be found, it returns null.
     *
     * If you want to obtain the action of a specific service,
     * use [Service.findAction] after obtaining Service
     * using [findServiceById] or [findServiceByType].
     *
     * @param name Action name
     * @return Action
     * @see Service
     * @see Action
     */
    fun findAction(name: String): Action?

    /**
     * Find Embedded Device by DeviceType.
     *
     * If it can not be found, it returns null.
     *
     * @param deviceType DeviceType
     * @return Embedded Device
     */
    fun findDeviceByType(deviceType: String): Device?

    /**
     * Find Embedded Device by DeviceType recursively.
     *
     * Search recursively for grandchildren and beyond.
     * If the same DeviceType exists, the one with the description order first takes precedence.
     *
     * If it can not be found, it returns null.
     *
     * @param deviceType DeviceType
     * @return Embedded Device
     */
    fun findDeviceByTypeRecursively(deviceType: String): Device?
}
