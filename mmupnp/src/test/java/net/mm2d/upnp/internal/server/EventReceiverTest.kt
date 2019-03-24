/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import net.mm2d.upnp.Http
import net.mm2d.upnp.HttpRequest
import net.mm2d.upnp.HttpResponse
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
        val notify = HttpRequest.create().apply {
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
    fun terminate() {
        taskExecutors.terminate()
    }

    @Test(timeout = 10000L)
    fun open_close_デッドロックしない() {
        val receiver = EventReceiver(mockk(relaxed = true), null)
        receiver.start()
        receiver.stop()
    }

    @Test(timeout = 1000L)
    fun close_open前なら即終了() {
        val receiver = EventReceiver(mockk(), null)
        receiver.stop()
    }

    @Test
    fun getLocalPort() {
        val port = 12345
        val serverSocket: ServerSocket = mockk(relaxed = true)
        every { serverSocket.localPort } returns port
        every { serverSocket.accept() } throws IOException()
        val receiver = spyk(EventReceiver(taskExecutors, null))
        every { receiver.createServerSocket() } returns serverSocket

        receiver.start()
        assertThat(receiver.getLocalPort()).isEqualTo(port)
        receiver.stop()
    }

    @Test
    fun getLocalPort_開始前は0() {
        val receiver = EventReceiver(mockk(), null)
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
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                }
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
        Thread.sleep(10)

        assertThat(firstSlot.captured).isEqualTo(SID)
        assertThat(secondSlot.captured).isEqualTo(0L)
        assertThat(thirdSlot.captured).contains("SystemUpdateID" to "0")
        assertThat(thirdSlot.captured).contains("ContainerUpdateIDs" to "")

        val response = HttpResponse.create()
        response.readData(ByteArrayInputStream(baos.toByteArray()))
        assertThat(response.status).isEqualTo(Http.Status.HTTP_OK)
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
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                }
                throw IOException()
            }
        }
        val receiver = spyk(EventReceiver(taskExecutors) { _, _, _ -> false })
        every { receiver.createServerSocket() } returns serverSocket

        receiver.start()
        Thread.sleep(500)
        receiver.stop()
        Thread.sleep(100)

        val response = HttpResponse.create()
        response.readData(ByteArrayInputStream(baos.toByteArray()))
        assertThat(response.status).isEqualTo(Http.Status.HTTP_PRECON_FAILED)
    }

    @Test(timeout = 10000L)
    fun onEventReceived_Failedが返る2() {
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
        val receiver = spyk(EventReceiver(taskExecutors, null))
        every { receiver.createServerSocket() } returns serverSocket
        receiver.start()
        Thread.sleep(100)
        receiver.stop()
        Thread.sleep(10)

        val response = HttpResponse.create()
        response.readData(ByteArrayInputStream(baos.toByteArray()))
        assertThat(response.status).isEqualTo(Http.Status.HTTP_PRECON_FAILED)
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
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                }
                throw IOException()
            }
        }
        val receiver = spyk(EventReceiver(taskExecutors, null))
        every { receiver.createServerSocket() } returns serverSocket
        receiver.start()
        Thread.sleep(100)
        receiver.stop()
        Thread.sleep(10)

        val response = HttpResponse.create()
        response.readData(ByteArrayInputStream(baos.toByteArray()))
        assertThat(response.status).isEqualTo(Http.Status.HTTP_BAD_REQUEST)
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
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
            }
            false
        })
        every { receiver.createServerSocket() } returns serverSocket

        receiver.start()
        Thread.sleep(100)
        receiver.stop()
        Thread.sleep(10)
    }

    @Test
    fun parsePropertyPairs_中身が空なら空のリスト() {
        val request = HttpRequest.create()

        assertThat(EventReceiver.parsePropertyPairs(request)).isEmpty()
    }

    @Test
    fun parsePropertyPairs_rootがpropertysetでない場合リスト() {
        val request = HttpRequest.create()
        request.setBody("<e:property xmlns:e=\"urn:schemas-upnp-org:event-1-0\">\n" +
                "<e:property>\n" +
                "<SystemUpdateID>0</SystemUpdateID>\n" +
                "</e:property>\n" +
                "<e:property>\n" +
                "<ContainerUpdateIDs></ContainerUpdateIDs>\n" +
                "</e:property>\n" +
                "</e:property>", true)

        assertThat(EventReceiver.parsePropertyPairs(request)).isEmpty()
    }

    @Test
    fun parsePropertyPairs_property以外の要素は無視() {
        val request = HttpRequest.create()
        request.setBody("<e:propertyset xmlns:e=\"urn:schemas-upnp-org:event-1-0\">\n" +
                "<e:property>\n" +
                "<SystemUpdateID>0</SystemUpdateID>\n" +
                "</e:property>\n" +
                "<e:proper>\n" +
                "<ContainerUpdateIDs></ContainerUpdateIDs>\n" +
                "</e:proper>\n" +
                "</e:propertyset>", true)

        assertThat(EventReceiver.parsePropertyPairs(request)).hasSize(1)
    }

    @Test
    fun parsePropertyPairs_xml異常() {
        val request = HttpRequest.create()
        request.setBody("<e:propertyset xmlns:e=\"urn:schemas-upnp-org:event-1-0\">\n" +
                "<e:property>\n" +
                "<>0</>\n" +
                "</e:property>\n" +
                "<e:property>\n" +
                "<ContainerUpdateIDs></ContainerUpdateIDs>\n" +
                "</e:property>\n" +
                "</e:propertyset>", true)

        assertThat(EventReceiver.parsePropertyPairs(request)).isEmpty()
    }

    @Test
    fun ServerTask_notifyEvent_空ならfalse() {
        val sid = "sid"
        val listener: (String, Long, List<Pair<String, String>>) -> Boolean = mockk(relaxed = true)
        val receiver = spyk(EventReceiver(taskExecutors, listener))

        val request = HttpRequest.create()
        every { listener.invoke(any(), any(), any()) } returns true

        assertThat(receiver.notifyEvent(sid, request)).isFalse()
        verify(inverse = true) { listener.invoke(any(), any(), any()) }
    }

    @Test
    fun ServerTask_notifyEvent_listenerの戻り値と等しい() {
        val sid = "sid"
        val listener: (String, Long, List<Pair<String, String>>) -> Boolean = mockk(relaxed = true)
        val receiver = spyk(EventReceiver(taskExecutors, listener))

        val request = HttpRequest.create().apply {
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
    fun ServerTask_notifyEvent_listenerがなければfalse() {
        val sid = "sid"
        val receiver = spyk(EventReceiver(taskExecutors, null))
        val request = HttpRequest.create().apply {
            setHeader(Http.SEQ, "0")
            setBody(TestUtils.getResourceAsString("propchange.xml"), true)
        }

        assertThat(receiver.notifyEvent(sid, request)).isFalse()
    }

    @Test
    fun ClientTask_run() {
        val socket: Socket = mockk(relaxed = true)
        every { socket.getInputStream() } returns mockk(relaxed = true)
        every { socket.getOutputStream() } returns mockk(relaxed = true)
        val receiver = spyk(EventReceiver(taskExecutors, null))
        val clientTask = spyk(ClientTask(receiver, socket))
        every { clientTask.receiveAndReply(any(), any()) } throws IOException()

        clientTask.run()

        verify(exactly = 1) { receiver.notifyClientFinished(clientTask) }
    }

    companion object {
        private const val SID = "uuid:s1234567-89ab-cdef-0123-456789abcdef"
    }
}
