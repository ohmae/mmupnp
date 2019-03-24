/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import java.util.concurrent.TimeUnit

/**
 * ライブラリのプロパティ情報を管理するクラス。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object Property {
    /**
     * ライブラリバージョン
     */
    const val LIB_VERSION = "mmupnp/2.0.0"
    /**
     * OSバージョン
     */
    @JvmField
    val OS_VERSION = "${System.getProperty("os.name").split(" ")[0]}/${System.getProperty("os.version")}"
    /**
     * UPnPバージョン
     */
    const val UPNP_VERSION = "UPnP/1.0"
    /**
     * ライブラリで使用するUserAgentの値
     */
    @JvmField
    val USER_AGENT_VALUE = "$OS_VERSION $UPNP_VERSION $LIB_VERSION"
    /**
     * ライブラリで使用するServer名
     */
    @JvmField
    val SERVER_VALUE = "$OS_VERSION $UPNP_VERSION $LIB_VERSION"
    /**
     * デフォルトタイムアウト値(ms)（30秒）
     *
     * [java.net.Socket.setSoTimeout]に渡すためint値で定義
     */
    @JvmField
    val DEFAULT_TIMEOUT = TimeUnit.SECONDS.toMillis(30).toInt()
}
