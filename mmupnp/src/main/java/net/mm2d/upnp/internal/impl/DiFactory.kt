/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.upnp.ControlPointConfig
import net.mm2d.upnp.Device
import net.mm2d.upnp.Protocol
import net.mm2d.upnp.Service
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.TaskExecutor
import net.mm2d.upnp.internal.manager.DeviceHolder
import net.mm2d.upnp.internal.manager.EmptySubscribeManager
import net.mm2d.upnp.internal.manager.SubscribeManager
import net.mm2d.upnp.internal.manager.SubscribeManagerImpl
import net.mm2d.upnp.internal.manager.SubscribeServiceHolder
import net.mm2d.upnp.internal.server.EventReceiver
import net.mm2d.upnp.internal.server.MulticastEventReceiverList
import net.mm2d.upnp.internal.server.SsdpNotifyServerList
import net.mm2d.upnp.internal.server.SsdpSearchServerList
import net.mm2d.upnp.internal.thread.TaskExecutors
import java.net.NetworkInterface

/**
 * Dependency injection for ControlPoint
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class DiFactory(
    private val protocol: Protocol = Protocol.DEFAULT,
    private val callbackExecutor: TaskExecutor? = null
) {
    fun createLoadingDeviceMap(): MutableMap<String, DeviceImpl.Builder> = mutableMapOf()

    fun createDeviceHolder(
        taskExecutors: TaskExecutors,
        listener: (device: Device) -> Unit
    ): DeviceHolder = DeviceHolder(taskExecutors, listener)

    fun createSsdpSearchServerList(
        taskExecutors: TaskExecutors,
        interfaces: Iterable<NetworkInterface>,
        listener: (SsdpMessage) -> Unit
    ): SsdpSearchServerList = SsdpSearchServerList(taskExecutors, protocol, interfaces, listener)

    fun createSsdpNotifyServerList(
        taskExecutors: TaskExecutors,
        interfaces: Iterable<NetworkInterface>,
        listener: (SsdpMessage) -> Unit
    ): SsdpNotifyServerList = SsdpNotifyServerList(taskExecutors, protocol, interfaces, listener)

    fun createSubscribeManager(
        subscriptionEnabled: Boolean,
        config: ControlPointConfig,
        listener: (service: Service, seq: Long, properties: List<Pair<String, String>>) -> Unit
    ): SubscribeManager = if (subscriptionEnabled) {
        SubscribeManagerImpl(config, listener, this)
    } else {
        EmptySubscribeManager()
    }

    fun createSubscribeServiceHolder(
        config: ControlPointConfig
    ): SubscribeServiceHolder = SubscribeServiceHolder(config)

    fun createEventReceiver(
        config: ControlPointConfig,
        listener: suspend (sid: String, seq: Long, properties: List<Pair<String, String>>) -> Boolean
    ): EventReceiver = EventReceiver(config, listener)

    fun createMulticastEventReceiverList(
        taskExecutors: TaskExecutors,
        interfaces: Iterable<NetworkInterface>,
        listener: (uuid: String, svcid: String, lvl: String, seq: Long, properties: List<Pair<String, String>>) -> Unit
    ): MulticastEventReceiverList = MulticastEventReceiverList(taskExecutors, protocol, interfaces, listener)

    fun createTaskExecutors(): TaskExecutors = TaskExecutors(callbackExecutor)
}
