/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import net.mm2d.upnp.common.SsdpMessage

/**
 * Interface for receiving SSDP packets
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal interface SsdpServer {
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

    /**
     * Send a message using this socket.
     *
     * @param messageSupplier Lambda to create a message to send
     */
    fun send(messageSupplier: () -> SsdpMessage)
}
