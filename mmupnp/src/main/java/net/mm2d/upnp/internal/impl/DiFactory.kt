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
import net.mm2d.upnp.internal.manager.DeviceHolder
import net.mm2d.upnp.internal.manager.SubscribeHolder
import net.mm2d.upnp.internal.manager.SubscribeManager
import net.mm2d.upnp.internal.server.EventReceiver
import net.mm2d.upnp.internal.server.SsdpNotifyReceiverList
import net.mm2d.upnp.internal.server.SsdpSearchServerList
import net.mm2d.upnp.internal.thread.TaskExecutors
import java.net.NetworkInterface

/**
 * ControlPointのテストを容易にするためのDependency injection
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

    fun createSsdpNotifyReceiverList(
        taskExecutors: TaskExecutors,
        interfaces: Iterable<NetworkInterface>,
        listener: (SsdpMessage) -> Unit
    ): SsdpNotifyReceiverList = SsdpNotifyReceiverList(taskExecutors, protocol, interfaces, listener)

    fun createSubscribeManager(
        taskExecutors: TaskExecutors,
        listeners: Set<NotifyEventListener>
    ): SubscribeManager = SubscribeManager(taskExecutors, listeners, this)

    fun createSubscribeHolder(
        taskExecutors: TaskExecutors
    ): SubscribeHolder = SubscribeHolder(taskExecutors)

    fun createEventReceiver(
        taskExecutors: TaskExecutors,
        listener: (sid: String, seq: Long, properties: List<Pair<String, String>>) -> Boolean
    ): EventReceiver = EventReceiver(taskExecutors, listener)

    fun createTaskExecutors(): TaskExecutors = TaskExecutors(callbackExecutor)
}
