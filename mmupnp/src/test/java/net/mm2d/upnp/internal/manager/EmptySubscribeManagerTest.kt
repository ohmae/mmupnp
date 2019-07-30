/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class EmptySubscribeManagerTest {
    @Test
    fun getEventPort() {
        assertThat(EmptySubscribeManager().getEventPort()).isEqualTo(0)
    }

    @Test
    fun initialize() {
        EmptySubscribeManager().initialize()
    }

    @Test
    fun start() {
        EmptySubscribeManager().start()
    }

    @Test
    fun stop() {
        EmptySubscribeManager().stop()
    }

    @Test
    fun terminate() {
        EmptySubscribeManager().terminate()
    }

    @Test
    fun getSubscribeService() {
        assertThat(EmptySubscribeManager().getSubscribeService("")).isNull()
    }

    @Test
    fun register() {
        EmptySubscribeManager().register(mockk(), 0L, true)
    }

    @Test
    fun renew() {
        EmptySubscribeManager().renew(mockk(), 0L)
    }

    @Test
    fun setKeepRenew() {
        EmptySubscribeManager().setKeepRenew(mockk(), true)
    }

    @Test
    fun unregister() {
        EmptySubscribeManager().unregister(mockk())
    }
}
