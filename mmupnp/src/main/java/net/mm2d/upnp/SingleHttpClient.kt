/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import net.mm2d.upnp.internal.util.closeQuietly
import net.mm2d.upnp.log.Logger
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket
import java.net.URL

/**
 * Client that performs HTTP communication.
 *
 * Specialized in sending and receiving small data often used in UPnP communication.
 * It does not assume sending or receiving of huge data.
 * Priority is given to being easy to use, and efficiency is not considered much.
 * In principle, it is assumed that an instance is created for each series of communication to the same server.
 *
 * It also provides a passive keep-alive function that make keep-alive behavior only when the response is keep-alive.
 * Naturally even in the keep-alive state,
 * if it is not the same host port as the connection maintained at post, disconnect and reconnect.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 *
 * @constructor initialize
 *
 * @param keepAlive true: keep-alive
 */
class SingleHttpClient(keepAlive: Boolean = true) {
    private class SocketHolder(
        val socket: Socket
    ) {
        val input: InputStream = BufferedInputStream(socket.getInputStream())
        val output: OutputStream = BufferedOutputStream(socket.getOutputStream())

        fun close() {
            socket.closeQuietly()
        }
    }

    private var socketHolder: SocketHolder? = null

    /**
     * Returns the local address used by the socket.
     *
     * Save after connect and hold until the next connection.
     */
    var localAddress: InetAddress? = null
        private set

    /**
     * keep-alive
     *
     * Default is true.
     * If true, the connection is continued if the response can be keep-alive.
     * Even if true, the connection is disconnected if the response is not capable of keep-alive.
     * If false, the connection is disconnected regardless of the response.
     *
     * Also, even if true or false,
     * the contents of [SingleHttpRequest] passed in the post argument will not be changed.
     *
     * If you need to describe keep-alive in the header,
     * you need to set it in [SingleHttpRequest] before calling post.
     */
    var isKeepAlive: Boolean = keepAlive

    /**
     * true if the socket is closed
     */
    val isClosed: Boolean
        get() = socketHolder == null

    /**
     * Send a request and receive a response.
     *
     * The HTTP method to use depends on the argument.
     *
     * @param request Request to send
     * @return Received response
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    fun post(request: SingleHttpRequest): SingleHttpResponse = post(request, 0)

    /**
     * Send a request and receive a response.
     *
     * The HTTP method to use depends on the argument.
     *
     * @param request Request to send
     * @param redirectDepth Depth of redirect
     * @return Received response
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    private fun post(request: SingleHttpRequest, redirectDepth: Int): SingleHttpResponse {
        confirmReuseSocket(request)
        val response: SingleHttpResponse = try {
            doRequest(request)
        } catch (e: IOException) {
            closeSocket()
            throw e
        }
        if (!isKeepAlive || !response.isKeepAlive()) {
            closeSocket()
        }
        return redirectIfNeeded(request, response, redirectDepth)
    }

    private fun confirmReuseSocket(request: SingleHttpRequest) {
        if (!canReuse(request)) {
            closeSocket()
        }
    }

    @Throws(IOException::class)
    private fun doRequest(request: SingleHttpRequest): SingleHttpResponse {
        val socketHolder = socketHolder
        return if (socketHolder == null) {
            writeAndRead(openSocket(request), request)
        } else {
            try {
                writeAndRead(socketHolder, request)
            } catch (e: IOException) {
                // コネクションを再利用した場合はpeerから既に切断されていた可能性がある。
                // KeepAliveできないサーバである可能性があるのでKeepAliveを無効にしてリトライ
                Logger.w { "retry:\n" + e.message }
                isKeepAlive = false
                closeSocket()
                writeAndRead(openSocket(request), request)
            }
        }
    }

    // open状態でのみコールする
    @Throws(IOException::class)
    private fun writeAndRead(socketHolder: SocketHolder, request: SingleHttpRequest): SingleHttpResponse {
        request.writeData(socketHolder.output)
        return SingleHttpResponse.create(socketHolder.input)
    }

    @Throws(IOException::class)
    private fun redirectIfNeeded(
        request: SingleHttpRequest,
        response: SingleHttpResponse,
        redirectDepth: Int
    ): SingleHttpResponse {
        if (needToRedirect(response) && redirectDepth < REDIRECT_MAX) {
            val location = response.getHeader(Http.LOCATION)
            if (!location.isNullOrEmpty()) {
                return redirect(request, location, redirectDepth)
            }
        }
        return response
    }

    private fun needToRedirect(response: SingleHttpResponse): Boolean =
        when (response.getStatus()) {
            Http.Status.HTTP_MOVED_PERM,
            Http.Status.HTTP_FOUND,
            Http.Status.HTTP_SEE_OTHER,
            Http.Status.HTTP_TEMP_REDIRECT -> true
            else -> false
        }

    @Throws(IOException::class)
    private fun redirect(request: SingleHttpRequest, location: String, redirectDepth: Int): SingleHttpResponse {
        val newRequest = SingleHttpRequest.copy(request).apply {
            setUrl(URL(location), true)
            setHeader(Http.CONNECTION, Http.CLOSE)
        }
        return SingleHttpClient(false)
            .post(newRequest, redirectDepth + 1)
    }

    // VisibleForTesting
    internal fun canReuse(request: SingleHttpRequest): Boolean =
        socketHolder?.socket?.canReuse(request) ?: false

    // VisibleForTesting
    internal fun Socket.canReuse(request: SingleHttpRequest): Boolean =
        isConnected && inetAddress == request.address && port == request.port

    @Throws(IOException::class)
    private fun openSocket(request: SingleHttpRequest): SocketHolder {
        val socket = Socket().also {
            it.connect(request.getSocketAddress(), Property.DEFAULT_TIMEOUT)
            it.soTimeout = Property.DEFAULT_TIMEOUT
            localAddress = it.localAddress
        }
        return SocketHolder(socket).also {
            socketHolder = it
        }
    }

    private fun closeSocket() {
        socketHolder?.close()
        socketHolder = null
    }

    /**
     * close socket.
     */
    fun close(): Unit = closeSocket()

    /**
     * Get a string by simple HTTP GET.
     *
     * @param url Destination URL
     * @return Received String
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    fun downloadString(url: URL): String = download(url).getBody()!!

    /**
     * Get a binary by simple HTTP GET.
     *
     * @param url Destination URL
     * @return Received binary
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    fun downloadBinary(url: URL): ByteArray = download(url).getBodyBinary()!!

    /**
     * Invoke simple HTTP GET.
     *
     * @param url Destination URL
     * @return Received response
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    fun download(url: URL): SingleHttpResponse {
        val request = makeHttpRequest(url)
        return post(request).also {
            // response bodyがemptyであることは正常
            if (it.getStatus() !== Http.Status.HTTP_OK || it.getBody() == null) {
                Logger.i { "request:\n$request\nresponse:\n$it" }
                throw IOException(it.startLine)
            }
        }
    }

    @Throws(IOException::class)
    private fun makeHttpRequest(url: URL): SingleHttpRequest =
        SingleHttpRequest.create().apply {
            setMethod(Http.GET)
            setUrl(url, true)
            setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
            setHeader(Http.CONNECTION, if (isKeepAlive) Http.KEEP_ALIVE else Http.CLOSE)
        }

    companion object {
        private const val REDIRECT_MAX = 2

        internal fun create(keepAlive: Boolean = true) = SingleHttpClient(keepAlive)
    }
}
