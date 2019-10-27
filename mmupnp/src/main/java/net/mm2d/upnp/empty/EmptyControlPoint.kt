/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty

import net.mm2d.upnp.ControlPoint
import net.mm2d.upnp.ControlPoint.*
import net.mm2d.upnp.Device
import net.mm2d.upnp.IconFilter
import net.mm2d.upnp.common.SsdpMessage

/**
 * Empty implementation of [ControlPoint].
 */
object EmptyControlPoint : ControlPoint {
    override val deviceListSize: Int = 0
    override val deviceList: List<Device> = emptyList()
    override fun initialize() = Unit
    override fun terminate() = Unit
    override fun start() = Unit
    override fun stop() = Unit
    override fun clearDeviceList() = Unit
    override fun search(st: String?) = Unit
    override fun setSsdpMessageFilter(predicate: ((SsdpMessage) -> Boolean)?) = Unit
    override fun setIconFilter(filter: IconFilter?) = Unit
    override fun addDiscoveryListener(listener: DiscoveryListener) = Unit
    override fun removeDiscoveryListener(listener: DiscoveryListener) = Unit
    @Suppress("DEPRECATION")
    override fun addNotifyEventListener(listener: NotifyEventListener) = Unit
    @Suppress("DEPRECATION")
    override fun removeNotifyEventListener(listener: NotifyEventListener) = Unit
    override fun addEventListener(listener: EventListener) = Unit
    override fun removeEventListener(listener: EventListener) = Unit
    override fun addMulticastEventListener(listener: MulticastEventListener) = Unit
    override fun removeMulticastEventListener(listener: MulticastEventListener) = Unit
    override fun getDevice(udn: String): Device? = null
    override fun tryAddDevice(uuid: String, location: String) = Unit
    override fun tryAddPinnedDevice(location: String) = Unit
    override fun removePinnedDevice(location: String) = Unit
}
