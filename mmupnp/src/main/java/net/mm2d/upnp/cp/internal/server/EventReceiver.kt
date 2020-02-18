/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.server

import net.mm2d.log.Logger
import net.mm2d.upnp.common.Http
import net.mm2d.upnp.common.HttpRequest
import net.mm2d.upnp.common.HttpResponse
import net.mm2d.upnp.common.Property
import net.mm2d.upnp.common.internal.server.TcpServer
import net.mm2d.upnp.common.internal.server.TcpServerDelegate
import net.mm2d.upnp.common.internal.thread.TaskExecutors
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

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
    private val listener: (sid: String, seq: Long, properties: List<Pair<String, String>>) -> Boolean,
    private val delegate: TcpServerDelegate = TcpServerDelegate(taskExecutors, "-event-receiver")
) : TcpServer by delegate {
    init {
        delegate.setClientProcess(::receiveAndReply)
    }

    @Throws(IOException::class)
    private fun receiveAndReply(inputStream: InputStream, outputStream: OutputStream) {
        val request = HttpRequest.create(inputStream)
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

    // VisibleForTesting
    internal fun notifyEvent(sid: String, request: HttpRequest): Boolean {
        val seq = request.getHeader(Http.SEQ)?.toLongOrNull() ?: return false
        val properties = request.getBody().parseEventXml()
        if (properties.isEmpty()) return false
        return listener(sid, seq, properties)
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
