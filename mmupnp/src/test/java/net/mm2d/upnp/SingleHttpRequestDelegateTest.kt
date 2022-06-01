/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import net.mm2d.upnp.SingleHttpRequest.StartLine
import net.mm2d.upnp.internal.message.SingleHttpMessageDelegate
import net.mm2d.upnp.internal.message.SingleHttpMessageDelegate.StartLineDelegate
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream
import java.io.OutputStream

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class SingleHttpRequestDelegateTest {
    private lateinit var delegate: SingleHttpMessageDelegate
    private lateinit var message: SingleHttpRequest

    @Before
    fun setUp() {
        val startLine: StartLineDelegate = mockk(relaxed = true)
        every { startLine.version } returns ""
        every { startLine.getStartLine() } returns ""
        delegate = spyk(SingleHttpMessageDelegate(startLine))
        message = SingleHttpRequest(StartLine(), delegate)
    }

    @Test
    fun getVersion() {
        message.version
        verify(exactly = 1) { delegate.version }
    }

    @Test
    fun setVersion() {
        val version = Http.HTTP_1_1
        message.setVersion(version)

        verify(exactly = 1) { delegate.setVersion(version) }
    }

    @Test
    fun setHeader() {
        val name = "name"
        val value = "value"
        message.setHeader(name, value)
        verify(exactly = 1) { delegate.setHeader(name, value) }
    }

    @Test
    fun setHeaderLine() {
        val line = "name: value"
        message.setHeaderLine(line)
        verify(exactly = 1) { delegate.setHeaderLine(line) }
    }

    @Test
    fun getHeader() {
        val name = "name"
        message.getHeader(name)
        verify(exactly = 1) { delegate.getHeader(name) }
    }

    @Test
    fun isChunked() {
        message.isChunked
        verify(exactly = 1) { delegate.isChunked }
    }

    @Test
    fun isKeepAlive() {
        message.isKeepAlive()
        verify(exactly = 1) { delegate.isKeepAlive() }
    }

    @Test
    fun getContentLength() {
        message.contentLength
        verify(exactly = 1) { delegate.contentLength }
    }

    @Test
    fun setBody() {
        val body = "body"
        message.setBody(body)
        verify(exactly = 1) { delegate.setBody(body) }
    }

    @Test
    fun setBody1() {
        val body = "body"
        message.setBody(body, true)
        verify(exactly = 1) { delegate.setBody(body, true) }
    }

    @Test
    fun setBodyBinary() {
        val body = ByteArray(0)
        message.setBodyBinary(body)
        verify(exactly = 1) { delegate.setBodyBinary(body) }
    }

    @Test
    fun setBodyBinary1() {
        val body = ByteArray(0)
        message.setBodyBinary(body, true)
        verify(exactly = 1) { delegate.setBodyBinary(body, true) }
    }

    @Test
    fun getBody() {
        message.getBody()
        verify(exactly = 1) { delegate.getBody() }
    }

    @Test
    fun getBodyBinary() {
        message.getBodyBinary()
        verify(exactly = 1) { delegate.getBodyBinary() }
    }

    @Test
    fun getMessageString() {
        message.getMessageString()
        verify(exactly = 1) { delegate.getMessageString() }
    }

    @Test
    fun writeData() {
        val outputStream: OutputStream = mockk(relaxed = true)
        message.writeData(outputStream)
        verify(exactly = 1) { delegate.writeData(outputStream) }
    }

    @Test
    fun readData() {
        val inputStream = ByteArrayInputStream(ByteArray(0))
        try {
            message.readData(inputStream)
        } catch (ignored: Exception) {
        }
        verify(exactly = 1) { delegate.readData(inputStream) }
    }

    @Test
    fun toString_() {
        message.toString()
    }
}
