/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import net.mm2d.log.Logger
import net.mm2d.upnp.common.Protocol
import net.mm2d.upnp.common.SsdpMessage
import net.mm2d.upnp.common.internal.thread.TaskExecutors
import java.net.NetworkInterface

/**
 * Class for putting together [SsdpSearchServer] for all interfaces.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SsdpSearchServerList(
    taskExecutors: TaskExecutors,
    protocol: Protocol,
    interfaces: Iterable<NetworkInterface>,
    listener: (SsdpMessage) -> Unit
) {
    private val list: List<SsdpSearchServer> = interfaces.createServerList(protocol,
        { newServer(taskExecutors, Address.IP_V4, it, listener) },
        { newServer(taskExecutors, Address.IP_V6, it, listener) }
    )

    fun setFilter(predicate: (SsdpMessage) -> Boolean): Unit =
        list.forEach { it.setFilter(predicate) }

    fun start(): Unit =
        list.forEach { it.start() }

    fun stop(): Unit =
        list.forEach { it.stop() }

    fun search(st: String?): Unit =
        list.forEach { it.search(st) }

    companion object {
        // VisibleForTesting
        internal fun newServer(
            taskExecutors: TaskExecutors,
            address: Address,
            nif: NetworkInterface,
            listener: (SsdpMessage) -> Unit
        ): SsdpSearchServer? = try {
            SsdpSearchServer(taskExecutors, address, nif).also {
                it.setResponseListener(listener)
            }
        } catch (e: IllegalArgumentException) {
            Logger.e(e)
            null
        }
    }
}
