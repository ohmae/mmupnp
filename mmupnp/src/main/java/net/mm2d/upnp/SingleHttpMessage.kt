/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Interface of HTTP message.
 *
 * Since the format of Start Line differs between Response and Request, separately implement that part.
 *
 * This specializes in the exchange of small data often used in UPnP communication,
 * and does not assume the exchange of large data.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 * @see SingleHttpResponse
 * @see SingleHttpRequest
 */
interface SingleHttpMessage {
    /**
     * Start Line.
     */
    val startLine: String?

    /**
     * HTTP Version.
     */
    val version: String

    /**
     * Whether this is chunked transmission from the header value.
     */
    val isChunked: Boolean

    /**
     * Content-Length. -1 if unknown
     */
    val contentLength: Int

    /**
     * Returns whether this is Keep-Alive from the header value
     *
     * In case of HTTP/1.0, "Connection" is "keep-alive"
     * In case of HTTP/1.1, "Connection" is not "close"
     * Judge as KeepAlive and return true.
     *
     * @return true if KeepAlive
     */
    fun isKeepAlive(): Boolean

    /**
     * Set the Start Line.
     *
     * @param line Start Line
     */
    @Throws(IllegalArgumentException::class)
    fun setStartLine(line: String)

    /**
     * Set HTTP version
     *
     * @param version HTTP version
     */
    fun setVersion(version: String)

    /**
     * Set the header value
     *
     * @param name header name
     * @param value value
     */
    fun setHeader(name: String, value: String)

    /**
     * Set the header value for line
     *
     * @param line header line
     */
    fun setHeaderLine(line: String)

    /**
     * Return the header value
     *
     * @param name header name
     * @return value
     */
    fun getHeader(name: String): String?

    /**
     * Return message body as String.
     *
     * @return message body
     */
    fun getBody(): String?

    /**
     * Set message body by String.
     *
     * @param body message body
     * @param withContentLength If true, Content-Length will be registered from the registered body value and registered.
     */
    fun setBody(body: String?, withContentLength: Boolean = false)

    /**
     * Return message body as binary.
     *
     * Handling Caution: Binary data is shared with the outside to save memory.
     *
     * @return message body
     */
    fun getBodyBinary(): ByteArray?

    /**
     * Set message body by binary.
     *
     * @param body message body
     * @param withContentLength If true, Content-Length will be registered from the registered body value and registered.
     */
    fun setBodyBinary(body: ByteArray?, withContentLength: Boolean = false)

    /**
     * Convert this message to a string
     *
     * @return message string
     */
    fun getMessageString(): String

    /**
     * Write data to OutputStream
     *
     * @param outputStream Output destination
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    fun writeData(outputStream: OutputStream)

    /**
     * Read data from InputStream.
     *
     * @param inputStream Input source
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    fun readData(inputStream: InputStream)
}
