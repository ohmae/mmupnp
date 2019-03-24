/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

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
class SsdpSearchServerListTest {
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
        val server: SsdpSearchServer = mockk(relaxed = true)
        val listener: (SsdpMessage) -> Unit = mockk(relaxed = true)
        mockkObject(SsdpSearchServerList.Companion)
        every { SsdpSearchServerList.newServer(any(), Address.IP_V4, any(), listener) } returns server
        val list = spyk(SsdpSearchServerList(mockk(), Protocol.DEFAULT, listOf(nif), listener))

        list.start()
        verify(exactly = 1) { server.start() }
    }

    @Test
    fun stop() {
        val server: SsdpSearchServer = mockk(relaxed = true)
        val listener: (SsdpMessage) -> Unit = mockk(relaxed = true)
        mockkObject(SsdpSearchServerList.Companion)
        every { SsdpSearchServerList.newServer(any(), Address.IP_V4, any(), listener) } returns server
        val list = spyk(SsdpSearchServerList(mockk(), Protocol.DEFAULT, listOf(nif), listener))

        list.stop()

        verify(exactly = 1) { server.stop() }
    }

    @Test
    fun search() {
        val server: SsdpSearchServer = mockk(relaxed = true)
        val listener: (SsdpMessage) -> Unit = mockk(relaxed = true)
        mockkObject(SsdpSearchServerList.Companion)
        every { SsdpSearchServerList.newServer(any(), Address.IP_V4, any(), listener) } returns server
        val list = spyk(SsdpSearchServerList(mockk(), Protocol.DEFAULT, listOf(nif), listener))

        list.search("")

        verify(exactly = 1) { server.search("") }
    }
}