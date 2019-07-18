/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayOutputStream

@RunWith(JUnit4::class)
class SsdpResponseDelegateTest {
    private lateinit var delegate: SsdpMessageDelegate
    private lateinit var message: SsdpResponse

    @Before
    fun setUp() {
        delegate = mockk(relaxed = true)
        every { delegate.type } returns ""
        every { delegate.uuid } returns ""
        message = SsdpResponse(mockk(relaxed = true), delegate)
    }

    @Test
    fun getLocalAddress() {
        message.localAddress
        verify(exactly = 1) { delegate.localAddress }
    }

    @Test
    fun getHeader() {
        val name = "name"
        message.getHeader(name)
        verify(exactly = 1) { delegate.getHeader(eq(name)) }
    }

    @Test
    fun setHeader() {
        val name = "name"
        val value = "value"
        message.setHeader(name, value)
        verify(exactly = 1) { delegate.setHeader(eq(name), eq(value)) }
    }

    @Test
    fun getUuid() {
        message.uuid
        verify(exactly = 1) { delegate.uuid }
    }

    @Test
    fun getType() {
        message.type
        verify(exactly = 1) { delegate.type }
    }

    @Test
    fun getNts() {
        message.nts
        verify(exactly = 1) { delegate.nts }
    }

    @Test
    fun getMaxAge() {
        message.maxAge
        verify(exactly = 1) { delegate.maxAge }
    }

    @Test
    fun getExpireTime() {
        message.expireTime
        verify(exactly = 1) { delegate.expireTime }
    }

    @Test
    fun getLocation() {
        message.location
        verify(exactly = 1) { delegate.location }
    }

    @Test
    fun writeData() {
        val os = ByteArrayOutputStream()
        message.writeData(os)
        verify(exactly = 1) { delegate.writeData(eq(os)) }
    }

    @Test
    fun toString_() {
        message.toString()
    }
}
