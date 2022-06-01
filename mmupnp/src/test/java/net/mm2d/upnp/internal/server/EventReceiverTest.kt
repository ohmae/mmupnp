/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import net.mm2d.upnp.Http
import net.mm2d.upnp.SingleHttpRequest
import net.mm2d.upnp.SingleHttpResponse
import net.mm2d.upnp.internal.server.EventReceiver.ClientTask
import net.mm2d.upnp.internal.thread.TaskExecutors
import net.mm2d.upnp.util.TestUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class EventReceiverTest {
    private lateinit var badRequest: ByteArray
    private lateinit var failRequest: ByteArray
    private lateinit var notifyRequest: ByteArray
    private lateinit var taskExecutors: TaskExecutors

    @Before
    fun setUp() {
        val notify = SingleHttpRequest.create().apply {
            setMethod(Http.NOTIFY)
            setUri("/")
            setHeader(Http.CONNECTION, Http.CLOSE)
            setHeader(Http.SEQ, "0")
            setBody(TestUtils.getResourceAsString("propchange.xml"), true)
        }
        val baos = ByteArrayOutputStream()
        notify.writeData(baos)
        badRequest = baos.toByteArray()
        baos.reset()

        notify.setHeader(Http.NT, Http.UPNP_EVENT)
        notify.setHeader(Http.NTS, Http.UPNP_PROPCHANGE)
        notify.writeData(baos)
        failRequest = baos.toByteArray()
        baos.reset()

        notify.setHeader(Http.SID, SID)
        notify.writeData(baos)
        notifyRequest = baos.toByteArray()
        taskExecutors = TaskExecutors()
    }

    @After
    fun tearDown() {
        taskExecutors.terminate()
    }

    @Test(timeout = 10000L)
    fun open_close_デッドロックしない() {
        val receiver = EventReceiver(mockk(relaxed = true), mockk())
        receiver.start()
        receiver.stop()
    }

    @Test(timeout = 10000L)
    fun close_open前なら即終了() {
        val receiver = EventReceiver(mockk(relaxed = true), mockk())
        receiver.stop()
    }

    @Test
    fun getLocalPort() {
        val port = 12345
        val serverSocket: ServerSocket = mockk(relaxed = true)
        every { serverSocket.localPort } returns port
        every { serverSocket.accept() } throws IOException()
        val receiver = spyk(EventReceiver(taskExecutors, mockk()))
        every { receiver.createServerSocket() } returns serverSocket

        receiver.start()
        assertThat(receiver.getLocalPort()).isEqualTo(port)
        receiver.stop()
    }

    @Test
    fun getLocalPort_開始前は0() {
        val receiver = EventReceiver(mockk(relaxed = true), mockk())
        assertThat(receiver.getLocalPort()).isEqualTo(0)
    }

    @Test(timeout = 10000L)
    fun onEventReceived_イベントの値が取得できること() {
        val baos = ByteArrayOutputStream()
        val bais = ByteArrayInputStream(notifyRequest)
        val socket: Socket = mockk(relaxed = true)
        every { socket.getOutputStream() } returns baos
        every { socket.getInputStream() } returns bais
        val serverSocket: ServerSocket = mockk(relaxed = true)
        var latch = false
        every { serverSocket.accept() } answers {
            if (!latch) {
                latch = true
                socket
            } else {
                runCatching { Thread.sleep(1000) }
                throw IOException()
            }
        }
        val firstSlot = slot<String>()
        val secondSlot = slot<Long>()
        val thirdSlot = slot<List<Pair<String, String>>>()
        val listener: (String, Long, List<Pair<String, String>>) -> Boolean = mockk()
        every { listener.invoke(capture(firstSlot), capture(secondSlot), capture(thirdSlot)) } returns true
        val receiver = spyk(EventReceiver(taskExecutors, listener))
        every { receiver.createServerSocket() } returns serverSocket

        receiver.start()
        Thread.sleep(100)
        receiver.stop()
        Thread.sleep(100)

        assertThat(firstSlot.captured).isEqualTo(SID)
        assertThat(secondSlot.captured).isEqualTo(0L)
        assertThat(thirdSlot.captured).contains("SystemUpdateID" to "0")
        assertThat(thirdSlot.captured).contains("ContainerUpdateIDs" to "")

        val response = SingleHttpResponse.create()
        response.readData(ByteArrayInputStream(baos.toByteArray()))
        assertThat(response.getStatus()).isEqualTo(Http.Status.HTTP_OK)
    }

    @Test(timeout = 10000L)
    fun onEventReceived_Failedが返る1() {
        val baos = ByteArrayOutputStream()
        val bais = ByteArrayInputStream(failRequest)
        val socket: Socket = mockk(relaxed = true)
        every { socket.getOutputStream() } returns baos
        every { socket.getInputStream() } returns bais
        val serverSocket: ServerSocket = mockk(relaxed = true)
        var latch = false
        every { serverSocket.accept() } answers {
            if (!latch) {
                latch = true
                socket
            } else {
                runCatching { Thread.sleep(1000) }
                throw IOException()
            }
        }
        val receiver = spyk(EventReceiver(taskExecutors) { _, _, _ -> false })
        every { receiver.createServerSocket() } returns serverSocket

        receiver.start()
        Thread.sleep(500)
        receiver.stop()
        Thread.sleep(100)

        val response = SingleHttpResponse.create()
        response.readData(ByteArrayInputStream(baos.toByteArray()))
        assertThat(response.getStatus()).isEqualTo(Http.Status.HTTP_PRECON_FAILED)
    }

    @Test(timeout = 10000L)
    fun onEventReceived_BadRequestが返る() {
        val baos = ByteArrayOutputStream()
        val bais = ByteArrayInputStream(badRequest)
        val socket: Socket = mockk(relaxed = true)
        every { socket.getOutputStream() } returns baos
        every { socket.getInputStream() } returns bais
        val serverSocket: ServerSocket = mockk(relaxed = true)
        var latch = false
        every { serverSocket.accept() } answers {
            if (!latch) {
                latch = true
                socket
            } else {
                runCatching { Thread.sleep(1000) }
                throw IOException()
            }
        }
        val receiver = spyk(EventReceiver(taskExecutors, mockk()))
        every { receiver.createServerSocket() } returns serverSocket
        receiver.start()
        Thread.sleep(100)
        receiver.stop()
        Thread.sleep(100)

        val response = SingleHttpResponse.create()
        response.readData(ByteArrayInputStream(baos.toByteArray()))
        assertThat(response.getStatus()).isEqualTo(Http.Status.HTTP_BAD_REQUEST)
    }

    @Test(timeout = 10000L)
    fun stop() {
        val baos = ByteArrayOutputStream()
        val bais = ByteArrayInputStream(notifyRequest)
        val socket: Socket = mockk(relaxed = true)
        every { socket.getOutputStream() } returns baos
        every { socket.getInputStream() } returns bais
        val serverSocket: ServerSocket = mockk(relaxed = true)
        var latch = false
        every { serverSocket.accept() } answers {
            if (!latch) {
                latch = true
                socket
            } else {
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                }
                throw IOException()
            }
        }
        val receiver = spyk(EventReceiver(taskExecutors) { _, _, _ ->
            runCatching { Thread.sleep(1000) }
            false
        })
        every { receiver.createServerSocket() } returns serverSocket

        receiver.start()
        Thread.sleep(100)
        receiver.stop()
        Thread.sleep(100)
    }

    @Test
    fun ServerTask_notifyEvent_空ならfalse() {
        val sid = "sid"
        val listener: (String, Long, List<Pair<String, String>>) -> Boolean = mockk(relaxed = true)
        val receiver = spyk(EventReceiver(taskExecutors, listener))

        val request = SingleHttpRequest.create()
        every { listener.invoke(any(), any(), any()) } returns true

        assertThat(receiver.notifyEvent(sid, request)).isFalse()
        verify(inverse = true) { listener.invoke(any(), any(), any()) }
    }

    @Test
    fun ServerTask_notifyEvent_listenerの戻り値と等しい() {
        val sid = "sid"
        val listener: (String, Long, List<Pair<String, String>>) -> Boolean = mockk(relaxed = true)
        val receiver = spyk(EventReceiver(taskExecutors, listener))

        val request = SingleHttpRequest.create().apply {
            setHeader(Http.SEQ, "0")
            setBody(TestUtils.getResourceAsString("propchange.xml"), true)
        }

        every { listener.invoke(any(), any(), any()) } returns true

        assertThat(receiver.notifyEvent(sid, request)).isTrue()

        verify(exactly = 1) { listener.invoke(any(), any(), any()) }

        every { listener.invoke(any(), any(), any()) } returns false

        assertThat(receiver.notifyEvent(sid, request)).isFalse()
        verify(exactly = 2) { listener.invoke(any(), any(), any()) }
    }

    @Test
    fun ClientTask_run() {
        val socket: Socket = mockk(relaxed = true)
        every { socket.getInputStream() } returns mockk(relaxed = true)
        every { socket.getOutputStream() } returns mockk(relaxed = true)
        val receiver = spyk(EventReceiver(taskExecutors, mockk()))
        val clientTask = spyk(ClientTask(taskExecutors, receiver, socket))
        every { clientTask.receiveAndReply(any(), any()) } throws IOException()

        clientTask.run()

        verify(exactly = 1) { receiver.notifyClientFinished(clientTask) }
    }

    companion object {
        private const val SID = "uuid:s1234567-89ab-cdef-0123-456789abcdef"
    }
}
