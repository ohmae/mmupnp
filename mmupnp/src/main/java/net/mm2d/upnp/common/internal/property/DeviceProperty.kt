/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.property

/**
 * Interface of UPnP Device.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class DeviceProperty(
    /**
     * XML string of DeviceDescription.
     */
    val description: String,

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
    val udn: String,

    /**
     * Value of UPC tag.
     *
     * null if value does not exist
     *
     * Allowed. Universal Product Code. 12-digit, all-numeric code that identifies the consumer package.
     * Managed by the Uniform Code Council. Specified by UPnP vendor. Single UPC.
     */
    val upc: String? = null,

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
    val deviceType: String,

    /**
     * Value of friendlyName tag.
     *
     * Required. Short description for end user. Is allowed to be localized (see ACCEPT- LANGUAGE and
     * CONTENT-LANGUAGE header fields). Specified by UPnP vendor. String. Should be &lt; 64 characters.
     */
    val friendlyName: String,

    /**
     * Value of manufacturer tag.
     *
     * null if value does not exist.
     *
     * Required. Manufacturer's name. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header
     * fields). Specified by UPnP vendor. String. Should be &lt; 64 characters.
     */
    val manufacture: String? = null,

    /**
     * Value of manufacturerURL tag.
     *
     * null if value does not exist.
     *
     * Allowed. Web site for Manufacturer. Is allowed to have a different value depending on language requested
     * (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields). Specified by UPnP vendor. Single URL.
     */
    val manufactureUrl: String? = null,

    /**
     * Value of modelName tag.
     *
     * Required. Model name. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT- LANGUAGE header fields).
     * Specified by UPnP vendor. String. Should be &lt; 32 characters.
     */
    val modelName: String,

    /**
     * Value of modelURL tag.
     *
     * null if value does not exist.
     *
     * Allowed. Web site for model. Is allowed to have a different value depending on language requested (see
     * ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields). Specified by UPnP vendor. Single URL.
     */
    val modelUrl: String? = null,

    /**
     * Value of modelDescription tag.
     *
     * null if value does not exist.
     *
     * Recommended. Long description for end user. Is allowed to be localized (see ACCEPT- LANGUAGE and
     * CONTENT-LANGUAGE header fields). Specified by UPnP vendor. String. Should be &lt; 128 characters.
     */
    val modelDescription: String? = null,

    /**
     * Value of modelNumber tag.
     *
     * null if value does not exist.
     *
     * Recommended. Model number. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields).
     * Specified by UPnP vendor. String. Should be &lt; 32 characters.
     */
    val modelNumber: String? = null,

    /**
     * Value of serialNumber tag.
     *
     * null if value does not exist.
     *
     * Recommended. Serial number. Is allowed to be localized (see ACCEPT-LANGUAGE and CONTENT-LANGUAGE header fields).
     * Specified by UPnP vendor. String. Should be &lt; 64 characters.
     */
    val serialNumber: String? = null,

    /**
     * Value of presentationURL tag.
     *
     * null if value does not exist.
     *
     * Recommended. URL to presentation for device (see clause 5, “Presentation”). shall be relative to the URL at
     * which the device description is located in accordance with the rules specified in clause 5 of RFC 3986.
     * Specified by UPnP vendor. Single URL.
     */
    val presentationUrl: String? = null,

    /**
     * Icon list.
     *
     * @see IconProperty
     */
    val iconList: List<IconProperty> = emptyList(),

    /**
     * Service list.
     *
     * @see ServiceProperty
     */
    val serviceList: List<ServiceProperty> = emptyList(),

    /**
     * URLBase.
     *
     * Use of URLBase is deprecated from UPnP 1.1 onwards;
     * UPnP 2.0 devices shall NOT include URLBase in their description.
     * This field is only used in ControlPoint for backward compatibility.
     * Do not use in DeviceArchitecture.
     */
    val urlBase: String? = null,

    /**
     * The parent Device if this is Embedded Device. null if this is root device
     */
    val parent: DeviceProperty? = null,

    private val tagMap: Map<String, Map<String, String>>,

    deviceBuilderList: List<Builder> = emptyList()
) {
    /**
     * Whether this is Embedded Device.
     */
    val isEmbeddedDevice: Boolean = parent != null

    /**
     * Embedded Device list.
     */
    val deviceList: List<DeviceProperty> = deviceBuilderList.map { it.build(this) }

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
    fun getValue(name: String): String? =
        tagMap.values
            .asSequence()
            .mapNotNull { it[name] }
            .firstOrNull()


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
    ): String? = tagMap[namespace]?.get(name)

    class Builder {
        var description: String? = null
        var udn: String? = null
        var upc: String? = null
        var deviceType: String? = null
        var friendlyName: String? = null
        var manufacture: String? = null
        var manufactureUrl: String? = null
        var modelName: String? = null
        var modelUrl: String? = null
        var modelDescription: String? = null
        var modelNumber: String? = null
        var serialNumber: String? = null
        var presentationUrl: String? = null
        var urlBase: String? = null
        val iconList: MutableList<IconProperty> = mutableListOf()
        val serviceBuilderList: MutableList<ServiceProperty.Builder> = mutableListOf()
        val deviceBuilderList: MutableList<Builder> = mutableListOf()
        // DeviceDescriptionにはAttributeは使用されていないためAttributeには非対応
        private val tagMap: MutableMap<String, MutableMap<String, String>> = mutableMapOf()

        fun putTag(namespaceUri: String?, tag: String, value: String) {
            tagMap.getOrPut(namespaceUri ?: "") { mutableMapOf() }[tag] = value
        }

        fun createDeviceBuilder(): Builder = Builder().also {
            it.description = description
            it.urlBase = urlBase
        }

        fun build(parent: DeviceProperty? = null): DeviceProperty {
            val description = checkNotNull(description) { "description must be set." }
            val deviceType = checkNotNull(deviceType) { "deviceType must be set." }
            val friendlyName = checkNotNull(friendlyName) { "friendlyName must be set." }
            val manufacture = checkNotNull(manufacture) { "manufacturer must be set." }
            val modelName = checkNotNull(modelName) { "modelName must be set." }
            val udn = checkNotNull(udn) { "UDN must be set." }

            return DeviceProperty(
                parent = parent,
                description = description,
                udn = udn,
                upc = upc,
                deviceType = deviceType,
                friendlyName = friendlyName,
                manufacture = manufacture,
                manufactureUrl = manufactureUrl,
                modelName = modelName,
                modelUrl = modelUrl,
                modelDescription = modelDescription,
                modelNumber = modelNumber,
                serialNumber = serialNumber,
                presentationUrl = presentationUrl,
                urlBase = urlBase,
                tagMap = tagMap,
                iconList = iconList,
                serviceList = serviceBuilderList.map { it.build() },
                deviceBuilderList = deviceBuilderList
            )
        }
    }
}
