/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da.internal.server

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

internal class DescriptionServer(
    private val taskExecutors: TaskExecutors,
    private val delegate: TcpServerDelegate = TcpServerDelegate(taskExecutors, "-description-server")
) : TcpServer by delegate {
    init {
        delegate.setClientProcess(::process)
    }

    @Throws(IOException::class)
    private fun process(inputStream: InputStream, outputStream: OutputStream) {
        val request = HttpRequest.create().apply {
            readData(inputStream)
        }
        makeResponse(request)
            .writeData(outputStream)
    }

    private fun makeResponse(request: HttpRequest): HttpResponse {
        if (request.getMethod() != Http.GET) {
            return RESPONSE_BAD
        }
        return RESPONSE_BAD
    }

    companion object {
        private val RESPONSE_BAD = HttpResponse.create().apply {
            setStatus(Http.Status.HTTP_BAD_REQUEST)
            setHeader(Http.SERVER, Property.SERVER_VALUE)
            setHeader(Http.CONNECTION, Http.CLOSE)
            setHeader(Http.CONTENT_LENGTH, "0")
        }
        private val RESPONSE_NOT_FOUND = HttpResponse.create().apply {
            setStatus(Http.Status.HTTP_NOT_FOUND)
            setHeader(Http.SERVER, Property.SERVER_VALUE)
            setHeader(Http.CONNECTION, Http.CLOSE)
            setHeader(Http.CONTENT_LENGTH, "0")
        }
    }
}
