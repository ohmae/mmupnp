/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.server

import net.mm2d.upnp.common.SsdpMessage

/**
 * Interface for receiving SSDP packets
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal interface SsdpServer : Server {
    /**
     * Send a message using this socket.
     *
     * @param messageSupplier Lambda to create a message to send
     */
    fun send(messageSupplier: () -> SsdpMessage)
}
