/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import net.mm2d.upnp.SsdpMessage

/**
 * SSDPパケットの受信を行うインターフェース
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal interface SsdpServer {
    /**
     * 受信スレッドの開始を行う。
     */
    fun start()

    /**
     * 受信スレッドの停止を行う。
     *
     * 停止のリクエストを送るのみで待ち合わせは行わない。
     */
    fun stop()

    /**
     * このソケットを使用してメッセージ送信を行う。
     *
     * @param messageSupplier 送信するメッセージを作成するラムダ
     */
    fun send(messageSupplier: () -> SsdpMessage)

    companion object {
        /**
         * SSDPに使用するポート番号。
         */
        const val SSDP_PORT = 1900
    }
}
