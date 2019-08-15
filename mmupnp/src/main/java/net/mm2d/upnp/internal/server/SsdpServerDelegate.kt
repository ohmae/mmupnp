/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import net.mm2d.log.Logger
import net.mm2d.upnp.Http
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.internal.thread.TaskExecutors
import net.mm2d.upnp.internal.util.closeQuietly
import net.mm2d.upnp.util.findInet4Address
import net.mm2d.upnp.util.findInet6Address
import net.mm2d.upnp.util.toSimpleString
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.*
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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

    private var futureTask: FutureTask<*>? = null
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var ready = false

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
        futureTask ?: stop()
        lock.withLock {
            ready = false
            FutureTask(this, null).also {
                futureTask = it
                taskExecutors.server(it)
            }
        }
    }

    override fun stop() {
        lock.withLock {
            futureTask?.cancel(false)
            futureTask = null
            socket.closeQuietly()
        }
    }

    override fun send(messageSupplier: () -> SsdpMessage) {
        taskExecutors.io { sendInner(messageSupplier()) }
    }

    private fun sendInner(message: SsdpMessage) {
        if (!waitReady()) {
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

    private fun waitReady(): Boolean = lock.withLock {
        val task = futureTask ?: return false
        if (task.isDone) return false
        if (!ready) {
            try {
                condition.awaitNanos(PREPARE_TIMEOUT_NANOS)
            } catch (ignored: InterruptedException) {
            }
        }
        ready
    }

    private fun notifyReady() {
        lock.withLock {
            ready = true
            condition.signalAll()
        }
    }

    // VisibleForTesting
    internal fun isCanceled(): Boolean = futureTask?.isCancelled ?: true

    override fun run() {
        val suffix = (if (bindPort == 0) "-ssdp-notify-" else "-ssdp-search-") +
            networkInterface.name + "-" + interfaceAddress.address.toSimpleString()
        Thread.currentThread().let {
            it.name = it.name + suffix
        }
        if (isCanceled()) {
            return
        }
        try {
            val socket = createMulticastSocket(bindPort)
            this.socket = socket
            if (bindPort != 0) {
                socket.joinGroup(getSsdpInetAddress())
            }
            notifyReady()
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
        while (!isCanceled()) {
            try {
                val dp = DatagramPacket(buf, buf.size)
                socket.receive(dp)
                if (isCanceled()) {
                    break
                }
                receiver?.invoke(dp.address, dp.data, dp.length)
            } catch (ignored: SocketTimeoutException) {
            }
        }
    }

    companion object {
        private val PREPARE_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(3)
    }
}

internal val DEFAULT_SSDP_MESSAGE_FILTER: (SsdpMessage) -> Boolean = { true }

/**
 * A normal URL is described in the Location of SsdpMessage,
 * and it is checked whether there is a mismatch between the description address and the packet source address.
 *
 * @receiver SsdpMessage to check
 * @param sourceAddress source address
 * @return true: if there is an invalid Location, such as a mismatch with the sender. false: otherwise
 */
internal fun SsdpMessage.hasInvalidLocation(sourceAddress: InetAddress): Boolean =
    (!hasValidLocation(sourceAddress)).also {
        if (it) Logger.w { "Location: $location is invalid from $sourceAddress" }
    }

private fun SsdpMessage.hasValidLocation(sourceAddress: InetAddress): Boolean {
    val location = location ?: return false
    if (!Http.isHttpUrl(location)) {
        return false
    }
    try {
        return sourceAddress == InetAddress.getByName(URL(location).host)
    } catch (ignored: IOException) {
    }
    return false
}

internal fun SsdpMessage.isNotUpnp(): Boolean {
    if (getHeader("X-TelepathyAddress.sony.com") != null) {
        // Sony telepathy service(urn:schemas-sony-com:service:X_Telepathy:1) send ssdp packet,
        // but it's location address refuses connection. Since this is not correct as UPnP, ignore it.
        Logger.v("ignore sony telepathy service")
        return true
    }
    return false
}
