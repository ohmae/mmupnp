/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty

import net.mm2d.upnp.Action
import net.mm2d.upnp.ControlPoint
import net.mm2d.upnp.ControlPoints
import net.mm2d.upnp.Device
import net.mm2d.upnp.Icon
import net.mm2d.upnp.IconFilter
import net.mm2d.upnp.Service
import net.mm2d.upnp.SingleHttpClient
import net.mm2d.upnp.SsdpMessage

/**
 * Empty implementation of [Device].
 */
object EmptyDevice : Device {
    override val controlPoint: ControlPoint
        get() = ControlPoints.emptyControlPoint()
    override val ssdpMessage: SsdpMessage
        get() = ControlPoints.emptySsdpMessage()
    override val expireTime: Long
        get() = 0L
    override val description: String
        get() = ""
    override val scopeId: Int
        get() = 0
    override val location: String
        get() = ""
    override val baseUrl: String
        get() = ""
    override val ipAddress: String
        get() = ""
    override val udn: String
        get() = ""
    override val upc: String?
        get() = null
    override val deviceType: String
        get() = ""
    override val friendlyName: String
        get() = ""
    override val manufacture: String?
        get() = null
    override val manufactureUrl: String?
        get() = null
    override val modelName: String
        get() = ""
    override val modelUrl: String?
        get() = null
    override val modelDescription: String?
        get() = null
    override val modelNumber: String?
        get() = null
    override val serialNumber: String?
        get() = null
    override val presentationUrl: String?
        get() = null
    override val iconList: List<Icon>
        get() = emptyList()
    override val serviceList: List<Service>
        get() = emptyList()
    override val isEmbeddedDevice: Boolean
        get() = false
    override val parent: Device?
        get() = null
    override val deviceList: List<Device>
        get() = emptyList()
    override val isPinned: Boolean
        get() = false

    override fun loadIconBinary(client: SingleHttpClient, filter: IconFilter) = Unit
    override fun updateSsdpMessage(message: SsdpMessage) = Unit
    override fun getValue(name: String): String? = null
    override fun getValueWithNamespace(namespace: String, name: String): String? = null
    override fun findServiceById(id: String): Service? = null
    override fun findServiceByType(type: String): Service? = null
    override fun findAction(name: String): Action? = null
    override fun findDeviceByType(deviceType: String): Device? = null
    override fun findDeviceByTypeRecursively(deviceType: String): Device? = null
}
