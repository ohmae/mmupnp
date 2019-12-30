/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.server

import net.mm2d.log.Logger
import net.mm2d.upnp.common.SsdpMessage
import net.mm2d.upnp.common.internal.thread.TaskExecutors
import net.mm2d.upnp.common.internal.thread.ThreadCondition
import net.mm2d.upnp.common.internal.util.closeQuietly
import net.mm2d.upnp.common.util.findInet4Address
import net.mm2d.upnp.common.util.findInet6Address
import net.mm2d.upnp.common.util.toSimpleString
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.*

/**
 * A class that implements the common part of [SsdpServer].
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 *
 * @param taskExecutors taskExecutors
 * @param address Multicast address
 * @param networkInterface network interface
 * @param bindPort port number
 */
internal class SsdpServerDelegate(
    private val taskExecutors: TaskExecutors,
    val address: Address,
    private val networkInterface: NetworkInterface,
    private val bindPort: Int = 0
) : SsdpServer, Runnable {
    val interfaceAddress: InterfaceAddress =
        if (address == Address.IP_V4)
            networkInterface.findInet4Address()
        else
            networkInterface.findInet6Address()
    private var socket: MulticastSocket? = null
    private var receiver: ((sourceAddress: InetAddress, data: ByteArray, length: Int) -> Unit)? = null
    private val threadCondition = ThreadCondition(taskExecutors.server)

    fun setReceiver(receiver: ((sourceAddress: InetAddress, data: ByteArray, length: Int) -> Unit)?) {
        this.receiver = receiver
    }

    fun getSsdpInetAddress(): InetAddress = address.ssdpInetAddress

    fun getSsdpAddressString(): String = address.ssdpAddressString

    fun getLocalAddress(): InetAddress = interfaceAddress.address

    // VisibleForTesting
    @Throws(IOException::class)
    internal fun createMulticastSocket(port: Int): MulticastSocket {
        return MulticastSocket(port).also {
            it.networkInterface = networkInterface
            it.timeToLive = 4
        }
    }

    override fun start() {
        receiver ?: throw IllegalStateException("receiver must be set")
        threadCondition.start(this)
    }

    override fun stop() {
        threadCondition.stop()
        socket.closeQuietly()
    }

    override fun send(messageSupplier: () -> SsdpMessage) {
        taskExecutors.io { sendInner(messageSupplier()) }
    }

    private fun sendInner(message: SsdpMessage) {
        if (!threadCondition.waitReady()) {
            Logger.w("socket is not ready")
            return
        }
        val socket = socket ?: return
        Logger.d { "send from $interfaceAddress:\n$message" }
        try {
            val data = ByteArrayOutputStream().also {
                message.writeData(it)
            }.toByteArray()
            socket.send(DatagramPacket(data, data.size, address.ssdpSocketAddress))
        } catch (e: IOException) {
            Logger.w(e)
        }
    }

    override fun run() {
        val suffix = (if (bindPort == 0) "-ssdp-notify-" else "-ssdp-search-") +
            networkInterface.name + "-" + interfaceAddress.address.toSimpleString()
        Thread.currentThread().let {
            it.name = it.name + suffix
        }
        if (threadCondition.isCanceled()) return
        try {
            val socket = createMulticastSocket(bindPort)
            this.socket = socket
            if (bindPort != 0) {
                socket.joinGroup(getSsdpInetAddress())
            }
            threadCondition.notifyReady()
            receiveLoop(socket)
        } catch (ignored: IOException) {
        } finally {
            if (bindPort != 0) {
                socket?.leaveGroup(getSsdpInetAddress())
            }
            socket.closeQuietly()
            socket = null
        }
    }

    // VisibleForTesting
    @Throws(IOException::class)
    internal fun receiveLoop(socket: MulticastSocket) {
        val buf = ByteArray(1500)
        while (!threadCondition.isCanceled()) {
            try {
                val dp = DatagramPacket(buf, buf.size)
                socket.receive(dp)
                if (threadCondition.isCanceled()) break
                receiver?.invoke(dp.address, dp.data, dp.length)
            } catch (ignored: SocketTimeoutException) {
            }
        }
    }
}
