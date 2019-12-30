/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.server

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import net.mm2d.upnp.common.Protocol
import net.mm2d.upnp.common.internal.server.Address
import net.mm2d.upnp.common.util.isAvailableInet4Interface
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.net.NetworkInterface

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class MulticastEventReceiverListTest {
    private lateinit var nif: NetworkInterface

    @Before
    fun setUp() {
        nif = mockk(relaxed = true)
        mockkStatic("net.mm2d.upnp.common.util.NetworkUtilsKt")
        every { nif.isAvailableInet4Interface() } returns true
    }

    @After
    fun tearDown() {
        unmockkStatic("net.mm2d.upnp.common.util.NetworkUtilsKt")
    }

    @Test
    fun start() {
        mockkObject(MulticastEventReceiverList.Companion)
        val receiver: MulticastEventReceiver = mockk(relaxed = true)
        every { MulticastEventReceiverList.newReceiver(any(), any(), any(), any()) } returns receiver
        val list = MulticastEventReceiverList(mockk(), Protocol.DEFAULT, listOf(nif), mockk())

        list.start()

        verify(exactly = 1) { receiver.start() }
        unmockkObject(MulticastEventReceiverList.Companion)
    }

    @Test
    fun stop() {
        mockkObject(MulticastEventReceiverList.Companion)
        val receiver: MulticastEventReceiver = mockk(relaxed = true)
        every { MulticastEventReceiverList.newReceiver(any(), any(), any(), any()) } returns receiver
        val list = MulticastEventReceiverList(mockk(), Protocol.DEFAULT, listOf(nif), mockk())

        list.stop()

        verify(exactly = 1) { receiver.stop() }
        unmockkObject(MulticastEventReceiverList.Companion)
    }

    @Test
    fun newReceiver() {
        assertThat(MulticastEventReceiverList.newReceiver(mockk(), Address.IP_V4, nif, mockk())).isNull()
    }
}
