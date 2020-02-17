/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.server

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import net.mm2d.upnp.common.Http
import net.mm2d.upnp.common.SsdpMessage
import net.mm2d.upnp.common.internal.message.SsdpRequest
import net.mm2d.upnp.common.internal.server.Address
import net.mm2d.upnp.common.internal.server.Address.IP_V4
import net.mm2d.upnp.common.internal.server.SsdpServerDelegate
import net.mm2d.upnp.common.internal.thread.TaskExecutors
import net.mm2d.upnp.common.util.NetworkUtils
import net.mm2d.upnp.util.TestUtils
import net.mm2d.upnp.util.createInterfaceAddress
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException
import java.net.InetAddress

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class SsdpNotifyReceiverTest {
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
    fun setNotifyListener_受信メッセージが通知されること() {
        val delegate =
            spyk(SsdpServerDelegate(taskExecutors, IP_V4, NetworkUtils.getAvailableInet4Interfaces()[0]))
        delegate.setReceiver(mockk(relaxed = true))
        val interfaceAddress = createInterfaceAddress("192.0.2.2", "255.255.255.0", 16)
        every { delegate.interfaceAddress } returns interfaceAddress
        val receiver = spyk(SsdpNotifyReceiver(delegate))

        val slot = slot<SsdpRequest>()
        val listener: (SsdpMessage) -> Unit = mockk()
        every { listener.invoke(capture(slot)) } answers { nothing }
        receiver.setNotifyListener(listener)

        val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")
        val address = InetAddress.getByName("192.0.2.2")
        receiver.onReceive(address, 0, data, data.size)

        assertThat(slot.captured.uuid).isEqualTo("uuid:01234567-89ab-cdef-0123-456789abcdef")
    }

    @Test
    fun onReceive_同一セグメントからのメッセージは通知する() {
        val receiver =
            spyk(SsdpNotifyReceiver(taskExecutors, Address.IP_V4, NetworkUtils.getAvailableInet4Interfaces()[0]))
        val address = createInterfaceAddress("192.0.2.1", "255.255.0.0", 24)
        every { receiver.interfaceAddress } returns address
        val listener: (SsdpMessage) -> Unit = spyk { }
        receiver.setNotifyListener(listener)
        val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")

        receiver.onReceive(InetAddress.getByName("192.0.2.2"), 0, data, data.size)

        verify(exactly = 1) { listener.invoke(any()) }
    }

    @Test
    fun onReceive_filterでfalseになるとlistenerコールされない() {
        val receiver =
            spyk(SsdpNotifyReceiver(taskExecutors, Address.IP_V4, NetworkUtils.getAvailableInet4Interfaces()[0]))
        val address = createInterfaceAddress("192.0.2.1", "255.255.0.0", 24)
        every { receiver.interfaceAddress } returns address
        val listener: (SsdpMessage) -> Unit = spyk { }
        receiver.setNotifyListener(listener)
        val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")
        receiver.setFilter { false }

        receiver.onReceive(InetAddress.getByName("192.0.2.2"), 0, data, data.size)

        verify(inverse = true) { listener.invoke(any()) }
    }

    @Test
    fun onReceive_Listenerがnullでもクラッシュしない() {
        val receiver =
            spyk(SsdpNotifyReceiver(taskExecutors, Address.IP_V4, NetworkUtils.getAvailableInet4Interfaces()[0]))
        val address = createInterfaceAddress("192.0.2.1", "255.255.0.0", 24)
        every { receiver.interfaceAddress } returns address
        val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")

        receiver.onReceive(InetAddress.getByName("192.0.2.2"), 0, data, data.size)
    }

    @Test
    fun onReceive_異なるセグメントからのメッセージは無視する() {
        val receiver =
            spyk(SsdpNotifyReceiver(taskExecutors, Address.IP_V4, NetworkUtils.getAvailableInet4Interfaces()[0]))
        val address = createInterfaceAddress("192.0.2.1", "255.255.0.0", 24)
        every { receiver.interfaceAddress } returns address
        val listener: (SsdpMessage) -> Unit = spyk { }
        receiver.setNotifyListener(listener)
        receiver.setSegmentCheckEnabled(true)
        val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")

        receiver.onReceive(InetAddress.getByName("192.1.2.2"), 0, data, data.size)

        verify(inverse = true) { listener.invoke(any()) }
    }

    @Test
    fun onReceive_M_SEARCHパケットは無視する() {
        val receiver =
            spyk(SsdpNotifyReceiver(taskExecutors, Address.IP_V4, NetworkUtils.getAvailableInet4Interfaces()[0]))
        val address = createInterfaceAddress("192.0.2.1", "255.255.0.0", 24)
        every { receiver.interfaceAddress } returns address
        val listener: (SsdpMessage) -> Unit = spyk { }
        receiver.setNotifyListener(listener)

        val message = SsdpRequest.create()
        message.setMethod(Http.M_SEARCH)
        message.setUri("*")
        message.setHeader(Http.HOST, Address.IP_V4.ssdpAddressString)
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER)
        message.setHeader(Http.MX, "1")
        message.setHeader(Http.ST, SsdpSearchServer.ST_ALL)
        val data = message.message.getMessageString().toByteArray()

        receiver.onReceive(InetAddress.getByName("192.0.2.2"), 0, data, data.size)

        verify(inverse = true) { listener.invoke(any()) }
    }

    @Test
    fun onReceive_ByeByeパケットは通知する() {
        val receiver =
            spyk(SsdpNotifyReceiver(taskExecutors, Address.IP_V4, NetworkUtils.getAvailableInet4Interfaces()[0]))
        val address = createInterfaceAddress("192.0.2.1", "255.255.0.0", 24)
        every { receiver.interfaceAddress } returns address
        val listener: (SsdpMessage) -> Unit = spyk { }
        receiver.setNotifyListener(listener)

        val data = TestUtils.getResourceAsByteArray("ssdp-notify-byebye0.bin")

        receiver.onReceive(InetAddress.getByName("192.0.2.2"), 0, data, data.size)

        verify(exactly = 1) { listener.invoke(any()) }
    }

    @Test
    fun onReceive_LocationとSourceが不一致のメッセージは無視する() {
        val receiver =
            spyk(SsdpNotifyReceiver(taskExecutors, Address.IP_V4, NetworkUtils.getAvailableInet4Interfaces()[0]))
        val address = createInterfaceAddress("192.0.2.1", "255.255.0.0", 24)
        every { receiver.interfaceAddress } returns address
        val listener: (SsdpMessage) -> Unit = spyk { }
        receiver.setNotifyListener(listener)
        val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")

        receiver.onReceive(InetAddress.getByName("192.0.2.3"), 0, data, data.size)

        verify(inverse = true) { listener.invoke(any()) }
    }

    @Test
    fun onReceive_sony_telepathy_serviceは無視する() {
        val receiver =
            spyk(SsdpNotifyReceiver(taskExecutors, Address.IP_V4, NetworkUtils.getAvailableInet4Interfaces()[0]))
        val address = createInterfaceAddress("192.0.2.1", "255.255.0.0", 24)
        every { receiver.interfaceAddress } returns address
        val listener: (SsdpMessage) -> Unit = spyk { }
        receiver.setNotifyListener(listener)
        val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive-telepathy.bin")

        receiver.onReceive(InetAddress.getByName("192.0.2.2"), 0, data, data.size)

        verify(inverse = true) { listener.invoke(any()) }
    }

    @Test
    fun onReceive_IOExceptionが発生してもクラッシュしない() {
        val receiver =
            spyk(SsdpNotifyReceiver(taskExecutors, Address.IP_V4, NetworkUtils.getAvailableInet4Interfaces()[0]))
        val address = createInterfaceAddress("192.0.2.1", "255.255.0.0", 24)
        every { receiver.interfaceAddress } returns address
        every { receiver.createSsdpRequestMessage(any(), any()) } throws IOException()
        val listener: (SsdpMessage) -> Unit = spyk { }
        receiver.setNotifyListener(listener)
        val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")

        receiver.onReceive(InetAddress.getByName("192.0.2.2"), 0, data, data.size)

        verify(inverse = true) { listener.invoke(any()) }
    }

    @Test
    fun send_delegateがコールされる() {
        val delegate: SsdpServerDelegate = mockk(relaxed = true)
        val receiver = spyk(SsdpNotifyReceiver(delegate))
        val message: SsdpMessage = mockk(relaxed = true)

        receiver.send { message }

        verify(exactly = 1) { delegate.send(any()) }
    }
}
