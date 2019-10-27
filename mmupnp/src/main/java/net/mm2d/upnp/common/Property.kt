/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common

import java.util.concurrent.TimeUnit

/**
 * Class that manages library property.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object Property {
    /**
     * Library version
     */
    private const val LIB_VERSION = "mmupnp/3.0.0"
    /**
     * UPnP Version
     */
    private const val UPNP_VERSION = "UPnP/1.0"
    /**
     * OS Version
     */
    private val OS_VERSION = "${System.getProperty("os.name").split(" ")[0]}/${System.getProperty("os.version")}"
    /**
     * User-Agent
     */
    @JvmField
    val USER_AGENT_VALUE = "$OS_VERSION $UPNP_VERSION $LIB_VERSION"
    /**
     * Server name
     */
    @JvmField
    val SERVER_VALUE = "$OS_VERSION $UPNP_VERSION $LIB_VERSION"
    /**
     * Default timeout (ms)
     *
     * Defined as int value for use with [java.net.Socket.setSoTimeout].
     */
    @JvmField
    val DEFAULT_TIMEOUT = TimeUnit.SECONDS.toMillis(30).toInt()
}
