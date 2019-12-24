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
import net.mm2d.upnp.cp.Action
import net.mm2d.upnp.cp.Device
import net.mm2d.upnp.cp.IconFilter
import net.mm2d.upnp.cp.Service
import net.mm2d.upnp.cp.internal.message.FakeSsdpMessage
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

/**
 * Implements for [Device].
 *
 * @author [大前良介(OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class DeviceImpl(
    override val controlPoint: ControlPointImpl,
    private val property: DeviceProperty,
    private val udnSet: Set<String>,
    ssdpMessage: SsdpMessage,
    location: String,
    override val iconList: List<IconImpl>,
    override val serviceList: List<ServiceImpl>,
    override val deviceList: List<DeviceImpl>
) : Device {
    init {
        serviceList.forEach { it.device = this }
        deviceList.forEach { it.parent = this }
    }

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
    override var parent: Device? = null
        private set
    override val isEmbeddedDevice: Boolean
        get() = parent != null

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

    override fun findAction(name: String): Action? =
        serviceList.asSequence()
            .mapNotNull { it.findAction(name) }
            .firstOrNull()

    override fun findDeviceByType(deviceType: String): Device? =
        deviceList.find { it.deviceType == deviceType }

    override fun findDeviceByTypeRecursively(deviceType: String): Device? =
        deviceList.asSequence().filter { it.deviceType == deviceType }.firstOrNull()
            ?: deviceList.asSequence().mapNotNull { it.findDeviceByTypeRecursively(deviceType) }.firstOrNull()

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

        fun build(): DeviceImpl {
            val property = propertyBuilder.build(null)
            val udnSet = collectUdn(property)
            // Sometime Embedded devices have different UUIDs. So, do not check when embedded device
            val ssdpMessage = ssdpMessage
            val uuid = getUuid()
            if (uuid.isEmpty() && ssdpMessage is FakeSsdpMessage) {
                ssdpMessage.uuid = property.udn
            } else {
                check(udnSet.contains(uuid)) { "uuid and udn does not match! uuid=$uuid udn=$udnSet" }
            }
            return build(property, udnSet)
        }

        private fun build(property: DeviceProperty, udnSet: Set<String>): DeviceImpl {
            val iconList = property.iconList.map { IconImpl(it) }.toList()
            val serviceList = property.serviceList.map {
                ServiceImpl.create(controlPoint, it)
            }.toList()
            val deviceList: List<DeviceImpl> = property.deviceList.map {
                build(it, udnSet)
            }.toList()
            return DeviceImpl(
                controlPoint = controlPoint,
                property = property,
                udnSet = udnSet,
                ssdpMessage = ssdpMessage,
                location = location,
                iconList = iconList,
                serviceList = serviceList,
                deviceList = deviceList
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
            append("\nSSDP:")
            append(ssdpMessage)
            append("\nDESCRIPTION:")
            append(propertyBuilder.description)
            if (propertyBuilder.serviceBuilderList.isNotEmpty()) {
                append("\n")
                propertyBuilder.serviceBuilderList.forEach {
                    append(it.description)
                }
            }
        }
    }
}
