/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.upnp.Action
import net.mm2d.upnp.Device
import net.mm2d.upnp.Icon
import net.mm2d.upnp.IconFilter
import net.mm2d.upnp.Service
import net.mm2d.upnp.SingleHttpClient
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.internal.message.FakeSsdpMessage
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

/**
 * Implements for [Device].
 *
 * @author [大前良介(OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class DeviceImpl private constructor(
    override val controlPoint: ControlPointImpl,
    override val parent: Device?,
    private val udnSet: Set<String>,
    ssdpMessage: SsdpMessage,
    location: String,
    override val description: String,
    override val udn: String,
    override val upc: String?,
    override val deviceType: String,
    override val friendlyName: String,
    override val manufacture: String?,
    override val manufactureUrl: String?,
    override val modelName: String,
    override val modelUrl: String?,
    override val modelDescription: String?,
    override val modelNumber: String?,
    override val serialNumber: String?,
    override val presentationUrl: String?,
    private val urlBase: String?,
    private val tagMap: Map<String, Map<String, String>>,
    override val iconList: List<Icon>,
    serviceBuilderList: List<ServiceImpl.Builder>,
    deviceBuilderList: List<Builder>
) : Device {
    override var ssdpMessage: SsdpMessage = ssdpMessage
        private set
    override val expireTime: Long
        get() = ssdpMessage.expireTime
    override val scopeId: Int
        get() = ssdpMessage.scopeId
    override var location: String = location
        private set
    override val baseUrl: String
        get() = urlBase ?: location
    override val ipAddress: String
        get() = try {
            URL(location).host
        } catch (e: MalformedURLException) {
            ""
        }
    override val serviceList: List<Service> = serviceBuilderList.map {
        it.setDevice(this)
        it.build()
    }
    override val isEmbeddedDevice: Boolean = parent != null
    override val deviceList: List<Device> = deviceBuilderList.map {
        it.build(this)
    }
    override val isPinned: Boolean
        get() = ssdpMessage.isPinned

    override fun loadIconBinary(client: SingleHttpClient, filter: IconFilter) {
        if (iconList.isEmpty()) return
        filter(iconList).mapNotNull { it as? IconImpl }.forEach {
            try {
                it.loadBinary(client, baseUrl, scopeId)
            } catch (ignored: IOException) {
            }
        }
    }

    override fun updateSsdpMessage(message: SsdpMessage) {
        if (ssdpMessage.isPinned) return
        require(isEmbeddedDevice || udnSet.contains(message.uuid)) { "uuid and udn does not match! uuid=${message.uuid} udn=$udnSet" }
        location = message.location ?: throw IllegalArgumentException()
        ssdpMessage = message
        deviceList.forEach {
            it.updateSsdpMessage(message)
        }
    }

    override fun getValue(name: String): String? =
        tagMap.values.mapNotNull { it[name] }.firstOrNull()

    override fun getValueWithNamespace(namespace: String, name: String): String? =
        tagMap[namespace]?.get(name)

    override fun findServiceById(id: String): Service? =
        serviceList.find { it.serviceId == id }

    override fun findServiceByType(type: String): Service? =
        serviceList.find { it.serviceType == type }

    override fun findAction(name: String): Action? {
        serviceList.forEach {
            it.findAction(name)?.let { action ->
                return action
            }
        }
        return null
    }

    override fun findDeviceByType(deviceType: String): Device? =
        deviceList.find { it.deviceType == deviceType }

    override fun findDeviceByTypeRecursively(deviceType: String): Device? {
        deviceList.forEach {
            if (deviceType == it.deviceType) {
                return it
            }
            it.findDeviceByTypeRecursively(deviceType)?.let { result ->
                return result
            }
        }
        return null
    }

    override fun hashCode(): Int = udn.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is Device) return false
        return udn == other.udn
    }

    override fun toString(): String = friendlyName

    internal class Builder(
        private val controlPoint: ControlPointImpl,
        private var ssdpMessage: SsdpMessage
    ) {
        private var location: String
        private var description: String? = null
        private var udn: String? = null
        private var upc: String? = null
        private var deviceType: String? = null
        private var friendlyName: String? = null
        private var manufacture: String? = null
        private var manufactureUrl: String? = null
        private var modelName: String? = null
        private var modelUrl: String? = null
        private var modelDescription: String? = null
        private var modelNumber: String? = null
        private var serialNumber: String? = null
        private var presentationUrl: String? = null
        private var urlBase: String? = null
        private val iconList = mutableListOf<Icon>()
        private val serviceBuilderList = mutableListOf<ServiceImpl.Builder>()
        private var deviceBuilderList: List<Builder> = emptyList()
        private val tagMap: MutableMap<String, MutableMap<String, String>>

        init {
            location = ssdpMessage.location ?: throw IllegalArgumentException()
            tagMap = mutableMapOf("" to mutableMapOf())
        }

        fun build(parent: Device? = null): DeviceImpl {
            val description = description
                ?: throw IllegalStateException("description must be set.")
            val deviceType = deviceType
                ?: throw IllegalStateException("deviceType must be set.")
            val friendlyName = friendlyName
                ?: throw IllegalStateException("friendlyName must be set.")
            val manufacture = manufacture
                ?: throw IllegalStateException("manufacturer must be set.")
            val modelName = modelName
                ?: throw IllegalStateException("modelName must be set.")
            val udn = udn
                ?: throw IllegalStateException("UDN must be set.")
            val udnSet = collectUdn()

            if (parent == null) {
                // Sometime Embedded devices have different UUIDs. So, do not check when embedded device
                val ssdpMessage = ssdpMessage
                val uuid = getUuid()
                if (uuid.isEmpty() && ssdpMessage is FakeSsdpMessage) {
                    ssdpMessage.uuid = udn
                } else {
                    check(udnSet.contains(uuid)) { "uuid and udn does not match! uuid=$uuid udn=$udnSet" }
                }
            }
            return DeviceImpl(
                controlPoint = controlPoint,
                parent = parent,
                udnSet = udnSet,
                ssdpMessage = ssdpMessage,
                location = location,
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
                serviceBuilderList = serviceBuilderList,
                deviceBuilderList = deviceBuilderList
            )
        }

        private fun collectUdn(): Set<String> = mutableSetOf<String>().also {
            collectUdn(this, it)
        }

        private fun collectUdn(builder: Builder, outSet: MutableSet<String>) {
            builder.udn?.let { outSet.add(it) }
            builder.deviceBuilderList.forEach { collectUdn(it, outSet) }
        }

        fun getSsdpMessage(): SsdpMessage = ssdpMessage

        fun getLocation(): String = location

        fun getUuid(): String = ssdpMessage.uuid

        fun getBaseUrl(): String = urlBase ?: location

        fun getServiceBuilderList(): List<ServiceImpl.Builder> = serviceBuilderList

        fun createEmbeddedDeviceBuilder(): Builder {
            val builder = Builder(controlPoint, ssdpMessage)
            builder.setDescription(description!!)
            builder.setUrlBase(urlBase)
            return builder
        }

        /**
         * Descriptionのダウンロード完了時にダウンロードに使用した[SingleHttpClient]を渡す。
         *
         * @param client Descriptionのダウンロードに使用した[SingleHttpClient]
         */
        fun setDownloadInfo(client: SingleHttpClient) {
            val ssdpMessage = ssdpMessage as? FakeSsdpMessage ?: return
            client.localAddress?.let {
                ssdpMessage.localAddress = it
            } ?: throw IllegalStateException("HttpClient is not connected yet.")
        }

        internal fun updateSsdpMessage(message: SsdpMessage) {
            if (ssdpMessage.isPinned) return
            location = message.location ?: throw IllegalArgumentException()
            ssdpMessage = message
            deviceBuilderList.forEach {
                it.updateSsdpMessage(message)
            }
        }

        fun setDescription(description: String): Builder = apply {
            this.description = description
        }

        fun setUdn(udn: String): Builder = apply {
            this.udn = udn
        }

        fun setUpc(upc: String): Builder = apply {
            this.upc = upc
        }

        fun setDeviceType(deviceType: String): Builder = apply {
            this.deviceType = deviceType
        }

        fun getDeviceType(): String? = deviceType

        fun setFriendlyName(friendlyName: String): Builder = apply {
            this.friendlyName = friendlyName
        }

        fun setManufacture(manufacture: String): Builder = apply {
            this.manufacture = manufacture
        }

        fun setManufactureUrl(manufactureUrl: String): Builder = apply {
            this.manufactureUrl = manufactureUrl
        }

        fun setModelName(modelName: String): Builder = apply {
            this.modelName = modelName
        }

        fun setModelUrl(modelUrl: String): Builder = apply {
            this.modelUrl = modelUrl
        }

        fun setModelDescription(modelDescription: String): Builder = apply {
            this.modelDescription = modelDescription
        }

        fun setModelNumber(modelNumber: String): Builder = apply {
            this.modelNumber = modelNumber
        }

        fun setSerialNumber(serialNumber: String): Builder = apply {
            this.serialNumber = serialNumber
        }

        fun setPresentationUrl(presentationUrl: String): Builder = apply {
            this.presentationUrl = presentationUrl
        }

        // URLBaseは1.1以降Deprecated
        fun setUrlBase(urlBase: String?): Builder = apply {
            this.urlBase = urlBase
        }

        fun addIcon(icon: Icon): Builder = apply {
            iconList.add(icon)
        }

        fun addServiceBuilder(builder: ServiceImpl.Builder): Builder = apply {
            serviceBuilderList.add(builder)
        }

        fun setEmbeddedDeviceBuilderList(builderList: List<Builder>): Builder = apply {
            deviceBuilderList = builderList
        }

        fun getEmbeddedDeviceBuilderList(): List<Builder> = deviceBuilderList

        // DeviceDescriptionにはAttributeは使用されていないためAttributeには非対応
        fun putTag(namespace: String?, tag: String, value: String): Builder = apply {
            val namespaceUri = namespace ?: ""
            val map = tagMap[namespaceUri] ?: mutableMapOf<String, String>().also {
                tagMap[namespaceUri] = it
            }
            map[tag] = value
        }

        fun toDumpString(): String = buildString {
            append("DeviceBuilder")
            append("\nSSDP:")
            append(ssdpMessage)
            append("\nDESCRIPTION:")
            append(description)
            if (serviceBuilderList.size != 0) {
                append("\n")
                serviceBuilderList.forEach {
                    append(it.toDumpString())
                }
            }
        }
    }
}
