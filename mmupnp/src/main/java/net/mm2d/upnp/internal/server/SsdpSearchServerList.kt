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
import net.mm2d.upnp.util.isAvailableInet4Interface
import net.mm2d.upnp.util.isAvailableInet6Interface
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
    private val list: List<SsdpSearchServer> = when (protocol) {
        Protocol.IP_V4_ONLY -> {
            interfaces.filter { it.isAvailableInet4Interface() }
                .map { newServer(taskExecutors, Address.IP_V4, it, listener) }
        }
        Protocol.IP_V6_ONLY -> {
            interfaces.filter { it.isAvailableInet6Interface() }
                .map { newServer(taskExecutors, Address.IP_V6_LINK_LOCAL, it, listener) }
        }
        Protocol.DUAL_STACK -> {
            val v4 = interfaces.filter { it.isAvailableInet4Interface() }
                .map { newServer(taskExecutors, Address.IP_V4, it, listener) }
            val v6 = interfaces.filter { it.isAvailableInet6Interface() }
                .map { newServer(taskExecutors, Address.IP_V6_LINK_LOCAL, it, listener) }
            v4.toMutableList().also { it.addAll(v6) }
        }
    }

    fun start(): Unit = list.forEach { it.start() }
    fun stop(): Unit = list.forEach { it.stop() }
    fun search(st: String?): Unit = list.forEach { it.search(st) }

    companion object {
        // VisibleForTesting
        internal fun newServer(
            taskExecutors: TaskExecutors,
            address: Address,
            nif: NetworkInterface,
            listener: (SsdpMessage) -> Unit
        ): SsdpSearchServer = SsdpSearchServer(taskExecutors, address, nif).also {
            it.setResponseListener(listener)
        }
    }
}
