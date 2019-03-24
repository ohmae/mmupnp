/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty

import net.mm2d.upnp.ControlPoint
import net.mm2d.upnp.Device
import net.mm2d.upnp.Icon
import net.mm2d.upnp.SsdpMessage

object EmptyControlPoint : ControlPoint {
    override val deviceListSize: Int = 0
    override val deviceList: List<Device> = emptyList()
    override fun initialize() {}
    override fun terminate() {}
    override fun start() {}
    override fun stop() {}
    override fun clearDeviceList() {}
    override fun search() {}
    override fun search(st: String?) {}
    override fun setSsdpMessageFilter(filter: ((SsdpMessage) -> Boolean)?) {}
    override fun setIconFilter(filter: ((List<Icon>) -> List<Icon>)?) {}
    override fun addDiscoveryListener(listener: ControlPoint.DiscoveryListener) {}
    override fun removeDiscoveryListener(listener: ControlPoint.DiscoveryListener) {}
    override fun addNotifyEventListener(listener: ControlPoint.NotifyEventListener) {}
    override fun removeNotifyEventListener(listener: ControlPoint.NotifyEventListener) {}
    override fun getDevice(udn: String): Device? = null
    override fun tryAddDevice(uuid: String, location: String) {}
    override fun tryAddPinnedDevice(location: String) {}
    override fun removePinnedDevice(location: String) {}
}
