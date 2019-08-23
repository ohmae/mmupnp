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
import io.mockk.spyk
import io.mockk.verify
import net.mm2d.upnp.Http
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.internal.message.SsdpRequest
import net.mm2d.upnp.internal.thread.TaskExecutors
import net.mm2d.upnp.util.NetworkUtils
import net.mm2d.upnp.util.TestUtils
import net.mm2d.upnp.util.findInet4Address
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class SsdpServerDelegateTest {
    private lateinit var taskExecutors: TaskExecutors

    @Before
    fun setUp() {
        taskExecutors = TaskExecutors()
    }

    @After
    fun tearDown() {
        taskExecutors.terminate()
    }

    @Test(timeout = 10000L)
    fun start_stop_デッドロックしない() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val server = SsdpServerDelegate(taskExecutors, Address.IP_V4, networkInterface)
        server.setReceiver(mockk(relaxed = true))
        server.start()
        server.stop()
    }

    @Test(timeout = 10000L)
    fun stop_デッドロックしない() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val server = SsdpServerDelegate(taskExecutors, Address.IP_V4, networkInterface)
        server.setReceiver(mockk(relaxed = true))
        server.stop()
    }

    @Test
    fun getInterfaceAddress() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val server = spyk(SsdpServerDelegate(taskExecutors, Address.IP_V4, networkInterface))
        assertThat(server.interfaceAddress).isEqualTo(networkInterface.findInet4Address())
    }

    @Test(expected = IllegalStateException::class)
    fun start_without_open() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val server = spyk(SsdpServerDelegate(taskExecutors, Address.IP_V4, networkInterface))
        val socket: MulticastSocket = mockk(relaxed = true)
        every { server.createMulticastSocket(any()) } returns socket
        server.start()
    }

    @Test
    @Throws(IOException::class)
    fun send_open前は何も起こらない() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val server = spyk(SsdpServerDelegate(taskExecutors, Address.IP_V4, networkInterface))
        val socket: MulticastSocket = mockk(relaxed = true)
        every { server.createMulticastSocket(any()) } returns socket

        val message = SsdpRequest.create()
        message.setMethod(SsdpMessage.M_SEARCH)
        message.setUri("*")
        message.setHeader(Http.HOST, server.getSsdpAddressString())
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER)
        message.setHeader(Http.MX, "1")
        message.setHeader(Http.ST, SsdpSearchServer.ST_ROOTDEVICE)

        server.send { message }

        verify(inverse = true) { socket.send(any()) }
    }

    @Test
    @Throws(IOException::class)
    fun send_socketから送信される() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val server = spyk(SsdpServerDelegate(taskExecutors, Address.IP_V4, networkInterface))
        server.setReceiver { _, _, _ -> }
        val socket = spyk(MockMulticastSocket())
        every { server.createMulticastSocket(any()) } returns socket

        val message = SsdpRequest.create()
        message.setMethod(SsdpMessage.M_SEARCH)
        message.setUri("*")
        message.setHeader(Http.HOST, server.getSsdpAddressString())
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER)
        message.setHeader(Http.MX, "1")
        message.setHeader(Http.ST, SsdpSearchServer.ST_ROOTDEVICE)

        server.start()
        server.send { message }
        Thread.sleep(100)

        verify(exactly = 1) { socket.send(any()) }

        val packet = socket.sendPacket!!
        assertThat(packet.address).isEqualTo(server.getSsdpInetAddress())
        assertThat(packet.port).isEqualTo(ServerConst.SSDP_PORT)
        assertThat(String(packet.data)).isEqualTo(message.message.getMessageString())
    }

    @Test
    @Throws(IOException::class)
    fun send_socketでExceptionが発生したら無視する() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val server = spyk(SsdpServerDelegate(taskExecutors, Address.IP_V4, networkInterface))
        server.setReceiver(mockk())
        val socket: MulticastSocket = mockk(relaxed = true)
        every { server.createMulticastSocket(any()) } returns socket
        every { socket.send(any()) } throws IOException()

        val message = SsdpRequest.create()
        message.setMethod(SsdpMessage.M_SEARCH)
        message.setUri("*")
        message.setHeader(Http.HOST, server.getSsdpAddressString())
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER)
        message.setHeader(Http.MX, "1")
        message.setHeader(Http.ST, SsdpSearchServer.ST_ROOTDEVICE)

        server.start()
        server.send { message }
        server.stop()
    }

    @Test
    fun setNotifyListener_受信メッセージが通知されること() {
        val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val address = InetAddress.getByName("192.0.2.2")

        val socket = MockMulticastSocket()
        socket.setReceiveData(address, data, 0)
        val receiver: (InetAddress, ByteArray, Int) -> Unit = mockk(relaxed = true)
        val delegate = spyk(SsdpServerDelegate(taskExecutors, Address.IP_V4, networkInterface))
        delegate.setReceiver(receiver)
        every { delegate.createMulticastSocket(any()) } returns socket

        delegate.start()
        Thread.sleep(1900)
        delegate.stop()

        val packetData = ByteArray(1500)
        System.arraycopy(data, 0, packetData, 0, data.size)
        verify(exactly = 1) { receiver.invoke(address, packetData, data.size) }
    }

    @Test(timeout = 10000L)
    fun ReceiveTask_スレッド内の処理_port0() {
        val socket = spyk(object : MulticastSocket() {
            @Throws(IOException::class)
            override fun joinGroup(mcastaddr: InetAddress) {
            }

            @Throws(IOException::class)
            override fun leaveGroup(mcastaddr: InetAddress) {
            }

            @Synchronized
            @Throws(IOException::class)
            override fun receive(p: DatagramPacket) {
                try { // avoid busy loop
                    Thread.sleep(100)
                } catch (ignored: InterruptedException) {
                }
                p.address = InetAddress.getByName("192.168.0.1")
                p.data = ByteArray(1)
            }
        })
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val server = spyk(SsdpServerDelegate(taskExecutors, Address.IP_V4, networkInterface))
        server.setReceiver(mockk(relaxed = true))
        every { server.createMulticastSocket(any()) } returns socket

        server.start()
        Thread.sleep(500)
        server.stop()
        Thread.sleep(100)
        verify(exactly = 1) { server.receiveLoop(socket) }

        verify(inverse = true) { socket.joinGroup(any()) }
        verify(inverse = true) { socket.leaveGroup(any()) }
    }

    @Test(timeout = 10000L)
    fun ReceiveTask_スレッド内の処理_port_non0() {
        val socket = spyk(object : MulticastSocket() {
            @Throws(IOException::class)
            override fun joinGroup(mcastaddr: InetAddress) {
            }

            @Throws(IOException::class)
            override fun leaveGroup(mcastaddr: InetAddress) {
            }

            @Synchronized
            @Throws(IOException::class)
            override fun receive(p: DatagramPacket) {
                try { // avoid busy loop
                    Thread.sleep(100)
                } catch (ignored: InterruptedException) {
                }
                p.address = InetAddress.getByName("192.168.0.1")
                p.data = ByteArray(1)
            }
        })
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val server = spyk(SsdpServerDelegate(taskExecutors, Address.IP_V4, networkInterface, 10))
        server.setReceiver(mockk(relaxed = true))
        every { server.createMulticastSocket(any()) } returns socket

        server.start()
        Thread.sleep(500)
        server.stop()
        Thread.sleep(100)
        verify(exactly = 1) { server.receiveLoop(socket) }

        verify(exactly = 1) { socket.joinGroup(any()) }
        verify(exactly = 1) { socket.leaveGroup(any()) }
    }

    @Test(timeout = 10000L)
    fun ReceiveTask_receiveLoop_exceptionが発生してもループを続ける() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val server = spyk(SsdpServerDelegate(taskExecutors, Address.IP_V4, networkInterface))
        val receiver: (InetAddress, ByteArray, Int) -> Unit = mockk(relaxed = true)
        server.setReceiver(receiver)
        val socket = spyk(object : MulticastSocket() {
            private var count: Int = 0

            @Synchronized
            @Throws(IOException::class)
            override fun receive(p: DatagramPacket) {
                count++
                if (count == 1) {
                    throw SocketTimeoutException()
                }
                server.stop()
            }
        })
        every { server.createMulticastSocket(any()) } returns socket
        server.start()
        Thread.sleep(500)
        verify(exactly = 2) { socket.receive(any()) }
        verify(inverse = true) { receiver.invoke(any(), any(), any()) }
    }

    @Test(timeout = 10000L)
    fun ReceiveTask_run_exceptionが発生したらループを抜ける() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val server = spyk(SsdpServerDelegate(taskExecutors, Address.IP_V4, networkInterface))
        val socket: MulticastSocket = mockk(relaxed = true)
        every { socket.receive(any()) } throws IOException()
        every { server.createMulticastSocket(any()) } returns socket

        server.run()
    }
}
