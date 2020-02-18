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
import java.net.MalformedURLException
import java.net.URL

internal class DeviceHttpServer(
    private val taskExecutors: TaskExecutors,
    private val delegate: TcpServerDelegate = TcpServerDelegate(taskExecutors, "-description-server")
) : TcpServer by delegate {

    private val methodMap: MutableMap<String, MutableMap<String, (HttpRequest) -> HttpResponse>> = mutableMapOf()

    init {
        delegate.setClientProcess(::process)
    }

    fun registerCallback(method: String, path: String, callback: (HttpRequest) -> HttpResponse) {
        methodMap.getOrPut(method) { mutableMapOf() }[path] = callback
    }

    @Throws(IOException::class)
    private fun process(inputStream: InputStream, outputStream: OutputStream) {
        makeResponse(HttpRequest.create(inputStream))
            .writeData(outputStream)
    }

    private fun makeResponse(request: HttpRequest): HttpResponse {
        val uri = request.getUri()
        if (!uri.startsWith(Http.HTTP_SCHEME) && uri[0] != '/') {
            return RESPONSE_BAD
        }
        try {
            return onRequest(URL(LOCALHOST, uri).path, request)
        } catch (e: MalformedURLException) {
        }
        return RESPONSE_BAD
    }

    private fun onRequest(path: String, request: HttpRequest): HttpResponse {
        val pathMap = methodMap[request.getMethod()] ?: return RESPONSE_BAD
        val callback = pathMap[path] ?: return RESPONSE_NOT_FOUND
        return callback(request)
    }

    companion object {
        private val LOCALHOST: URL = URL("http://localhost")
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
