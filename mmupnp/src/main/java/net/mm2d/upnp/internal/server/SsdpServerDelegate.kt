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
import net.mm2d.upnp.util.toSimpleString
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.*
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * [SsdpServer]の共通処理を実装するクラス。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 *
 * @param address          モード
 * @param networkInterface 使用するインターフェース
 * @param bindPort         使用するポート
 */
internal class SsdpServerDelegate
@JvmOverloads constructor(
    private val taskExecutors: TaskExecutors,
    val address: Address,
    private val networkInterface: NetworkInterface,
    private val bindPort: Int = 0
) : SsdpServer, Runnable {
    val interfaceAddress: InterfaceAddress
    private var socket: MulticastSocket? = null
    private var receiver: ((sourceAddress: InetAddress, data: ByteArray, length: Int) -> Unit)? = null

    private var futureTask: FutureTask<*>? = null
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var ready = false

    init {
        interfaceAddress = if (address == Address.IP_V4)
            findInet4Address(networkInterface.interfaceAddresses)
        else
            findInet6Address(networkInterface.interfaceAddresses)
    }

    fun setReceiver(receiver: ((sourceAddress: InetAddress, data: ByteArray, length: Int) -> Unit)?) {
        this.receiver = receiver
    }

    /**
     * SSDPに使用するアドレス。
     *
     * @return SSDPで使用する[InetAddress]
     */
    fun getSsdpInetAddress(): InetAddress = address.inetAddress

    /**
     * SSDPに使用するアドレス＋ポートの文字列。
     *
     * @return SSDPに使用するアドレス＋ポートの文字列。
     */
    fun getSsdpAddressString(): String = address.addressString

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
            socket.send(DatagramPacket(data, data.size, address.socketAddress))
        } catch (e: IOException) {
            Logger.w(e)
        }
    }

    private fun waitReady(): Boolean {
        lock.withLock {
            val task = futureTask ?: return false
            if (task.isDone) return false
            if (!ready) {
                try {
                    condition.awaitNanos(PREPARE_TIMEOUT_NANOS)
                } catch (ignored: InterruptedException) {
                }
            }
            return ready
        }
    }

    private fun notifyReady() {
        lock.withLock {
            ready = true
            condition.signalAll()
        }
    }

    // VisibleForTesting
    internal fun isCanceled(): Boolean {
        return futureTask?.isCancelled ?: true
    }

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

    /**
     * 受信処理を行う。
     *
     * @throws IOException 入出力処理で例外発生
     */
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

        fun findInet4Address(addressList: List<InterfaceAddress>): InterfaceAddress {
            return addressList.find { it.address is Inet4Address }
                ?: throw IllegalArgumentException("ni does not have IPv4 address.")
        }

        fun findInet6Address(addressList: List<InterfaceAddress>): InterfaceAddress {
            return addressList.find { it.address is Inet6Address && it.address.isLinkLocalAddress }
                ?: throw IllegalArgumentException("ni does not have IPv6 address.")
        }

        /**
         * SsdpMessageのLocationに正常なURLが記述されており、
         * 記述のアドレスとパケットの送信元アドレスに不一致がないか検査する。
         *
         * @param message       確認するSsdpMessage
         * @param sourceAddress 送信元アドレス
         * @return true:送信元との不一致を含めてLocationに不正がある場合。false:それ以外
         */
        fun isInvalidLocation(message: SsdpMessage, sourceAddress: InetAddress): Boolean {
            return !isValidLocation(message, sourceAddress)
        }

        private fun isValidLocation(message: SsdpMessage, sourceAddress: InetAddress): Boolean {
            val location = message.location ?: return false
            if (!Http.isHttpUrl(location)) {
                return false
            }
            try {
                return sourceAddress == InetAddress.getByName(URL(location).host)
            } catch (ignored: MalformedURLException) {
            } catch (ignored: UnknownHostException) {
            }
            return false
        }
    }
}
