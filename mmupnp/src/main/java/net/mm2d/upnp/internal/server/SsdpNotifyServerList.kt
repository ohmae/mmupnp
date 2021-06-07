/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import net.mm2d.upnp.Protocol
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.internal.thread.TaskExecutors
import net.mm2d.upnp.log.Logger
import java.net.NetworkInterface

/**
 * Class for putting together [SsdpNotifyServer] for all interfaces.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SsdpNotifyServerList(
    taskExecutors: TaskExecutors,
    protocol: Protocol,
    interfaces: Iterable<NetworkInterface>,
    listener: (SsdpMessage) -> Unit
) {
    private val list: List<SsdpNotifyServer> = interfaces.createServerList(protocol,
        { newServer(taskExecutors, Address.IP_V4, it, listener) },
        { newServer(taskExecutors, Address.IP_V6, it, listener) }
    )

    fun setSegmentCheckEnabled(enabled: Boolean): Unit =
        list.forEach { it.setSegmentCheckEnabled(enabled) }

    fun setFilter(predicate: (SsdpMessage) -> Boolean): Unit =
        list.forEach { it.setFilter(predicate) }

    fun start(): Unit = list.forEach { it.start() }
    fun stop(): Unit = list.forEach { it.stop() }

    companion object {
        // VisibleForTesting
        internal fun newServer(
            taskExecutors: TaskExecutors,
            address: Address,
            nif: NetworkInterface,
            listener: (SsdpMessage) -> Unit
        ): SsdpNotifyServer? = try {
            SsdpNotifyServer(taskExecutors, address, nif).also {
                it.setNotifyListener(listener)
            }
        } catch (e: IllegalArgumentException) {
            Logger.e(e)
            null
        }
    }
}
