/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.upnp.*
import net.mm2d.upnp.internal.manager.SubscribeManager
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
    private val subscribeManager: SubscribeManager,
    override val parent: Device?,
    private var _ssdpMessage: SsdpMessage,
    private var _location: String,
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
    private val serviceBuilderList: List<ServiceImpl.Builder>,
    private val deviceBuilderList: List<DeviceImpl.Builder>
) : Device {
    override val ssdpMessage: SsdpMessage
        get() = _ssdpMessage
    override val expireTime: Long = ssdpMessage.expireTime
    override val scopeId: Int = ssdpMessage.scopeId
    override val location: String
        get() = _location
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
        it.setSubscribeManager(subscribeManager)
        it.build()
    }
    override val isEmbeddedDevice: Boolean = parent != null
    override val deviceList: List<Device> = deviceBuilderList.map {
        it.build(this)
    }
    override val isPinned: Boolean
        get() = ssdpMessage.isPinned

    override fun loadIconBinary(client: HttpClient, filter: IconFilter) {
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
        if (!isEmbeddedDevice && udn != message.uuid) {
            throw IllegalArgumentException("uuid and udn does not match! uuid=${message.uuid} udn=$udn")
        }
        _location = message.location ?: throw IllegalArgumentException()
        _ssdpMessage = message
        deviceList.forEach {
            it.updateSsdpMessage(message)
        }
    }

    override fun getValue(name: String): String? {
        tagMap.values.forEach {
            it[name]?.let { value ->
                return value
            }
        }
        return null
    }

    override fun getValueWithNamespace(namespace: String, name: String): String? {
        val map = tagMap[namespace] ?: return null
        return map[name]
    }

    override fun findServiceById(id: String): Service? {
        return serviceList.find { it.serviceId == id }
    }

    override fun findServiceByType(type: String): Service? {
        return serviceList.find { it.serviceType == type }
    }

    override fun findAction(name: String): Action? {
        serviceList.forEach {
            it.findAction(name)?.let { action ->
                return action
            }
        }
        return null
    }

    override fun findDeviceByType(deviceType: String): Device? {
        return deviceList.find { it.deviceType == deviceType }
    }

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

    override fun hashCode(): Int {
        return udn.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is Device) return false
        return udn == other.udn
    }

    override fun toString(): String {
        return friendlyName
    }

    internal class Builder(
        private val controlPoint: ControlPointImpl,
        private val subscribeManager: SubscribeManager,
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
        @Volatile
        private var deviceBuilderList: List<DeviceImpl.Builder> = emptyList()
        private val tagMap: MutableMap<String, MutableMap<String, String>>

        init {
            location = ssdpMessage.location ?: throw IllegalArgumentException()
            tagMap = mutableMapOf("" to mutableMapOf())
        }

        fun build(parent: Device? = null): DeviceImpl {
            val description = description ?: throw IllegalStateException("description must be set.")
            val deviceType = deviceType ?: throw IllegalStateException("deviceType must be set.")
            val friendlyName = friendlyName ?: throw IllegalStateException("friendlyName must be set.")
            val manufacture = manufacture ?: throw IllegalStateException("manufacturer must be set.")
            val modelName = modelName ?: throw IllegalStateException("modelName must be set.")
            val udn = udn ?: throw IllegalStateException("UDN must be set.")

            if (parent == null) {
                // Sometime Embedded devices have different UUIDs. So, do not check when embedded device
                val ssdpMessage = ssdpMessage
                val uuid = getUuid()
                if (uuid.isEmpty() && ssdpMessage is FakeSsdpMessage) {
                    ssdpMessage.uuid = udn
                } else if (udn != uuid) {
                    throw IllegalStateException("uuid and udn does not match! uuid=$uuid udn=$udn")
                }
            }
            return DeviceImpl(
                controlPoint = controlPoint,
                subscribeManager = subscribeManager,
                parent = parent,
                _ssdpMessage = ssdpMessage,
                _location = location,
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

        fun getSsdpMessage(): SsdpMessage = ssdpMessage

        fun getLocation(): String = location

        fun getUuid(): String = ssdpMessage.uuid

        fun getBaseUrl(): String = urlBase ?: location

        fun getServiceBuilderList(): List<ServiceImpl.Builder> = serviceBuilderList

        fun createEmbeddedDeviceBuilder(): Builder {
            val builder = Builder(controlPoint, subscribeManager, ssdpMessage)
            builder.setDescription(description!!)
            builder.setUrlBase(urlBase)
            return builder
        }

        /**
         * Descriptionのダウンロード完了時にダウンロードに使用した[HttpClient]を渡す。
         *
         * @param client Descriptionのダウンロードに使用した[HttpClient]
         */
        fun setDownloadInfo(client: HttpClient) {
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

        fun setDescription(description: String): Builder {
            this.description = description
            return this
        }

        fun setUdn(udn: String): Builder {
            this.udn = udn
            return this
        }

        fun setUpc(upc: String): Builder {
            this.upc = upc
            return this
        }

        fun setDeviceType(deviceType: String): Builder {
            this.deviceType = deviceType
            return this
        }

        fun setFriendlyName(friendlyName: String): Builder {
            this.friendlyName = friendlyName
            return this
        }

        fun setManufacture(manufacture: String): Builder {
            this.manufacture = manufacture
            return this
        }

        fun setManufactureUrl(manufactureUrl: String): Builder {
            this.manufactureUrl = manufactureUrl
            return this
        }

        fun setModelName(modelName: String): Builder {
            this.modelName = modelName
            return this
        }

        fun setModelUrl(modelUrl: String): Builder {
            this.modelUrl = modelUrl
            return this
        }

        fun setModelDescription(modelDescription: String): Builder {
            this.modelDescription = modelDescription
            return this
        }

        fun setModelNumber(modelNumber: String): Builder {
            this.modelNumber = modelNumber
            return this
        }

        fun setSerialNumber(serialNumber: String): Builder {
            this.serialNumber = serialNumber
            return this
        }

        fun setPresentationUrl(presentationUrl: String): Builder {
            this.presentationUrl = presentationUrl
            return this
        }

        // URLBaseは1.1以降Deprecated
        fun setUrlBase(urlBase: String?): Builder {
            this.urlBase = urlBase
            return this
        }

        fun addIcon(icon: Icon): Builder {
            iconList.add(icon)
            return this
        }

        fun addServiceBuilder(builder: ServiceImpl.Builder): Builder {
            serviceBuilderList.add(builder)
            return this
        }

        fun setEmbeddedDeviceBuilderList(builderList: List<Builder>): Builder {
            deviceBuilderList = builderList
            return this
        }

        fun getEmbeddedDeviceBuilderList(): List<DeviceImpl.Builder> {
            return deviceBuilderList
        }

        // DeviceDescriptionにはAttributeは使用されていないためAttributeには非対応
        fun putTag(namespace: String?, tag: String, value: String): Builder {
            val namespaceUri = namespace ?: ""
            val map = tagMap[namespaceUri] ?: mutableMapOf<String, String>().also {
                tagMap[namespaceUri] = it
            }
            map[tag] = value
            return this
        }

        fun toDumpString(): String {
            val sb = StringBuilder()
                .append("DeviceBuilder")
                .append("\nSSDP:").append(ssdpMessage)
                .append("\nDESCRIPTION:").append(description)
            if (serviceBuilderList.size != 0) {
                sb.append("\n")
                serviceBuilderList.forEach {
                    sb.append(it.toDumpString())
                }
            }
            return sb.toString()
        }
    }
}
