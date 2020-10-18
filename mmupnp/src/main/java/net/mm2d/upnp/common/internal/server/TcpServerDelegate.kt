/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.server

import net.mm2d.upnp.common.log.Logger
import net.mm2d.upnp.common.Property
import net.mm2d.upnp.common.internal.thread.TaskExecutors
import net.mm2d.upnp.common.internal.thread.ThreadCondition
import net.mm2d.upnp.common.internal.util.closeQuietly
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*

internal class TcpServerDelegate(
    private val taskExecutors: TaskExecutors,
    private val threadSuffix: String
) : TcpServer, Runnable {
    private var serverSocket: ServerSocket? = null
    private val clientList: MutableList<ClientTask> = Collections.synchronizedList(LinkedList())
    private val threadCondition = ThreadCondition(taskExecutors.server)
    private lateinit var clientProcess: (inputStream: InputStream, outputStream: OutputStream) -> Unit

    fun setClientProcess(process: ((inputStream: InputStream, outputStream: OutputStream) -> Unit)) {
        this.clientProcess = process
    }

    override fun start() {
        if (!::clientProcess.isInitialized) error("")
        threadCondition.start(this)
    }

    override fun stop() {
        threadCondition.stop()
        serverSocket.closeQuietly()
        synchronized(clientList) {
            clientList.forEach { it.stop() }
            clientList.clear()
        }
    }

    override fun getLocalPort(): Int {
        if (!threadCondition.waitReady()) return 0
        return serverSocket?.localPort ?: 0
    }

    override fun getInetAddress(): InetAddress? {
        if (!threadCondition.waitReady()) return null
        return serverSocket?.inetAddress
    }

    // VisibleForTesting
    @Throws(IOException::class)
    internal fun createServerSocket(): ServerSocket = ServerSocket(0)

    override fun run() {
        ThreadCondition.setThreadNameSuffix(threadSuffix)
        try {
            val socket = createServerSocket()
            serverSocket = socket
            threadCondition.notifyReady()
            while (!threadCondition.isCanceled()) {
                val clientSocket = socket.accept().also {
                    it.soTimeout = Property.DEFAULT_TIMEOUT
                }
                ClientTask(taskExecutors, this, clientSocket, clientProcess).let {
                    clientList.add(it)
                    it.start()
                }
            }
        } catch (ignored: IOException) {
        } finally {
            serverSocket.closeQuietly()
            serverSocket = null
        }
    }

    // VisibleForTesting
    internal fun notifyClientFinished(client: ClientTask) {
        clientList.remove(client)
    }

    // VisibleForTesting
    internal class ClientTask(
        taskExecutors: TaskExecutors,
        private val server: TcpServerDelegate,
        private val socket: Socket,
        private val clientProcess: (inputStream: InputStream, outputStream: OutputStream) -> Unit
    ) : Runnable {
        private val condition = ThreadCondition(taskExecutors.io)

        fun start(): Unit = condition.start(this)

        fun stop() {
            condition.stop()
            socket.closeQuietly()
        }

        override fun run() {
            try {
                clientProcess(socket.getInputStream(), socket.getOutputStream())
            } catch (e: Exception) {
                Logger.w(e)
            } finally {
                socket.closeQuietly()
                server.notifyClientFinished(this)
            }
        }
    }
}
