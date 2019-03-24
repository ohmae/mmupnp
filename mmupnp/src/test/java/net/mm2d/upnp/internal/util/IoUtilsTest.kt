/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.util

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.Closeable
import java.io.IOException
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.Selector

@Suppress("NonAsciiCharacters", "TestFunctionName")
@RunWith(JUnit4::class)
class IoUtilsTest {
    @Test
    fun `closeQuietly Closeable closeがコールされる`() {
        val closeable = mockk<Closeable>(relaxed = true)
        closeable.closeQuietly()
        verify(exactly = 1) { closeable.close() }
    }

    @Test
    fun `closeQuietly Closeable nullを渡してもExceptionが発生しない`() {
        val closeable: Closeable? = null
        closeable.closeQuietly()
    }

    @Test
    fun `closeQuietly Closeable closeでIOExceptionが発生しても外に伝搬しない`() {
        val closeable = mockk<Closeable>(relaxed = true)
        every { closeable.close() }.throws(IOException())
        closeable.closeQuietly()
    }

    @Test
    fun `closeQuietly Socket closeがコールされる`() {
        val socket = mockk<Socket>(relaxed = true)
        socket.closeQuietly()
        verify(exactly = 1) { socket.close() }
    }

    @Test
    fun `closeQuietly Socket nullを渡してもExceptionが発生しない`() {
        val socket: Socket? = null
        socket.closeQuietly()
    }

    @Test
    fun `closeQuietly Socket closeでIOExceptionが発生しても外に伝搬しない`() {
        val socket = mockk<Socket>(relaxed = true)
        every { socket.close() }.throws(IOException())
        socket.closeQuietly()
    }

    @Test
    fun `closeQuietly DatagramSocket closeがコールされる`() {
        val datagramSocket = mockk<DatagramSocket>(relaxed = true)
        datagramSocket.closeQuietly()
        verify(exactly = 1) { datagramSocket.close() }
    }

    @Test
    fun `closeQuietly DatagramSocket nullを渡してもExceptionが発生しない`() {
        val datagramSocket: DatagramSocket? = null
        datagramSocket.closeQuietly()
    }

    @Test
    fun `closeQuietly ServerSocket closeがコールされる`() {
        val serverSocket = mockk<ServerSocket>(relaxed = true)
        serverSocket.closeQuietly()
        verify(exactly = 1) { serverSocket.close() }
    }

    @Test
    fun `closeQuietly ServerSocket nullを渡してもExceptionが発生しない`() {
        val serverSocket: ServerSocket? = null
        serverSocket.closeQuietly()
    }

    @Test
    fun `closeQuietly ServerSocket closeでIOExceptionが発生しても外に伝搬しない`() {
        val serverSocket = mockk<ServerSocket>(relaxed = true)
        every { serverSocket.close() }.throws(IOException())
        serverSocket.closeQuietly()
    }

    @Test
    fun `closeQuietly Selector closeがコールされる`() {
        val selector = mockk<Selector>(relaxed = true)
        selector.closeQuietly()
        verify(exactly = 1) { selector.close() }
    }

    @Test
    fun `closeQuietly Selector nullを渡してもExceptionが発生しない`() {
        val selector: Selector? = null
        selector.closeQuietly()
    }

    @Test
    fun `closeQuietly Selector closeでIOExceptionが発生しても外に伝搬しない`() {
        val selector = mockk<Selector>(relaxed = true)
        every { selector.close() }.throws(IOException())
        selector.closeQuietly()
    }
}
