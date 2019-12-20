/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.impl

import net.mm2d.upnp.common.HttpClient
import net.mm2d.upnp.common.SsdpMessage
import net.mm2d.upnp.common.internal.property.DeviceProperty
import net.mm2d.upnp.cp.*
import net.mm2d.upnp.cp.internal.message.FakeSsdpMessage
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
    private val property: DeviceProperty,
    private val udnSet: Set<String>,
    ssdpMessage: SsdpMessage,
    location: String
) : Device {
    override var ssdpMessage: SsdpMessage = ssdpMessage
        private set
    override val expireTime: Long = ssdpMessage.expireTime
    override val scopeId: Int = ssdpMessage.scopeId
    override var location: String = location
        private set
    override val baseUrl: String
        get() = property.urlBase ?: location
    override val ipAddress: String
        get() = try {
            URL(location).host
        } catch (e: MalformedURLException) {
            ""
        }
    override val description: String = property.description
    override val udn: String = property.udn
    override val upc: String? = property.upc
    override val deviceType: String = property.deviceType
    override val friendlyName: String = property.friendlyName
    override val manufacture: String? = property.manufacture
    override val manufactureUrl: String? = property.manufactureUrl
    override val modelName: String = property.modelName
    override val modelUrl: String? = property.modelUrl
    override val modelDescription: String? = property.modelDescription
    override val modelNumber: String? = property.modelNumber
    override val serialNumber: String? = property.serialNumber
    override val presentationUrl: String? = property.presentationUrl
    override val iconList: List<Icon> = property.iconList.map { IconImpl(it) }
    override val serviceList: List<Service> = property.serviceList.map {
        ServiceImpl(this, it)
    }
    override val isEmbeddedDevice: Boolean = parent != null
    override val deviceList: List<Device> = property.deviceList.map {
        DeviceImpl(
            controlPoint = controlPoint,
            parent = this,
            property = it,
            udnSet = collectUdn(it),
            ssdpMessage = ssdpMessage,
            location = location
        )
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
        require(isEmbeddedDevice || udnSet.contains(message.uuid)) { "uuid and udn does not match! uuid=${message.uuid} udn=$udnSet" }
        location = message.location ?: throw IllegalArgumentException()
        ssdpMessage = message
        deviceList.forEach {
            it.updateSsdpMessage(message)
        }
    }

    override fun getValue(name: String): String? = property.getValue(name)

    override fun getValueWithNamespace(namespace: String, name: String): String? =
        property.getValueWithNamespace(namespace, name)

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

    companion object {
        private fun collectUdn(property: DeviceProperty): Set<String> = mutableSetOf<String>().also {
            collectUdn(property, it)
        }

        private fun collectUdn(property: DeviceProperty, outSet: MutableSet<String>) {
            outSet.add(property.udn)
            property.deviceList.forEach {
                collectUdn(it, outSet)
            }
        }
    }

    internal class Builder(
        private val controlPoint: ControlPointImpl,
        private var ssdpMessage: SsdpMessage
    ) {
        private var location: String
        val propertyBuilder: DeviceProperty.Builder = DeviceProperty.Builder()

        init {
            location = ssdpMessage.location ?: throw IllegalArgumentException()
        }

        fun build(parent: Device? = null): DeviceImpl {
            val property = propertyBuilder.build(null)
            val udnSet = collectUdn(property)

            if (parent == null) {
                // Sometime Embedded devices have different UUIDs. So, do not check when embedded device
                val ssdpMessage = ssdpMessage
                val uuid = getUuid()
                if (uuid.isEmpty() && ssdpMessage is FakeSsdpMessage) {
                    ssdpMessage.uuid = property.udn
                } else {
                    check(udnSet.contains(uuid)) { "uuid and udn does not match! uuid=$uuid udn=$udnSet" }
                }
            }
            return DeviceImpl(
                controlPoint = controlPoint,
                parent = parent,
                property = property,
                udnSet = udnSet,
                ssdpMessage = ssdpMessage,
                location = location
            )
        }

        fun getSsdpMessage(): SsdpMessage = ssdpMessage

        fun getLocation(): String = location

        fun getUuid(): String = ssdpMessage.uuid

        fun getBaseUrl(): String = propertyBuilder.urlBase ?: location

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
        }

        fun toDumpString(): String = buildString {
            append("DeviceBuilder")
        }
    }
}
