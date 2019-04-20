/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import net.mm2d.upnp.ControlPoint.DiscoveryListener
import net.mm2d.upnp.ControlPoint.NotifyEventListener

/**
 * Adapter to convert lambda to interface.
 */
object Adapter {

    /**
     * Adapter for [TaskExecutor].
     *
     * @param handler execution callback
     * @return TaskExecutor
     */
    @JvmStatic
    fun taskExecutor(
        handler: (Runnable) -> Boolean
    ): TaskExecutor = object : TaskExecutor {
        override fun execute(task: Runnable): Boolean = handler(task)
    }

    /**
     * Adapter for [DiscoveryListener].
     *
     * @param discover onDiscover callback
     * @param lost     onLost callback
     * @return DiscoveryListener
     */
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

    /**
     * Adapter for [NotifyEventListener].
     *
     * @param notifyEvent onNotifyEvent callback.
     * @return NotifyEventListener
     */
    @JvmStatic
    fun adapter(
        notifyEvent: (service: Service, seq: Long, variable: String, value: String) -> Unit
    ): NotifyEventListener = object : NotifyEventListener {
        override fun onNotifyEvent(service: Service, seq: Long, variable: String, value: String) {
            notifyEvent(service, seq, variable, value)
        }
    }

    /**
     * Adapter for [IconFilter].
     *
     * @param filter filter method
     * @return IconFilter
     */
    @JvmStatic
    fun iconFilter(
        filter: (List<Icon>) -> List<Icon>
    ): IconFilter = object : IconFilter {
        override fun invoke(list: List<Icon>): List<Icon> = filter.invoke(list)
    }
}
