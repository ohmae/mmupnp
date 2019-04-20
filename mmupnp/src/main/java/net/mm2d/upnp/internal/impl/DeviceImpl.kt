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
 * Deviceの実装。
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

    /**
     * DeviceのBuilder。
     *
     * XMLファイルの読み込み処理もBuilderに対して行う。
     * @param controlPoint     ControlPoint
     * @param subscribeManager 購読状態マネージャ
     * @param ssdpMessage      SSDPパケット
     */
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

        /**
         * Deviceのインスタンスを作成する。
         *
         * @param parent 親Device、EmbeddedDeviceの場合に指定
         * @return Deviceのインスタンス
         */
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

        /**
         * 最新のSSDPパケットを返す。
         *
         * @return 最新のSSDPパケット
         */
        fun getSsdpMessage(): SsdpMessage = ssdpMessage

        /**
         * SSDPに記述されたLocationの値を返す。
         *
         * @return SSDPに記述されたLocationの値
         */
        fun getLocation(): String = location

        /**
         * SSDPに記述されたUUIDを返す。
         *
         * @return SSDPに記述されたUUID
         */
        fun getUuid(): String = ssdpMessage.uuid

        /**
         * URLのベースとして使用する値を返す。
         *
         * URLBaseの値が存在する場合はURLBase、存在しない場合はLocationの値を利用する。
         *
         * @return URLのベースとして使用する値
         */
        fun getBaseUrl(): String = urlBase ?: location

        /**
         * ServiceのBuilderのリストを返す。
         *
         * @return ServiceのBuilderのリスト
         */
        fun getServiceBuilderList(): List<ServiceImpl.Builder> = serviceBuilderList

        /**
         * EmbeddedDevice用のBuilderを作成する。
         *
         * @return EmbeddedDevice用のBuilder
         */
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

        /**
         * SSDPパケットを登録する。
         *
         * @param message SSDPパケット
         */
        internal fun updateSsdpMessage(message: SsdpMessage) {
            if (ssdpMessage.isPinned) return
            location = message.location ?: throw IllegalArgumentException()
            ssdpMessage = message
            deviceBuilderList.forEach {
                it.updateSsdpMessage(message)
            }
        }

        /**
         * パース前のDescriptionXMLを登録する。
         *
         * @param description DescriptionXML
         */
        fun setDescription(description: String): Builder {
            this.description = description
            return this
        }

        /**
         * UDNの値を登録する。
         *
         * @param udn UDN
         */
        fun setUdn(udn: String): Builder {
            this.udn = udn
            return this
        }

        /**
         * UPCの値を登録する。
         *
         * @param upc UPC
         */
        fun setUpc(upc: String): Builder {
            this.upc = upc
            return this
        }

        /**
         * DeviceTypeの値を登録する。
         *
         * @param deviceType DeviceType
         */
        fun setDeviceType(deviceType: String): Builder {
            this.deviceType = deviceType
            return this
        }

        /**
         * FriendlyNameの値を登録する。
         *
         * @param friendlyName FriendlyName
         */
        fun setFriendlyName(friendlyName: String): Builder {
            this.friendlyName = friendlyName
            return this
        }

        /**
         * Manufactureの値を登録する。
         *
         * @param manufacture Manufacture
         */
        fun setManufacture(manufacture: String): Builder {
            this.manufacture = manufacture
            return this
        }

        /**
         * ManufactureUrlの値を登録する。
         *
         * @param manufactureUrl ManufactureUrl
         */
        fun setManufactureUrl(manufactureUrl: String): Builder {
            this.manufactureUrl = manufactureUrl
            return this
        }

        /**
         * ModelNameの値を登録する。
         *
         * @param modelName ModelName
         */
        fun setModelName(modelName: String): Builder {
            this.modelName = modelName
            return this
        }

        /**
         * ModelUrlの値を登録する。
         *
         * @param modelUrl ModelUrl
         */
        fun setModelUrl(modelUrl: String): Builder {
            this.modelUrl = modelUrl
            return this
        }

        /**
         * ModelDescriptionの値を登録する。
         *
         * @param modelDescription ModelDescription
         */
        fun setModelDescription(modelDescription: String): Builder {
            this.modelDescription = modelDescription
            return this
        }

        /**
         * ModelNumberの値を登録する。
         *
         * @param modelNumber ModelNumber
         */
        fun setModelNumber(modelNumber: String): Builder {
            this.modelNumber = modelNumber
            return this
        }

        /**
         * SerialNumberの値を登録する。
         *
         * @param serialNumber SerialNumber
         */
        fun setSerialNumber(serialNumber: String): Builder {
            this.serialNumber = serialNumber
            return this
        }

        /**
         * PresentationUrlの値を登録する。
         *
         * @param presentationUrl PresentationUrl
         * @return Builder
         */
        fun setPresentationUrl(presentationUrl: String): Builder {
            this.presentationUrl = presentationUrl
            return this
        }

        /**
         * URLBaseの値を登録する。
         *
         * URLBaseは1.1以降Deprecated
         *
         * @param urlBase URLBaseの値
         * @return Builder
         */
        fun setUrlBase(urlBase: String?): Builder {
            this.urlBase = urlBase
            return this
        }

        /**
         * IconのBuilderを登録する。
         *
         * @param icon Icon
         */
        fun addIcon(icon: Icon): Builder {
            iconList.add(icon)
            return this
        }

        /**
         * ServiceのBuilderを登録する。
         *
         * @param builder ServiceのBuilder
         */
        fun addServiceBuilder(builder: ServiceImpl.Builder): Builder {
            serviceBuilderList.add(builder)
            return this
        }

        /**
         * Embedded DeviceのBuilderを登録する。
         *
         * @param builderList Embedded DeviceのBuilderリスト
         */
        fun setEmbeddedDeviceBuilderList(builderList: List<Builder>): Builder {
            deviceBuilderList = builderList
            return this
        }

        /**
         * Embedded DeviceのBuilderのリストを返す。
         *
         * @return Embedded DeviceのBuilderのリスト
         */
        fun getEmbeddedDeviceBuilderList(): List<DeviceImpl.Builder> {
            return deviceBuilderList
        }

        /**
         * XMLタグの情報を登録する。
         *
         * DeviceDescriptionにはAttributeは使用されていないため
         * Attributeには非対応
         *
         * @param namespace namespace uri
         * @param tag       タグ名
         * @param value     タグの値
         */
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
