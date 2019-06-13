/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message

import net.mm2d.log.Logger
import net.mm2d.upnp.Http
import net.mm2d.upnp.HttpMessage
import java.io.*
import kotlin.math.min

/**
 * Common implementation of [HttpMessage].
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class HttpMessageDelegate(
    private val startLineDelegate: StartLineDelegate,
    original: HttpMessageDelegate? = null
) : HttpMessage {
    internal interface StartLineDelegate {
        var version: String

        fun getStartLine(): String

        fun setStartLine(startLine: String)
    }

    private val headers: HttpHeaders
    private var body: String? = null
    private var bodyBinary: ByteArray? = null

    init {
        if (original == null) {
            headers = HttpHeaders()
        } else {
            headers = HttpHeaders(original.headers)
            body = original.body
            bodyBinary = original.bodyBinary?.copyOf()
        }
    }

    override val startLine: String?
        get() = startLineDelegate.getStartLine()
    override val version: String
        get() = startLineDelegate.version
    override val isChunked: Boolean
        get() = headers.containsValue(Http.TRANSFER_ENCODING, Http.CHUNKED)
    override val contentLength: Int
        get() = headers[Http.CONTENT_LENGTH]?.toIntOrNull() ?: 0

    // VisibleForTesting
    @Throws(UnsupportedEncodingException::class)
    fun ByteArray.newString(): String {
        return String(this, CHARSET)
    }

    private fun getHeaderString(): String {
        return getHeaderStringBuilder().toString()
    }

    private fun getHeaderStringBuilder(): StringBuilder {
        return StringBuilder().also { sb ->
            sb.append(startLine)
            sb.append(EOL)
            headers.values().forEach {
                sb.append(it.name)
                sb.append(": ")
                sb.append(it.value)
                sb.append(EOL)
            }
            sb.append(EOL)
        }
    }

    // VisibleForTesting
    internal fun getHeaderBytes(): ByteArray {
        try {
            return getBytes(getHeaderString())
        } catch (e: UnsupportedEncodingException) {
            Logger.w(e)
        }
        return ByteArray(0)
    }

    override fun isKeepAlive(): Boolean = if (version == Http.HTTP_1_0) {
        headers.containsValue(Http.CONNECTION, Http.KEEP_ALIVE)
    } else {
        !headers.containsValue(Http.CONNECTION, Http.CLOSE)
    }

    override fun setStartLine(line: String) {
        startLineDelegate.setStartLine(line)
    }

    override fun setVersion(version: String) {
        startLineDelegate.version = version
    }

    override fun setHeader(name: String, value: String) {
        headers.put(name, value)
    }

    override fun setHeaderLine(line: String) {
        val section = line.split(":", limit = 2)
        if (section.size < 2) {
            return
        }
        return setHeader(section[0].trim(), section[1].trim())
    }

    override fun getHeader(name: String): String? {
        return headers[name]
    }

    override fun getBody(): String? {
        return body ?: bodyBinary?.decode()?.also { body = it }
    }

    private fun ByteArray.decode(): String? {
        if (isEmpty()) return ""
        try {
            return newString()
        } catch (e: Exception) {
            // for bug in Android Sdk, ArrayIndexOutOfBoundsException may occur.
            Logger.w(e)
        }
        return null
    }

    override fun setBody(body: String?, withContentLength: Boolean) {
        setBodyInner(body, null, withContentLength)
    }

    override fun getBodyBinary(): ByteArray? {
        return bodyBinary
    }

    override fun setBodyBinary(body: ByteArray?, withContentLength: Boolean) {
        setBodyInner(null, body, withContentLength)
    }

    private fun setBodyInner(string: String?, binary: ByteArray?, withContentLength: Boolean) {
        body = string
        when {
            string == null ->
                bodyBinary = binary
            string.isEmpty() ->
                bodyBinary = ByteArray(0)
            else -> try {
                bodyBinary = getBytes(string)
            } catch (e: UnsupportedEncodingException) {
                Logger.w(e)
            }
        }
        if (withContentLength) {
            setHeader(Http.CONTENT_LENGTH, (bodyBinary?.size ?: 0).toString())
        }
    }

    // VisibleForTesting
    @Throws(UnsupportedEncodingException::class)
    internal fun getBytes(string: String): ByteArray {
        return string.toByteArray(CHARSET)
    }

    override fun getMessageString(): String {
        val body = body
        return getHeaderStringBuilder().also {
            if (!body.isNullOrEmpty()) {
                it.append(body)
            }
        }.toString()
    }

    @Throws(IOException::class)
    override fun writeData(outputStream: OutputStream) {
        outputStream.write(getHeaderBytes())
        bodyBinary?.let {
            if (isChunked) {
                writeChunkedBody(outputStream, it)
            } else {
                outputStream.write(it)
            }
        }
        outputStream.flush()
    }

    @Throws(IOException::class)
    private fun writeChunkedBody(outputStream: OutputStream, binary: ByteArray) {
        var offset = 0
        while (offset < binary.size) {
            val size = min(DEFAULT_CHUNK_SIZE, binary.size - offset)
            writeChunkSize(outputStream, size)
            outputStream.write(binary, offset, size)
            outputStream.write(CRLF)
            offset += size
        }
        writeChunkSize(outputStream, 0)
        outputStream.write(CRLF)
    }

    @Throws(IOException::class)
    private fun writeChunkSize(outputStream: OutputStream, size: Int) {
        outputStream.write(getBytes(Integer.toHexString(size)))
        outputStream.write(CRLF)
    }

    @Throws(IOException::class)
    override fun readData(inputStream: InputStream) {
        readStartLine(inputStream)
        readHeaders(inputStream)
        if (isChunked) {
            readChunkedBody(inputStream)
        } else {
            readBody(inputStream)
        }
    }

    @Throws(IOException::class)
    private fun readStartLine(inputStream: InputStream) {
        val startLine = readLine(inputStream)
        if (startLine.isEmpty()) {
            throw IOException("Illegal start line:$startLine")
        }
        try {
            setStartLine(startLine)
        } catch (e: IllegalArgumentException) {
            throw IOException("Illegal start line:$startLine")
        }
    }

    @Throws(IOException::class)
    private fun readHeaders(inputStream: InputStream) {
        while (true) {
            val line = readLine(inputStream)
            if (line.isEmpty()) {
                break
            }
            setHeaderLine(line)
        }
    }

    @Throws(IOException::class)
    private fun readBody(inputStream: InputStream) {
        bodyBinary = ByteArrayOutputStream().also {
            readInputStream(inputStream, it, contentLength)
        }.toByteArray()
    }

    @Throws(IOException::class)
    private fun readChunkedBody(inputStream: InputStream) {
        bodyBinary = ByteArrayOutputStream().also {
            while (true) {
                val length = readChunkSize(inputStream)
                if (length == 0) {
                    readLine(inputStream)
                    break
                }
                readInputStream(inputStream, it, length)
                readLine(inputStream)
            }
        }.toByteArray()
    }

    @Throws(IOException::class)
    private fun readInputStream(inputStream: InputStream, outputStream: OutputStream, length: Int) {
        val buffer = ByteArray(BUFFER_SIZE)
        var remain = length
        while (remain > 0) {
            val stroke = if (remain > buffer.size) buffer.size else remain
            val size = inputStream.read(buffer, 0, stroke)
            if (size < 0) {
                throw IOException("can't read from InputStream")
            }
            outputStream.write(buffer, 0, size)
            remain -= size
        }
    }

    @Throws(IOException::class)
    private fun readChunkSize(inputStream: InputStream): Int {
        val line = readLine(inputStream)
        if (line.isEmpty()) {
            throw IOException("Can not read chunk size!")
        }
        val chunkSize = line.split(";", limit = 2)[0]
        return chunkSize.toIntOrNull(16) ?: throw IOException("Chunk format error! $chunkSize")
    }

    override fun toString(): String {
        return getMessageString()
    }

    companion object {
        private const val BUFFER_SIZE = 1500
        private const val DEFAULT_CHUNK_SIZE = 1024
        private const val CR: Int = '\r'.toInt()
        private const val LF: Int = '\n'.toInt()
        private const val EOL: String = "\r\n"
        private val CRLF = byteArrayOf(CR.toByte(), LF.toByte())
        private val CHARSET = Charsets.UTF_8

        @Throws(IOException::class)
        private fun readLine(inputStream: InputStream): String {
            return ByteArrayOutputStream().also {
                while (true) {
                    val b = inputStream.read()
                    if (b < 0) {
                        if (it.size() == 0) throw IOException("can't read from InputStream")
                        break
                    }
                    if (b == LF) break
                    if (b == CR) continue
                    it.write(b)
                }
            }.toString(CHARSET.name())
        }
    }
}
