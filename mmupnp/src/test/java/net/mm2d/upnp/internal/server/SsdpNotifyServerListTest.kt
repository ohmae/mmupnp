/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import net.mm2d.upnp.common.Protocol
import net.mm2d.upnp.common.SsdpMessage
import net.mm2d.upnp.common.util.isAvailableInet4Interface
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.net.NetworkInterface

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class SsdpNotifyServerListTest {
    private lateinit var nif: NetworkInterface

    @Before
    fun setUp() {
        nif = mockk(relaxed = true)
        mockkStatic("net.mm2d.upnp.common.util.NetworkUtilsKt")
        every { nif.isAvailableInet4Interface() } returns true
    }

    @After
    fun teardown() {
        unmockkStatic("net.mm2d.upnp.common.util.NetworkUtilsKt")
    }

    @Test
    fun start() {
        val server: SsdpNotifyServer = mockk(relaxed = true)
        val listener: (SsdpMessage) -> Unit = mockk(relaxed = true)
        mockkObject(SsdpNotifyServerList.Companion)
        every { SsdpNotifyServerList.newServer(any(), Address.IP_V4, nif, listener) } returns server
        val list = spyk(SsdpNotifyServerList(mockk(), Protocol.DEFAULT, listOf(nif), listener))

        list.start()

        verify(exactly = 1) { server.start() }
    }

    @Test
    fun stop() {
        val server: SsdpNotifyServer = mockk(relaxed = true)
        val listener: (SsdpMessage) -> Unit = mockk(relaxed = true)
        mockkObject(SsdpNotifyServerList.Companion)
        every { SsdpNotifyServerList.newServer(any(), Address.IP_V4, nif, listener) } returns server
        val list = spyk(SsdpNotifyServerList(mockk(), Protocol.DEFAULT, listOf(nif), listener))

        list.stop()

        verify(exactly = 1) { server.stop() }
    }

    @Test
    fun newReceiver_該当アドレスがなければnull() {
        assertThat(SsdpNotifyServerList.newServer(mockk(), Address.IP_V4, nif, mockk())).isNull()
    }
}
