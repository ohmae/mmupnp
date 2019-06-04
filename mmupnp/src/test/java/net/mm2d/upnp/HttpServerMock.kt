/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import net.mm2d.upnp.internal.util.closeQuietly
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.*

typealias ServerCore = (socket: Socket, inputStream: InputStream, outputStream: OutputStream) -> Boolean

class HttpServerMock {
    private var serverSocket: ServerSocket? = null
    private var serverTask: ServerTask? = null
    private var serverCore: ServerCore? = null

    internal val localPort: Int
        get() = serverSocket?.localPort ?: 0

    fun setServerCore(serverCore: ServerCore?) {
        this.serverCore = serverCore
        serverTask?.setServerCore(serverCore)
    }

    @Throws(IOException::class)
    internal fun open() {
        serverSocket = ServerSocket(0).also { socket ->
            serverTask = ServerTask(socket).also { task ->
                task.setServerCore(serverCore)
                task.start()
            }
        }
    }

    internal fun close() {
        serverTask?.shutdownRequest()
        serverTask = null
    }

    private class ServerTask(
        private val serverSocket: ServerSocket
    ) : Runnable {
        @Volatile
        private var shutdownRequest = false
        private val clientList: MutableList<ClientTask> = Collections.synchronizedList(LinkedList())
        private var thread: Thread? = null
        private var serverCore: ServerCore? = null

        fun setServerCore(serverCore: ServerCore?) {
            this.serverCore = serverCore
        }

        internal fun start() {
            thread = Thread(this, "HttpServerMock::ServerTask").also {
                it.start()
            }
        }

        internal fun shutdownRequest() {
            shutdownRequest = true
            thread?.interrupt()
            thread = null
            serverSocket.closeQuietly()
            synchronized(clientList) {
                clientList.forEach { it.shutdownRequest() }
                clientList.clear()
            }
        }

        internal fun notifyClientFinished(client: ClientTask) {
            clientList.remove(client)
        }

        override fun run() {
            try {
                while (!shutdownRequest) {
                    val sock = serverSocket.accept().also {
                        it.soTimeout = Property.DEFAULT_TIMEOUT
                    }
                    ClientTask(this, sock).also {
                        clientList.add(it)
                        it.start()
                    }
                }
            } catch (ignored: IOException) {
            } finally {
                serverSocket.closeQuietly()
            }
        }

        @Throws(IOException::class)
        fun receiveAndReply(socket: Socket, inputStream: InputStream, outputStream: OutputStream): Boolean {
            return serverCore?.invoke(socket, inputStream, outputStream) ?: false
        }
    }

    private class ClientTask(
        private val server: ServerTask,
        private val socket: Socket
    ) : Runnable {
        private var thread: Thread? = null

        internal fun start() {
            thread = Thread(this, "HttpServerMock::ClientTask").also {
                it.start()
            }
        }

        internal fun shutdownRequest() {
            thread?.interrupt()
            thread = null
            socket.closeQuietly()
        }

        override fun run() {
            try {
                socket.use {
                    val input = socket.getInputStream()
                    val output = socket.getOutputStream()
                    while (server.receiveAndReply(socket, input, output)) {
                    }
                }
            } catch (ignored: IOException) {
            }
            server.notifyClientFinished(this)
        }
    }
}
