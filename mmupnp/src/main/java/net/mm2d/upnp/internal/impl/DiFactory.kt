/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.upnp.ControlPoint.NotifyEventListener
import net.mm2d.upnp.Device
import net.mm2d.upnp.Protocol
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.TaskExecutor
import net.mm2d.upnp.internal.manager.*
import net.mm2d.upnp.internal.server.EventReceiver
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
        taskExecutors: TaskExecutors,
        listeners: Set<NotifyEventListener>
    ): SubscribeManager = if (subscriptionEnabled) {
        SubscribeManagerImpl(taskExecutors, listeners, this)
    } else {
        EmptySubscribeManager()
    }

    fun createSubscribeServiceHolder(
        taskExecutors: TaskExecutors
    ): SubscribeServiceHolder = SubscribeServiceHolder(taskExecutors)

    fun createEventReceiver(
        taskExecutors: TaskExecutors,
        listener: (sid: String, seq: Long, properties: List<Pair<String, String>>) -> Boolean
    ): EventReceiver = EventReceiver(taskExecutors, listener)

    fun createTaskExecutors(): TaskExecutors = TaskExecutors(callbackExecutor)
}
