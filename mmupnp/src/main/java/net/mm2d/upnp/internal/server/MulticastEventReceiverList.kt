/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import net.mm2d.log.Logger
import net.mm2d.upnp.common.Protocol
import net.mm2d.upnp.internal.thread.TaskExecutors
import java.net.NetworkInterface

internal class MulticastEventReceiverList(
    taskExecutors: TaskExecutors,
    protocol: Protocol,
    interfaces: Iterable<NetworkInterface>,
    listener: (uuid: String, svcid: String, lvl: String, seq: Long, properties: List<Pair<String, String>>) -> Unit
) {
    private val list: List<MulticastEventReceiver> = interfaces.createServerList(protocol,
        { newReceiver(taskExecutors, Address.IP_V4, it, listener) },
        { newReceiver(taskExecutors, Address.IP_V6, it, listener) }
    )

    fun start(): Unit = list.forEach { it.start() }
    fun stop(): Unit = list.forEach { it.stop() }

    companion object {
        // VisibleForTesting
        internal fun newReceiver(
            taskExecutors: TaskExecutors,
            address: Address,
            nif: NetworkInterface,
            listener: (uuid: String, svcid: String, lvl: String, seq: Long, properties: List<Pair<String, String>>) -> Unit
        ): MulticastEventReceiver? = try {
            MulticastEventReceiver(taskExecutors, address, nif, listener)
        } catch (e: IllegalArgumentException) {
            Logger.e(e)
            null
        }
    }
}
