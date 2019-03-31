/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import net.mm2d.upnp.ControlPoint.DiscoveryListener
import net.mm2d.upnp.ControlPoint.NotifyEventListener

object Adapter {
    @JvmStatic
    fun adapter(
        handler: (Runnable) -> Boolean
    ): TaskExecutor = object : TaskExecutor {
        override fun execute(task: Runnable): Boolean = handler(task)
    }

    @JvmStatic
    fun adapter(
        discover: (Device) -> Unit,
        lost: (Device) -> Unit
    ): DiscoveryListener = object : DiscoveryListener {
        override fun onDiscover(device: Device) {
            discover(device)
        }

        override fun onLost(device: Device) {
            lost(device)
        }
    }

    @JvmStatic
    fun adapter(
        notifyEvent: (service: Service, seq: Long, variable: String, value: String) -> Unit
    ): NotifyEventListener = object : NotifyEventListener {
        override fun onNotifyEvent(service: Service, seq: Long, variable: String, value: String) {
            notifyEvent(service, seq, variable, value)
        }
    }
}
