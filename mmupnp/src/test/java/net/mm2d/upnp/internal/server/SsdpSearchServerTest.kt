/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.common.Http
import net.mm2d.upnp.common.util.NetworkUtils
import net.mm2d.upnp.internal.message.SsdpRequest
import net.mm2d.upnp.internal.message.SsdpResponse
import net.mm2d.upnp.internal.thread.TaskExecutors
import net.mm2d.upnp.util.TestUtils
import net.mm2d.upnp.util.createInterfaceAddress
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.net.InetAddress

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class SsdpSearchServerTest {
    private lateinit var taskExecutors: TaskExecutors

    @Before
    fun setUp() {
        taskExecutors = TaskExecutors()
    }

    @After
    fun tearDown() {
        taskExecutors.terminate()
    }

    @Test
    fun search_ST_ALLでのサーチ() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val delegate = spyk(SsdpServerDelegate(taskExecutors, Address.IP_V4, networkInterface))
        delegate.setReceiver(mockk(relaxed = true))
        val interfaceAddress = createInterfaceAddress("192.0.2.2", "255.255.255.0", 16)
        every { delegate.interfaceAddress } returns interfaceAddress
        val socket = MockMulticastSocket()
        every { delegate.createMulticastSocket(any()) } returns socket
        val server = spyk(SsdpSearchServer(delegate))
        server.start()
        server.search()
        Thread.sleep(500)
        server.stop()

        val packet = socket.sendPacket!!
        val message = SsdpRequest.create(mockk(relaxed = true), packet.data, packet.length)
        assertThat(message.getMethod()).isEqualTo(SsdpMessage.M_SEARCH)
        assertThat(message.getHeader(Http.ST)).isEqualTo(SsdpSearchServer.ST_ALL)
        assertThat(message.getHeader(Http.MAN)).isEqualTo(SsdpMessage.SSDP_DISCOVER)
    }

    @Test
    fun search_ST_ROOTDEVICEでのサーチ() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val delegate = spyk(SsdpServerDelegate(taskExecutors, Address.IP_V4, networkInterface))
        delegate.setReceiver(mockk(relaxed = true))
        val interfaceAddress = createInterfaceAddress("192.0.2.2", "255.255.255.0", 16)
        every { delegate.interfaceAddress } returns interfaceAddress
        val socket = MockMulticastSocket()
        every { delegate.createMulticastSocket(any()) } returns socket
        val server = spyk(SsdpSearchServer(delegate))
        server.start()
        server.search(SsdpSearchServer.ST_ROOTDEVICE)
        Thread.sleep(500)
        server.stop()

        val packet = socket.sendPacket!!
        val message = SsdpRequest.create(mockk(relaxed = true), packet.data, packet.length)
        assertThat(message.getMethod()).isEqualTo(SsdpMessage.M_SEARCH)
        assertThat(message.getHeader(Http.ST)).isEqualTo(SsdpSearchServer.ST_ROOTDEVICE)
        assertThat(message.getHeader(Http.MAN)).isEqualTo(SsdpMessage.SSDP_DISCOVER)
    }

    @Test
    fun setResponseListener_受信メッセージが通知されること() {
        val delegate: SsdpServerDelegate = mockk(relaxed = true)
        delegate.setReceiver(mockk(relaxed = true))
        val interfaceAddress = createInterfaceAddress("192.0.2.2", "255.255.255.0", 16)
        every { delegate.interfaceAddress } returns interfaceAddress
        every { delegate.getLocalAddress() } returns interfaceAddress.address

        val server = spyk(SsdpSearchServer(delegate))
        val data = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin")
        val address = InetAddress.getByName("192.0.2.2")
        val slot = slot<SsdpResponse>()
        val listener: (SsdpMessage) -> Unit = mockk(relaxed = true)
        every { listener.invoke(capture(slot)) } answers { nothing }
        server.setResponseListener(listener)
        server.onReceive(address, data, data.size)

        val response = slot.captured
        assertThat(response.getStatus()).isEqualTo(Http.Status.HTTP_OK)
        assertThat(response.uuid).isEqualTo("uuid:01234567-89ab-cdef-0123-456789abcdef")
    }

    @Test
    fun onReceive_listenerがコールされる() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val server = SsdpSearchServer(taskExecutors, Address.IP_V4, networkInterface)
        val listener: (SsdpMessage) -> Unit = spyk()
        server.setResponseListener(listener)
        val address = InetAddress.getByName("192.0.2.2")
        val data = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin")

        server.onReceive(address, data, data.size)

        verify(exactly = 1) { listener.invoke(any()) }
    }

    @Test
    fun onReceive_filterでfalseになるとlistenerコールされない() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val server = SsdpSearchServer(taskExecutors, Address.IP_V4, networkInterface)
        val listener: (SsdpMessage) -> Unit = spyk()
        server.setResponseListener(listener)
        val address = InetAddress.getByName("192.0.2.2")
        val data = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin")
        server.setFilter { false }

        server.onReceive(address, data, data.size)

        verify(inverse = true) { listener.invoke(any()) }
    }

    @Test
    fun onReceive_アドレス不一致ならlistenerコールされない() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val server = SsdpSearchServer(taskExecutors, Address.IP_V4, networkInterface)
        val listener: (SsdpMessage) -> Unit = mockk(relaxed = true)
        server.setResponseListener(listener)
        val address = InetAddress.getByName("192.0.2.3")
        val data = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin")

        server.onReceive(address, data, data.size)

        verify(inverse = true) { listener.invoke(any()) }
    }

    @Test
    fun onReceive_sony_telepathy_serviceは無視する() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val server = SsdpSearchServer(taskExecutors, Address.IP_V4, networkInterface)
        val listener: (SsdpMessage) -> Unit = mockk(relaxed = true)
        server.setResponseListener(listener)
        val address = InetAddress.getByName("192.0.2.2")
        val data = TestUtils.getResourceAsByteArray("ssdp-search-response-telepathy.bin")

        server.onReceive(address, data, data.size)

        verify(inverse = true) { listener.invoke(any()) }
    }

    @Test
    fun onReceive_データに問題がある場合listenerコールされない() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val server = SsdpSearchServer(taskExecutors, Address.IP_V4, networkInterface)
        val listener: (SsdpMessage) -> Unit = mockk(relaxed = true)
        server.setResponseListener(listener)
        val address = InetAddress.getByName("192.0.2.2")
        val data = ByteArray(0)

        server.onReceive(address, data, data.size)

        verify(inverse = true) { listener.invoke(any()) }
    }

    @Test
    fun onReceive_listenerが登録されていなくてもクラッシュしない() {
        val networkInterface = NetworkUtils.getAvailableInet4Interfaces()[0]
        val server = SsdpSearchServer(taskExecutors, Address.IP_V4, networkInterface)

        val address = InetAddress.getByName("192.0.2.2")
        val data = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin")

        server.onReceive(address, data, data.size)
    }
}
