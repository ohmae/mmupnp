/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import net.mm2d.upnp.Protocol
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.util.isAvailableInet4Interface
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.net.NetworkInterface

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class SsdpNotifyReceiverListTest {
    private lateinit var nif: NetworkInterface

    @Before
    fun setUp() {
        nif = mockk(relaxed = true)
        mockkStatic("net.mm2d.upnp.util.NetworkUtilsKt")
        every { nif.isAvailableInet4Interface() } returns true
    }

    @After
    fun teardown() {
        unmockkStatic("net.mm2d.upnp.util.NetworkUtilsKt")
    }

    @Test
    fun start() {
        val receiver: SsdpNotifyReceiver = mockk(relaxed = true)
        val listener: (SsdpMessage) -> Unit = mockk(relaxed = true)
        mockkObject(SsdpNotifyReceiverList.Companion)
        every { SsdpNotifyReceiverList.newReceiver(any(), Address.IP_V4, nif, listener) } returns receiver
        val list = spyk(SsdpNotifyReceiverList(mockk(), Protocol.DEFAULT, listOf(nif), listener))

        list.start()

        verify(exactly = 1) { receiver.start() }
    }

    @Test
    fun stop() {
        val receiver: SsdpNotifyReceiver = mockk(relaxed = true)
        val listener: (SsdpMessage) -> Unit = mockk(relaxed = true)
        mockkObject(SsdpNotifyReceiverList.Companion)
        every { SsdpNotifyReceiverList.newReceiver(any(), Address.IP_V4, nif, listener) } returns receiver
        val list = spyk(SsdpNotifyReceiverList(mockk(), Protocol.DEFAULT, listOf(nif), listener))

        list.stop()

        verify(exactly = 1) { receiver.stop() }
    }

    @Test
    fun newReceiver_該当アドレスがなければnull() {
        assertThat(SsdpNotifyReceiverList.newReceiver(mockk(), Address.IP_V4, nif, mockk())).isNull()
    }
}
