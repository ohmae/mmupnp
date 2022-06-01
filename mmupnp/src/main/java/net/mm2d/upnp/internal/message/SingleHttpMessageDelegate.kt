/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message

import net.mm2d.upnp.Http
import net.mm2d.upnp.SingleHttpMessage
import net.mm2d.upnp.log.Logger
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException

/**
 * Common implementation of [SingleHttpMessage].
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SingleHttpMessageDelegate(
    private val startLineDelegate: StartLineDelegate,
    original: SingleHttpMessageDelegate? = null
) : SingleHttpMessage {
    internal interface StartLineDelegate {
        var version: String
        val shouldStriveToReadBody: Boolean
        fun getStartLine(): String
        fun setStartLine(startLine: String)
    }

    private val headers: SingleHttpHeaders
    private var body: String? = null
    private var bodyBinary: ByteArray? = null

    init {
        if (original == null) {
            headers = SingleHttpHeaders()
        } else {
            headers = SingleHttpHeaders(original.headers)
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
        get() = headers[Http.CONTENT_LENGTH]?.toIntOrNull() ?: -1

    // VisibleForTesting
    @Throws(UnsupportedEncodingException::class)
    fun ByteArray.newString(): String = toString(Charsets.UTF_8)

    private fun getHeaderString(): String = getHeaderStringBuilder().toString()

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

    override fun setStartLine(line: String): Unit = startLineDelegate.setStartLine(line)

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

    override fun getBodyBinary(): ByteArray? = bodyBinary

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
    internal fun getBytes(string: String): ByteArray = string.toByteArray()

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
                outputStream.writeChunkedBody(it)
            } else {
                outputStream.write(it)
            }
        }
        outputStream.flush()
    }

    @Throws(IOException::class)
    private fun OutputStream.writeChunkedBody(binary: ByteArray) {
        var offset = 0
        while (offset < binary.size) {
            val size = minOf(DEFAULT_CHUNK_SIZE, binary.size - offset)
            writeChunkSize(size)
            write(binary, offset, size)
            write(CRLF)
            offset += size
        }
        writeChunkSize(0)
        write(CRLF)
    }

    @Throws(IOException::class)
    private fun OutputStream.writeChunkSize(size: Int) {
        write(getBytes(Integer.toHexString(size)))
        write(CRLF)
    }

    @Throws(IOException::class)
    override fun readData(inputStream: InputStream) {
        inputStream.run {
            readStartLine()
            readHeaders()
            if (isChunked) {
                readChunkedBody()
            } else {
                readBody()
            }
        }
    }

    @Throws(IOException::class)
    private fun InputStream.readStartLine() {
        val startLine = readLine()
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
    private fun InputStream.readHeaders() {
        while (true) {
            val line = readLine()
            if (line.isEmpty()) {
                break
            }
            setHeaderLine(line)
        }
    }

    private fun shouldStriveToReadBody() = !isKeepAlive() && startLineDelegate.shouldStriveToReadBody

    @Throws(IOException::class)
    private fun InputStream.readBody() {
        val length = contentLength
        bodyBinary = if (length < 0 && shouldStriveToReadBody()) {
            readBytes()
        } else if (length <= 0) {
            byteArrayOf()
        } else {
            readBytes(length)
        }
    }

    @Throws(IOException::class)
    private fun InputStream.readChunkedBody() {
        bodyBinary = toOutputStream {
            while (true) {
                val length = readChunkSize()
                if (length == 0) {
                    readLine()
                    break
                }
                copyTo(it, length)
                readLine()
            }
        }.toByteArray()
    }

    override fun toString(): String = getMessageString()

    companion object {
        private const val DEFAULT_CHUNK_SIZE = 1024
        private const val BUFFER_SIZE = 1500
        private const val CR: Int = '\r'.code
        private const val LF: Int = '\n'.code
        private const val EOL: String = "\r\n"
        private val CRLF = EOL.toByteArray()

        @Throws(IOException::class)
        private inline fun InputStream.toOutputStream(
            block: InputStream.(ByteArrayOutputStream) -> Unit
        ): ByteArrayOutputStream = ByteArrayOutputStream(maxOf(available(), BUFFER_SIZE)).also { block(it) }

        @Throws(IOException::class)
        private fun InputStream.copyTo(out: OutputStream, requestBytes: Int) {
            val buffer = ByteArray(BUFFER_SIZE)
            var remain = requestBytes
            while (remain > 0) {
                val stroke = if (remain > buffer.size) buffer.size else remain
                val size = read(buffer, 0, stroke)
                if (size < 0) {
                    throw IOException("can't read from InputStream: ${requestBytes - remain} / $requestBytes")
                }
                out.write(buffer, 0, size)
                remain -= size
            }
        }

        @Throws(IOException::class)
        private fun InputStream.readLine(): String = toOutputStream {
            while (true) {
                val b = read()
                if (b < 0) {
                    if (it.size() == 0) throw IOException("can't read from InputStream")
                    break
                }
                if (b == LF) break
                if (b == CR) continue
                it.write(b)
            }
        }.toString(Charsets.UTF_8.name())

        @Throws(IOException::class)
        private fun InputStream.readBytes(length: Int): ByteArray = toOutputStream { copyTo(it, length) }.toByteArray()

        @Throws(IOException::class)
        private fun InputStream.readChunkSize(): Int {
            val line = readLine()
            if (line.isEmpty()) {
                throw IOException("Can not read chunk size!")
            }
            val chunkSize = line.split(";", limit = 2)[0]
            return chunkSize.toIntOrNull(16) ?: throw IOException("Chunk format error! $chunkSize")
        }
    }
}
