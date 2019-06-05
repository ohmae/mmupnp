/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import net.mm2d.log.Logger
import net.mm2d.upnp.internal.util.closeQuietly
import java.io.*
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
class HttpClient(keepAlive: Boolean = true) {
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
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
     * the contents of [HttpRequest] passed in the post argument will not be changed.
     *
     * If you need to describe keep-alive in the header,
     * you need to set it in [HttpRequest] before calling post.
     */
    var isKeepAlive: Boolean = keepAlive

    /**
     * true if the socket is closed
     */
    val isClosed: Boolean
        get() = socket == null

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
    fun post(request: HttpRequest): HttpResponse {
        return post(request, 0)
    }

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
    private fun post(request: HttpRequest, redirectDepth: Int): HttpResponse {
        confirmReuseSocket(request)
        val response: HttpResponse = try {
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

    private fun confirmReuseSocket(request: HttpRequest) {
        if (!canReuse(request)) {
            closeSocket()
        }
    }

    @Throws(IOException::class)
    private fun doRequest(request: HttpRequest): HttpResponse {
        return if (isClosed) {
            openSocket(request)
            writeAndRead(request)
        } else {
            try {
                writeAndRead(request)
            } catch (e: IOException) {
                // コネクションを再利用した場合はpeerから既に切断されていた可能性がある。
                // KeepAliveできないサーバである可能性があるのでKeepAliveを無効にしてリトライ
                Logger.w { "retry:\n" + e.message }
                isKeepAlive = false
                closeSocket()
                openSocket(request)
                writeAndRead(request)
            }
        }
    }

    // open状態でのみコールする
    @Throws(IOException::class)
    private fun writeAndRead(request: HttpRequest): HttpResponse {
        request.writeData(outputStream!!)
        return HttpResponse.create().also { it.readData(inputStream!!) }
    }

    @Throws(IOException::class)
    private fun redirectIfNeeded(request: HttpRequest, response: HttpResponse, redirectDepth: Int): HttpResponse {
        if (needToRedirect(response) && redirectDepth < REDIRECT_MAX) {
            val location = response.getHeader(Http.LOCATION)
            if (!location.isNullOrEmpty()) {
                return redirect(request, location, redirectDepth)
            }
        }
        return response
    }

    private fun needToRedirect(response: HttpResponse): Boolean {
        return when (response.getStatus()) {
            Http.Status.HTTP_MOVED_PERM,
            Http.Status.HTTP_FOUND,
            Http.Status.HTTP_SEE_OTHER,
            Http.Status.HTTP_TEMP_REDIRECT -> true
            else -> false
        }
    }

    @Throws(IOException::class)
    private fun redirect(request: HttpRequest, location: String, redirectDepth: Int): HttpResponse {
        val newRequest = HttpRequest.copy(request).apply {
            setUrl(URL(location), true)
            setHeader(Http.CONNECTION, Http.CLOSE)
        }
        return HttpClient(false)
            .post(newRequest, redirectDepth + 1)
    }

    // VisibleForTesting
    internal fun canReuse(request: HttpRequest): Boolean {
        val socket = socket ?: return false
        return socket.isConnected &&
                socket.inetAddress == request.address &&
                socket.port == request.port
    }

    @Throws(IOException::class)
    private fun openSocket(request: HttpRequest) {
        val socket = Socket().also { socket = it }
        socket.connect(request.getSocketAddress(), Property.DEFAULT_TIMEOUT)
        socket.soTimeout = Property.DEFAULT_TIMEOUT
        inputStream = BufferedInputStream(socket.getInputStream())
        outputStream = BufferedOutputStream(socket.getOutputStream())
        localAddress = socket.localAddress
    }

    private fun closeSocket() {
        socket.closeQuietly()
        inputStream = null
        outputStream = null
        socket = null
    }

    /**
     * close socket.
     */
    fun close() {
        closeSocket()
    }

    /**
     * Get a string by simple HTTP GET.
     *
     * @param url Destination URL
     * @return Received String
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    fun downloadString(url: URL): String {
        // download()の中でgetBody()がnullで無いことはチェック済み
        return download(url).getBody()!!
    }

    /**
     * Get a binary by simple HTTP GET.
     *
     * @param url Destination URL
     * @return Received binary
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    fun downloadBinary(url: URL): ByteArray {
        // download()の中でgetBody()がnullで無いことはチェック済み
        return download(url).getBodyBinary()!!
    }

    /**
     * Invoke simple HTTP GET.
     *
     * @param url Destination URL
     * @return Received response
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    fun download(url: URL): HttpResponse {
        val request = makeHttpRequest(url)
        val response = post(request)
        // response bodyがemptyであることは正常
        if (response.getStatus() !== Http.Status.HTTP_OK || response.getBody() == null) {
            Logger.i { "request:\n$request\nresponse:\n$response" }
            throw IOException(response.startLine)
        }
        return response
    }

    @Throws(IOException::class)
    private fun makeHttpRequest(url: URL): HttpRequest {
        return HttpRequest.create().apply {
            setMethod(Http.GET)
            setUrl(url, true)
            setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
            setHeader(Http.CONNECTION, if (isKeepAlive) Http.KEEP_ALIVE else Http.CLOSE)
        }
    }

    companion object {
        private const val REDIRECT_MAX = 2
    }
}
