/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import java.io.UnsupportedEncodingException

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class SingleHttpMessageDelegateTest {
    @Test
    fun setBody_エンコード不可でもExceptionは発生しない() {
        val body = "body"
        val message = spyk(SingleHttpMessageDelegate(mockk(relaxed = true)))
        every { message.getBytes(any()) } throws UnsupportedEncodingException()
        message.setBody(body, true)
        assertThat(message.getBody()).isEqualTo(body)
    }

    @Test
    fun getBody_デコード不可ならnullが返る() {
        val message = spyk(SingleHttpMessageDelegate(mockk(relaxed = true)))
        val byteArray = "body".toByteArray(charset("utf-8"))
        message.setBodyBinary(byteArray)
        with(message) {
            every { byteArray.newString() } throws UnsupportedEncodingException()
        }
        assertThat(message.getBody()).isNull()
    }

    @Test
    fun getHeaderBytes_エンコード不可でもnullが返らない() {
        val message = spyk(SingleHttpMessageDelegate(mockk(relaxed = true)))
        every { message.getBytes(any()) } throws UnsupportedEncodingException()
        assertThat(message.getHeaderBytes()).hasLength(0)
    }
}
