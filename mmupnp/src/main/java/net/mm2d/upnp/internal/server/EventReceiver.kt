/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import net.mm2d.log.Logger
import net.mm2d.upnp.Http
import net.mm2d.upnp.HttpRequest
import net.mm2d.upnp.HttpResponse
import net.mm2d.upnp.Property
import net.mm2d.upnp.internal.thread.TaskExecutors
import net.mm2d.upnp.internal.util.closeQuietly
import net.mm2d.upnp.util.XmlUtils
import net.mm2d.upnp.util.forEachElement
import org.xml.sax.SAXException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.xml.parsers.ParserConfigurationException
import kotlin.concurrent.withLock

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
    private val listener: ((sid: String, seq: Long, properties: List<Pair<String, String>>) -> Boolean)?
) : Runnable {
    private var serverSocket: ServerSocket? = null
    private val clientList: MutableList<ClientTask> = Collections.synchronizedList(LinkedList())
    private var futureTask: FutureTask<*>? = null
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var ready = false

    fun start() {
        lock.withLock {
            ready = false
        }
        FutureTask(this, null).also {
            futureTask = it
            taskExecutors.server(it)
        }
    }

    fun stop() {
        futureTask?.cancel(false)
        futureTask = null
        serverSocket.closeQuietly()
        synchronized(clientList) {
            clientList.forEach { it.stop() }
            clientList.clear()
        }
    }

    // VisibleForTesting
    @Throws(IOException::class)
    internal fun createServerSocket(): ServerSocket {
        return ServerSocket(0)
    }

    fun getLocalPort(): Int {
        if (!waitReady()) return 0
        return serverSocket?.localPort ?: 0
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

    private fun isCanceled(): Boolean {
        return futureTask?.isCancelled ?: true
    }

    override fun run() {
        Thread.currentThread().let {
            it.name = it.name + "-event-receiver"
        }
        try {
            val socket = createServerSocket()
            serverSocket = socket
            notifyReady()
            while (!isCanceled()) {
                val clientSocket = socket.accept().also {
                    it.soTimeout = Property.DEFAULT_TIMEOUT
                }
                ClientTask(this, clientSocket).let {
                    clientList.add(it)
                    it.start(taskExecutors)
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
    fun notifyEvent(sid: String, request: HttpRequest): Boolean {
        val listener = listener ?: return false
        val list = parsePropertyPairs(request)
        if (list.isEmpty()) {
            return false
        }
        val seq = request.getHeader(Http.SEQ)?.toLongOrNull() ?: 0
        return listener.invoke(sid, seq, list)
    }

    // VisibleForTesting
    internal class ClientTask(
        private val eventReceiver: EventReceiver,
        private val socket: Socket
    ) : Runnable {
        private var futureTask: FutureTask<Nothing?>? = null

        fun start(taskExecutors: TaskExecutors) {
            FutureTask(this, null).also {
                futureTask = it
                taskExecutors.io(it)
            }
        }

        fun stop() {
            futureTask?.cancel(false)
            futureTask = null
            socket.closeQuietly()
        }

        private fun notifyEvent(sid: String, request: HttpRequest): Boolean {
            return eventReceiver.notifyEvent(sid, request)
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
            val request = HttpRequest.create().apply {
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
                if (notifyEvent(sid, request)) {
                    RESPONSE_OK.writeData(outputStream)
                } else {
                    RESPONSE_FAIL.writeData(outputStream)
                }
            }
        }

        companion object {
            private val RESPONSE_OK = HttpResponse.create().apply {
                setStatus(Http.Status.HTTP_OK)
                setHeader(Http.SERVER, Property.SERVER_VALUE)
                setHeader(Http.CONNECTION, Http.CLOSE)
                setHeader(Http.CONTENT_LENGTH, "0")
            }
            private val RESPONSE_BAD = HttpResponse.create().apply {
                setStatus(Http.Status.HTTP_BAD_REQUEST)
                setHeader(Http.SERVER, Property.SERVER_VALUE)
                setHeader(Http.CONNECTION, Http.CLOSE)
                setHeader(Http.CONTENT_LENGTH, "0")
            }
            private val RESPONSE_FAIL = HttpResponse.create().apply {
                setStatus(Http.Status.HTTP_PRECON_FAILED)
                setHeader(Http.SERVER, Property.SERVER_VALUE)
                setHeader(Http.CONNECTION, Http.CLOSE)
                setHeader(Http.CONTENT_LENGTH, "0")
            }
        }
    }

    companion object {
        private val PREPARE_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(1)

        internal fun parsePropertyPairs(request: HttpRequest): List<Pair<String, String>> {
            val xml = request.getBody()
            if (xml.isNullOrEmpty()) {
                return emptyList()
            }
            try {
                val propertySetNode = XmlUtils.newDocument(true, xml).documentElement
                if (propertySetNode.localName != "propertyset") {
                    return emptyList()
                }
                val list = mutableListOf<Pair<String, String>>()
                propertySetNode.firstChild.forEachElement {
                    if (it.localName == "property") {
                        it.firstChild.forEachElement {
                            val name = it.localName
                            if (!name.isNullOrEmpty()) {
                                list.add(name to it.textContent)
                            }
                        }
                    }
                }
                return list
            } catch (ignored: IOException) {
            } catch (ignored: SAXException) {
            } catch (ignored: ParserConfigurationException) {
            }
            return emptyList()
        }
    }
}
