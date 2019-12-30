/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da.internal.impl

import net.mm2d.upnp.common.internal.property.DeviceProperty
import net.mm2d.upnp.common.util.XmlUtils
import net.mm2d.upnp.common.util.append
import net.mm2d.upnp.common.util.appendWithNs
import net.mm2d.upnp.common.util.toXml
import net.mm2d.upnp.da.DaAction
import net.mm2d.upnp.da.DaDevice
import net.mm2d.upnp.da.DaService
import org.w3c.dom.Element

class DaDeviceImpl(
    private val property: DeviceProperty,
    override val iconList: List<DaIconImpl>,
    override val serviceList: List<DaServiceImpl>,
    override val deviceList: List<DaDeviceImpl>
) : DaDevice, XmlAppendable {
    init {
        serviceList.forEach { it.device = this }
        deviceList.forEach { it.parent = this }
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
    override val isEmbeddedDevice: Boolean
        get() = parent != null
    override var parent: DaDevice? = null
    override fun getValue(name: String): String? =
        property.getValue(name)

    override fun getValueWithNamespace(namespace: String, name: String): String? =
        property.getValueWithNamespace(namespace, name)

    override fun findServiceById(id: String): DaService? =
        serviceList.find { it.serviceId == id }

    override fun findServiceByType(type: String): DaService? =
        serviceList.find { it.serviceType == type }

    override fun findAction(name: String): DaAction? =
        serviceList.asSequence()
            .mapNotNull { it.findAction(name) }
            .firstOrNull()

    override fun findDeviceByType(deviceType: String): DaDevice? =
        deviceList.find { it.deviceType == deviceType }

    override fun findDeviceByTypeRecursively(deviceType: String): DaDevice? =
        deviceList.asSequence()
            .filter { it.deviceType == deviceType }
            .firstOrNull()
            ?: deviceList.asSequence()
                .mapNotNull { it.findDeviceByTypeRecursively(deviceType) }
                .firstOrNull()

    fun createDescription(): String =
        XmlUtils.newDocument(true).also {
            it.append("root") {
                setAttribute("xmlns", "urn:schemas-upnp-org:device-1-0")
                append("specVersion") {
                    append("major", "1")
                    append("minor", "0")
                }
                appendTo(this)
            }
        }.toXml()

    override fun appendTo(parent: Element) {
        parent.append("device") {
            property.tagMap.forEach { namespaceEntry ->
                namespaceEntry.value.forEach {
                    appendWithNs(namespaceEntry.key, it.key, it.value)
                }
            }
            append(iconList, "iconList")
            append(serviceList, "serviceList")
            append(deviceList, "deviceList")
        }
    }

    companion object {
        fun create(property: DeviceProperty): DaDeviceImpl =
            DaDeviceImpl(
                property,
                property.iconList.map { DaIconImpl.create(it) },
                property.serviceList.map { DaServiceImpl.create(it) },
                property.deviceList.map { create(it) }
            )
    }
}
