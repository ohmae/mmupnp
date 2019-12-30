/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.server

internal interface Server {
    /**
     * Start a receiving thread.
     */
    fun start()

    /**
     * Stop a receiving thread.
     *
     * It only sends a request for a stop and does not wait.
     */
    fun stop()
}
