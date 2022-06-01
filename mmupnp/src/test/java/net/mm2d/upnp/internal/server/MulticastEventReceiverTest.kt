/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.unmockkConstructor
import io.mockk.verify
import net.mm2d.upnp.Http
import net.mm2d.upnp.SingleHttpRequest
import net.mm2d.upnp.internal.thread.TaskExecutors
import net.mm2d.upnp.internal.thread.ThreadCondition
import net.mm2d.upnp.internal.util.closeQuietly
import net.mm2d.upnp.util.NetworkUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayOutputStream
import java.net.MulticastSocket
import java.net.SocketTimeoutException

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class MulticastEventReceiverTest {
    private lateinit var taskExecutors: TaskExecutors

    @Before
    fun setUp() {
        taskExecutors = TaskExecutors()
    }

    @After
    fun tearDown() {
        taskExecutors.terminate()
    }

    @Test(timeout = 20000L)
    fun `start stop デッドロックしない`() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val receiver = MulticastEventReceiver(taskExecutors, Address.IP_V4, networkInterface, mockk())
        receiver.start()
        receiver.stop()
    }

    @Test(timeout = 20000L)
    fun `stop デッドロックしない`() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val receiver = MulticastEventReceiver(taskExecutors, Address.IP_V4, networkInterface, mockk())
        receiver.stop()
    }

    @Test(timeout = 20000L)
    fun `run 正常動作`() {
        mockkConstructor(ThreadCondition::class)
        every { anyConstructed<ThreadCondition>().isCanceled() } returns false
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val receiver = spyk(MulticastEventReceiver(taskExecutors, Address.IP_V4, networkInterface, mockk()))
        val socket: MulticastSocket = mockk(relaxed = true)
        every { receiver.createMulticastSocket(any()) } returns socket
        every { receiver.receiveLoop(any()) } answers { nothing }
        receiver.run()
        verify {
            socket.joinGroup(any())
            receiver.receiveLoop(any())
            socket.leaveGroup(any())
            socket.closeQuietly()
        }

        unmockkConstructor(ThreadCondition::class)
    }

    @Test(timeout = 20000L)
    fun `run すでにcancel`() {
        mockkConstructor(ThreadCondition::class)
        every { anyConstructed<ThreadCondition>().isCanceled() } returns true
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val receiver = spyk(MulticastEventReceiver(taskExecutors, Address.IP_V4, networkInterface, mockk()))
        val socket: MulticastSocket = mockk(relaxed = true)
        every { receiver.createMulticastSocket(any()) } returns socket
        every { receiver.receiveLoop(any()) } answers { nothing }
        receiver.run()
        verify(inverse = true) {
            socket.joinGroup(any())
            receiver.receiveLoop(any())
            socket.leaveGroup(any())
            socket.closeQuietly()
        }

        unmockkConstructor(ThreadCondition::class)
    }

    @Test(timeout = 20000L)
    fun `receiveLoop 1ループ`() {
        mockkConstructor(ThreadCondition::class)
        every { anyConstructed<ThreadCondition>().isCanceled() } returns false
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val receiver = spyk(MulticastEventReceiver(taskExecutors, Address.IP_V4, networkInterface, mockk()))
        val socket: MulticastSocket = mockk(relaxed = true)
        every { receiver.onReceive(any(), any()) } answers {
            every { anyConstructed<ThreadCondition>().isCanceled() } returns true
        }

        receiver.receiveLoop(socket)

        verify(exactly = 1) { receiver.onReceive(any(), any()) }
        unmockkConstructor(ThreadCondition::class)
    }

    @Test(timeout = 20000L)
    fun `receiveLoop SocketTimeoutExceptionが発生しても次のループに入る`() {
        mockkConstructor(ThreadCondition::class)
        every { anyConstructed<ThreadCondition>().isCanceled() } returns false
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val receiver = spyk(MulticastEventReceiver(taskExecutors, Address.IP_V4, networkInterface, mockk()))
        val socket: MulticastSocket = mockk(relaxed = true)
        every { receiver.onReceive(any(), any()) } throws (SocketTimeoutException()) andThenAnswer {
            every { anyConstructed<ThreadCondition>().isCanceled() } returns true
        }

        receiver.receiveLoop(socket)

        verify(exactly = 2) { receiver.onReceive(any(), any()) }
        unmockkConstructor(ThreadCondition::class)
    }

    @Test(timeout = 20000L)
    fun `receiveLoop receiveの時点でcancelされればonReceiveはコールされない`() {
        mockkConstructor(ThreadCondition::class)
        every { anyConstructed<ThreadCondition>().isCanceled() } returns false
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val receiver = spyk(MulticastEventReceiver(taskExecutors, Address.IP_V4, networkInterface, mockk()))
        val socket: MulticastSocket = mockk(relaxed = true)
        every { socket.receive(any()) } throws (SocketTimeoutException()) andThenAnswer {
            every { anyConstructed<ThreadCondition>().isCanceled() } returns true
        }

        receiver.receiveLoop(socket)

        verify(exactly = 2) { socket.receive(any()) }
        verify(inverse = true) { receiver.onReceive(any(), any()) }
        unmockkConstructor(ThreadCondition::class)
    }

    @Test
    fun `onReceive 条件がそろわなければ通知されない`() {
        val listener: (uuid: String, svcid: String, lvl: String, seq: Long, properties: List<Pair<String, String>>) -> Unit =
            mockk(relaxed = true)
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val receiver = MulticastEventReceiver(taskExecutors, Address.IP_V4, networkInterface, listener)

        val request = SingleHttpRequest.create()
        request.setStartLine("NOTIFY * HTTP/1.0")

        request.toByteArray().also {
            receiver.onReceive(it, it.size)
        }
        verify(inverse = true) { listener.invoke(any(), any(), any(), any(), any()) }

        request.setHeader(Http.NT, Http.UPNP_EVENT)
        request.toByteArray().also {
            receiver.onReceive(it, it.size)
        }
        verify(inverse = true) { listener.invoke(any(), any(), any(), any(), any()) }

        request.setHeader(Http.NTS, Http.UPNP_PROPCHANGE)
        request.toByteArray().also {
            receiver.onReceive(it, it.size)
        }
        verify(inverse = true) { listener.invoke(any(), any(), any(), any(), any()) }

        request.setHeader(Http.LVL, "")
        request.toByteArray().also {
            receiver.onReceive(it, it.size)
        }
        verify(inverse = true) { listener.invoke(any(), any(), any(), any(), any()) }

        request.setHeader(Http.LVL, "upnp:/info")
        request.toByteArray().also {
            receiver.onReceive(it, it.size)
        }
        verify(inverse = true) { listener.invoke(any(), any(), any(), any(), any()) }

        request.setHeader(Http.SEQ, "hoge")
        request.toByteArray().also {
            receiver.onReceive(it, it.size)
        }
        verify(inverse = true) { listener.invoke(any(), any(), any(), any(), any()) }

        request.setHeader(Http.SEQ, "1")
        request.toByteArray().also {
            receiver.onReceive(it, it.size)
        }
        verify(inverse = true) { listener.invoke(any(), any(), any(), any(), any()) }

        request.setHeader(Http.SVCID, "")
        request.toByteArray().also {
            receiver.onReceive(it, it.size)
        }
        verify(inverse = true) { listener.invoke(any(), any(), any(), any(), any()) }

        request.setHeader(Http.SVCID, "svcid")
        request.toByteArray().also {
            receiver.onReceive(it, it.size)
        }
        verify(inverse = true) { listener.invoke(any(), any(), any(), any(), any()) }

        request.setHeader(Http.USN, "uuid:01234567-89ab-cdef-0123-456789abcdef")
        request.toByteArray().also {
            receiver.onReceive(it, it.size)
        }
        verify(inverse = true) { listener.invoke(any(), any(), any(), any(), any()) }

        request.setBody(
            "<e:propertyset xmlns:e=\"urn:schemas-upnp-org:event-1-0\">\n" +
                "<e:property>\n" +
                "<SystemUpdateID>0</SystemUpdateID>\n" +
                "</e:property>\n" +
                "<e:property>\n" +
                "<ContainerUpdateIDs></ContainerUpdateIDs>\n" +
                "</e:property>\n" +
                "</e:propertyset>", true
        )

        request.toByteArray().also {
            receiver.onReceive(it, it.size)
        }
        verify(exactly = 1) { listener.invoke(any(), any(), any(), any(), any()) }
    }

    private fun SingleHttpRequest.toByteArray(): ByteArray = ByteArrayOutputStream().also { writeData(it) }.toByteArray()
}
