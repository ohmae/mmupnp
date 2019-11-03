/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.empty

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException

@RunWith(JUnit4::class)
class EmptySsdpMessageTest {
    @Test
    fun isPinned() {
        val message = EmptySsdpMessage
        assertThat(message.isPinned).isFalse()
    }

    @Test
    fun getScopeId() {
        val message = EmptySsdpMessage
        assertThat(message.scopeId).isEqualTo(0)
    }

    @Test
    fun getLocalAddress() {
        val message = EmptySsdpMessage
        assertThat(message.localAddress).isNull()
    }

    @Test
    fun getHeader() {
        val message = EmptySsdpMessage
        assertThat(message.getHeader("")).isNull()
    }

    @Test
    fun setHeader() {
        val message = EmptySsdpMessage
        message.setHeader("", "")
    }

    @Test
    fun getUuid() {
        val message = EmptySsdpMessage
        assertThat(message.uuid).isNotNull()
    }

    @Test
    fun getType() {
        val message = EmptySsdpMessage
        assertThat(message.type).isNotNull()
    }

    @Test
    fun getNts() {
        val message = EmptySsdpMessage
        assertThat(message.nts).isNull()
    }

    @Test
    fun getMaxAge() {
        val message = EmptySsdpMessage
        assertThat(message.maxAge >= 0).isTrue()
    }

    @Test
    fun getExpireTime() {
        val message = EmptySsdpMessage
        assertThat(message.expireTime >= 0).isTrue()
    }

    @Test
    fun getLocation() {
        val message = EmptySsdpMessage
        assertThat(message.location).isNull()
    }

    @Test(expected = IOException::class)
    fun writeData() {
        val message = EmptySsdpMessage
        message.writeData(mockk())
    }
}
