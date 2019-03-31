/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import net.mm2d.upnp.Adapter.adapter
import net.mm2d.upnp.internal.impl.ControlPointImpl
import net.mm2d.upnp.internal.impl.DiFactory
import java.net.NetworkInterface

/**
 * ControlPointのインスタンスを作成するFactory。
 *
 * 将来的にControlPointもインターフェースに変更するため、
 * ControlPointのコンストラクタはDeprecatedとしている。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object ControlPointFactory {
    /**
     * ControlPointのインスタンスを作成する。
     *
     * @param protocol 使用するプロトコルスタック。未指定の場合デフォルトのプロトコルスタックが使用される
     * @param interfaces 使用するインターフェース。未指定の場合、プロトコルスタックから自動選択される。
     * @param callbackExecutor コールバックを適切なスレッドで実行する[TaskExecutor]。
     * コールバックのスレッドを指定したい場合に指定する。未指定の場合singleThreadのExecutorが使用される。
     * @param callbackHandler コールバックを適切なスレッドで実行する。[callbackExecutor]の指定の方が優先される。
     * @param notifySegmentCheckEnabled SSDP Notifyパケットを受け取った時にセグメントチェックを行う。
     * @return ControlPointのインスタンス
     * @throws IllegalStateException 使用可能なインターフェースがない。
     */
    @JvmStatic
    fun create(
        protocol: Protocol = Protocol.DEFAULT,
        interfaces: Iterable<NetworkInterface>? = null,
        callbackExecutor: TaskExecutor? = null,
        callbackHandler: ((Runnable) -> Boolean)? = null,
        notifySegmentCheckEnabled: Boolean = false
    ): ControlPoint {
        val executor = callbackExecutor
            ?: callbackHandler?.let { adapter(it) }
        return ControlPointImpl(
            protocol,
            getDefaultInterfacesIfEmpty(protocol, interfaces),
            notifySegmentCheckEnabled,
            DiFactory(protocol, executor)
        )
    }

    private fun getDefaultInterfacesIfEmpty(
        protocol: Protocol,
        interfaces: Iterable<NetworkInterface>?
    ): Iterable<NetworkInterface> {
        return if (interfaces?.none() != false) {
            protocol.availableInterfaces
        } else interfaces
    }
}
