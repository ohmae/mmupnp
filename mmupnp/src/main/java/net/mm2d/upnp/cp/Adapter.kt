/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp

import net.mm2d.upnp.common.TaskExecutor
import net.mm2d.upnp.cp.ControlPoint.*

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
     * @param lost onLost callback
     * @return DiscoveryListener
     */
    @JvmStatic
    fun discoveryListener(
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
     * Adapter for [EventListener].
     *
     * @param onEvent callback
     * @return EventListener
     */
    @JvmStatic
    fun eventListener(
        onEvent: (service: Service, seq: Long, properties: List<Pair<String, String>>) -> Unit
    ): EventListener = object : EventListener {
        override fun onEvent(service: Service, seq: Long, properties: List<Pair<String, String>>) {
            onEvent(service, seq, properties)
        }
    }

    /**
     * Adapter for [MulticastEventListener].
     *
     * @param onEvent callback
     * @return MulticastEventListener
     */
    @JvmStatic
    fun multicastEventListener(
        onEvent: (service: Service, lvl: String, seq: Long, properties: List<Pair<String, String>>) -> Unit
    ): MulticastEventListener = object : MulticastEventListener {
        override fun onEvent(service: Service, lvl: String, seq: Long, properties: List<Pair<String, String>>) {
            onEvent(service, lvl, seq, properties)
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