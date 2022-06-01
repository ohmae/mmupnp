/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import net.mm2d.upnp.Http
import net.mm2d.upnp.Property
import net.mm2d.upnp.SingleHttpRequest
import net.mm2d.upnp.SingleHttpResponse
import net.mm2d.upnp.internal.parser.parseEventXml
import net.mm2d.upnp.internal.thread.TaskExecutors
import net.mm2d.upnp.internal.thread.ThreadCondition
import net.mm2d.upnp.internal.util.closeQuietly
import net.mm2d.upnp.log.Logger
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.*

/**
 * Class to receive Event notified by event subscription.
 *
 * It only accepts requests as an HTTP server.
 * The listener parses HTTP messages.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class EventReceiver(
    private val taskExecutors: TaskExecutors,
    private val listener: (sid: String, seq: Long, properties: List<Pair<String, String>>) -> Boolean
) : Runnable {
    private var serverSocket: ServerSocket? = null
    private val clientList: MutableList<ClientTask> = Collections.synchronizedList(LinkedList())
    private val threadCondition = ThreadCondition(taskExecutors.server)

    fun start(): Unit = threadCondition.start(this)

    fun stop() {
        threadCondition.stop()
        serverSocket.closeQuietly()
        synchronized(clientList) {
            clientList.forEach { it.stop() }
            clientList.clear()
        }
    }

    // VisibleForTesting
    @Throws(IOException::class)
    internal fun createServerSocket(): ServerSocket = ServerSocket(0)

    fun getLocalPort(): Int {
        if (!threadCondition.waitReady()) return 0
        return serverSocket?.localPort ?: 0
    }

    override fun run() {
        Thread.currentThread().let {
            it.name = it.name + "-event-receiver"
        }
        try {
            val socket = createServerSocket()
            serverSocket = socket
            threadCondition.notifyReady()
            while (!threadCondition.isCanceled()) {
                val clientSocket = socket.accept().also {
                    it.soTimeout = Property.DEFAULT_TIMEOUT
                }
                ClientTask(taskExecutors, this, clientSocket).let {
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

    fun notifyClientFinished(client: ClientTask) {
        clientList.remove(client)
    }

    // VisibleForTesting
    internal fun notifyEvent(sid: String, request: SingleHttpRequest): Boolean {
        val seq = request.getHeader(Http.SEQ)?.toLongOrNull() ?: return false
        val properties = request.getBody().parseEventXml()
        if (properties.isEmpty()) return false
        return listener(sid, seq, properties)
    }

    // VisibleForTesting
    internal class ClientTask(
        taskExecutors: TaskExecutors,
        private val eventReceiver: EventReceiver,
        private val socket: Socket
    ) : Runnable {
        private val condition = ThreadCondition(taskExecutors.io)

        fun start(): Unit = condition.start(this)

        fun stop() {
            condition.stop()
            socket.closeQuietly()
        }

        override fun run() {
            try {
                receiveAndReply(socket.getInputStream(), socket.getOutputStream())
            } catch (e: IOException) {
                Logger.w(e)
            } finally {
                socket.closeQuietly()
                eventReceiver.notifyClientFinished(this)
            }
        }

        // VisibleForTesting
        @Throws(IOException::class)
        fun receiveAndReply(inputStream: InputStream, outputStream: OutputStream) {
            val request = SingleHttpRequest.create().apply {
                readData(inputStream)
            }
            Logger.v { "receive event:\n$request" }
            val nt = request.getHeader(Http.NT)
            val nts = request.getHeader(Http.NTS)
            val sid = request.getHeader(Http.SID)
            if (nt.isNullOrEmpty() || nts.isNullOrEmpty()) {
                RESPONSE_BAD.writeData(outputStream)
            } else if (sid.isNullOrEmpty() || nt != Http.UPNP_EVENT || nts != Http.UPNP_PROPCHANGE) {
                RESPONSE_FAIL.writeData(outputStream)
            } else {
                if (eventReceiver.notifyEvent(sid, request)) {
                    RESPONSE_OK.writeData(outputStream)
                } else {
                    RESPONSE_FAIL.writeData(outputStream)
                }
            }
        }

        companion object {
            private val RESPONSE_OK = SingleHttpResponse.create().apply {
                setStatus(Http.Status.HTTP_OK)
                setHeader(Http.SERVER, Property.SERVER_VALUE)
                setHeader(Http.CONNECTION, Http.CLOSE)
                setHeader(Http.CONTENT_LENGTH, "0")
            }
            private val RESPONSE_BAD = SingleHttpResponse.create().apply {
                setStatus(Http.Status.HTTP_BAD_REQUEST)
                setHeader(Http.SERVER, Property.SERVER_VALUE)
                setHeader(Http.CONNECTION, Http.CLOSE)
                setHeader(Http.CONTENT_LENGTH, "0")
            }
            private val RESPONSE_FAIL = SingleHttpResponse.create().apply {
                setStatus(Http.Status.HTTP_PRECON_FAILED)
                setHeader(Http.SERVER, Property.SERVER_VALUE)
                setHeader(Http.CONNECTION, Http.CLOSE)
                setHeader(Http.CONTENT_LENGTH, "0")
            }
        }
    }
}
