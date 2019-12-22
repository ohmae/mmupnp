/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.impl

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.mm2d.upnp.common.HttpClient
import net.mm2d.upnp.common.SsdpMessage
import net.mm2d.upnp.cp.internal.message.FakeSsdpMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.net.InetAddress

@Suppress("NonAsciiCharacters", "TestFunctionName")
@RunWith(JUnit4::class)
class DeviceBuilderTest {
    @Test
    fun build_PinnedSsdpMessage_update() {
        val message: FakeSsdpMessage = mockk(relaxed = true)
        every { message.location } returns "location"
        every { message.isPinned } returns true
        val newMessage: SsdpMessage = mockk(relaxed = true)
        val builder = DeviceImpl.Builder(mockk(relaxed = true), message)
        builder.updateSsdpMessage(newMessage)
        assertThat(builder.getSsdpMessage()).isEqualTo(message)
    }

    @Test(expected = IllegalArgumentException::class)
    fun build_不正なSsdpMessage1() {
        val illegalMessage: SsdpMessage = mockk(relaxed = true)
        every { illegalMessage.location } returns null
        DeviceImpl.Builder(mockk(relaxed = true), illegalMessage)
    }

    @Test(expected = IllegalArgumentException::class)
    fun build_不正なSsdpMessage2() {
        val message: SsdpMessage = mockk(relaxed = true)
        val location = "location"
        val uuid = "uuid"
        every { message.location } returns location
        every { message.uuid } returns uuid
        val illegalMessage: SsdpMessage = mockk(relaxed = true)
        every { illegalMessage.location } returns null
        DeviceImpl.Builder(mockk(relaxed = true), message)
            .updateSsdpMessage(illegalMessage)
    }

    @Test
    fun onDownloadDescription() {
        val message: FakeSsdpMessage = mockk(relaxed = true)
        every { message.location } returns "location"
        val builder = DeviceImpl.Builder(mockk(relaxed = true), message)
        val client: HttpClient = mockk(relaxed = true)
        val address = InetAddress.getByName("127.0.0.1")
        every { client.localAddress } returns address
        builder.setDownloadInfo(client)

        verify(exactly = 1) { client.localAddress }
        verify(exactly = 1) { message.localAddress = address }
    }

    @Test(expected = IllegalStateException::class)
    fun onDownloadDescription_before_download() {
        val message: FakeSsdpMessage = mockk(relaxed = true)
        every { message.location } returns "location"
        val builder = DeviceImpl.Builder(mockk(relaxed = true), message)
        val client = HttpClient()
        builder.setDownloadInfo(client)
    }
}
