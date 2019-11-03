/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.message

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.mm2d.upnp.common.SsdpMessage
import org.junit.Before
import org.junit.Test

import java.net.InetAddress

class FakeSsdpMessageTest {
    private lateinit var fakeSsdpMessage: FakeSsdpMessage

    @Before
    fun setUp() {
        fakeSsdpMessage = FakeSsdpMessage(LOCATION)
    }

    @Test
    fun isPinned() {
        assertThat(FakeSsdpMessage(LOCATION, "", true).isPinned).isTrue()
        assertThat(FakeSsdpMessage(LOCATION, "", false).isPinned).isFalse()
    }

    @Test
    fun getScopeId() {
        assertThat(fakeSsdpMessage.scopeId).isEqualTo(0)
    }

    @Test
    fun setLocalAddress() {
        val address = InetAddress.getByName("127.0.0.1")
        fakeSsdpMessage.localAddress = address
        assertThat(fakeSsdpMessage.localAddress).isEqualTo(address)
    }

    @Test
    fun getLocalAddress() {
        assertThat(fakeSsdpMessage.localAddress).isNull()
    }

    @Test
    fun getHeader() {
        assertThat(fakeSsdpMessage.getHeader("")).isNull()
    }

    @Test
    fun setHeader() {
        fakeSsdpMessage.setHeader("", "")
    }

    @Test
    fun setUuid() {
        val uuid = "uuid"
        fakeSsdpMessage.uuid = uuid
        assertThat(fakeSsdpMessage.uuid).isEqualTo(uuid)
    }

    @Test
    fun getUuid() {
        assertThat(fakeSsdpMessage.uuid).isEmpty()
    }

    @Test
    fun getType() {
        assertThat(fakeSsdpMessage.type).isEmpty()
    }

    @Test
    fun getNts() {
        assertThat(fakeSsdpMessage.nts).isEqualTo(SsdpMessage.SSDP_ALIVE)
    }

    @Test
    fun getMaxAge() {
        assertThat(fakeSsdpMessage.maxAge).isEqualTo(Integer.MAX_VALUE)
    }

    @Test
    fun getExpireTime() {
        assertThat(fakeSsdpMessage.expireTime).isEqualTo(java.lang.Long.MAX_VALUE)
    }

    @Test
    fun getLocation() {
        assertThat(fakeSsdpMessage.location).isEqualTo(LOCATION)
    }

    @Test
    fun writeData() {
        fakeSsdpMessage.writeData(mockk(relaxed = true))
    }

    companion object {
        private const val LOCATION = "http://127.0.0.1/"
    }
}
