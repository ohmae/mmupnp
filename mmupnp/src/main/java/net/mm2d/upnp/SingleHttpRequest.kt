/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import net.mm2d.upnp.internal.message.SingleHttpMessageDelegate
import net.mm2d.upnp.internal.message.SingleHttpMessageDelegate.StartLineDelegate
import net.mm2d.upnp.util.toAddressString
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.URL

/**
 * HTTP request message.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class SingleHttpRequest internal constructor(
    private val startLineDelegate: StartLine,
    private val delegate: SingleHttpMessageDelegate
) : SingleHttpMessage by delegate {

    internal data class StartLine(
        var method: String = Http.GET,
        var uri: String = "",
        override var version: String = Http.DEFAULT_HTTP_VERSION
    ) : StartLineDelegate {
        override val shouldStriveToReadBody: Boolean = false

        override fun getStartLine(): String = "$method $uri $version"

        override fun setStartLine(startLine: String) {
            val params = startLine.split(" ", limit = 3)
            require(params.size >= 3)
            method = params[0]
            uri = params[1]
            version = params[2]
        }
    }

    /**
     * Destination address.
     */
    var address: InetAddress? = null

    /**
     * Destination port number.
     */
    var port: Int = 0

    /**
     * Returns the request method.
     *
     * @return request method
     */
    fun getMethod(): String = startLineDelegate.method

    /**
     * Set the request method.
     *
     * @param method request method
     */
    fun setMethod(method: String) {
        startLineDelegate.method = method
    }

    /**
     * Return the URI (request path)
     *
     * @return URI (request path)
     */
    fun getUri(): String = startLineDelegate.uri

    /**
     * Set the URI (request path)
     *
     * Setting of path only, not setting of connection destination.
     *
     * @param uri URI (request path)
     */
    fun setUri(uri: String) {
        startLineDelegate.uri = uri
    }

    /**
     * Return address and port number combination string
     *
     * @return address and port string
     */
    @Throws(IllegalStateException::class)
    fun getAddressString(): String =
        address?.toAddressString(port) ?: throw IllegalStateException("address must be set")

    /**
     * Set the destination URL.
     *
     * @param url destination URL
     * @param withHostHeader true: also set the HOST header based on the URL. default is false
     * @throws IOException specify something other than http, or error occurs while the URL parse
     */
    @Throws(IOException::class)
    fun setUrl(url: URL, withHostHeader: Boolean = false) {
        if (url.protocol != "http") {
            throw IOException("unsupported protocol." + url.protocol)
        }
        address = InetAddress.getByName(url.host)
        port = when {
            url.port > 65535 ->
                throw IOException("port number is too large. port=${url.port}")
            url.port < 0 ->
                Http.DEFAULT_PORT
            else ->
                url.port
        }
        setUri(url.file)
        if (withHostHeader) {
            setHeader(Http.HOST, getAddressString())
        }
    }

    /**
     * Return destination SocketAddress
     *
     * @return destination SocketAddress
     */
    @Throws(IllegalStateException::class)
    fun getSocketAddress(): SocketAddress =
        address?.let { InetSocketAddress(it, port) } ?: throw IllegalStateException("address must be set")

    /**
     * Message as String
     */
    override fun toString(): String = delegate.toString()

    companion object {
        /**
         * Create a new instance.
         */
        @JvmStatic
        fun create(): SingleHttpRequest {
            val startLine = StartLine()
            val delegate = SingleHttpMessageDelegate(startLine)
            return SingleHttpRequest(startLine, delegate)
        }

        /**
         * Create a new instance with the same contents as the argument.
         *
         * @param original original message
         */
        @JvmStatic
        fun copy(original: SingleHttpRequest): SingleHttpRequest {
            val startLine = original.startLineDelegate.copy()
            val delegate = SingleHttpMessageDelegate(startLine, original.delegate)
            return SingleHttpRequest(startLine, delegate).also {
                it.address = original.address
                it.port = original.port
            }
        }
    }
}
