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
 * 全インターフェース分の[SsdpNotifyReceiver]をまとめるためのクラス。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SsdpNotifyReceiverList(
        taskExecutors: TaskExecutors,
        protocol: Protocol,
        interfaces: Iterable<NetworkInterface>,
        listener: (SsdpMessage) -> Unit
) {
    private val list: List<SsdpNotifyReceiver> = when (protocol) {
        Protocol.IP_V4_ONLY -> {
            interfaces.filter { it.isAvailableInet4Interface() }
                    .map { newReceiver(taskExecutors, Address.IP_V4, it, listener) }
        }
        Protocol.IP_V6_ONLY -> {
            interfaces.filter { it.isAvailableInet6Interface() }
                    .map { newReceiver(taskExecutors, Address.IP_V6_LINK_LOCAL, it, listener) }
        }
        Protocol.DUAL_STACK -> {
            val v4 = interfaces.filter { it.isAvailableInet4Interface() }
                    .map { newReceiver(taskExecutors, Address.IP_V4, it, listener) }
            val v6 = interfaces.filter { it.isAvailableInet6Interface() }
                    .map { newReceiver(taskExecutors, Address.IP_V6_LINK_LOCAL, it, listener) }
            v4.toMutableList().also { it.addAll(v6) }
        }
    }

    fun setSegmentCheckEnabled(enabled: Boolean) {
        list.forEach { it.setSegmentCheckEnabled(enabled) }
    }

    fun start() {
        list.forEach { it.start() }
    }

    fun stop() {
        list.forEach { it.stop() }
    }

    companion object {
        // VisibleForTesting
        fun newReceiver(
                taskExecutors: TaskExecutors,
                address: Address,
                nif: NetworkInterface,
                listener: (SsdpMessage) -> Unit
        ): SsdpNotifyReceiver {
            return SsdpNotifyReceiver(taskExecutors, address, nif).also {
                it.setNotifyListener(listener)
            }
        }
    }
}
