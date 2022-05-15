/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException

@RunWith(JUnit4::class)
class EmptyActionTest {

    @Test
    fun getService() {
        assertThat(EmptyAction.service).isNotNull()
    }

    @Test
    fun getName() {
        assertThat(EmptyAction.name).isNotNull()
    }

    @Test
    fun getArgumentList() {
        assertThat(EmptyAction.argumentList).isNotNull()
    }

    @Test
    fun findArgument() {
        assertThat(EmptyAction.findArgument("")).isNull()
    }

    @Test(expected = IOException::class)
    fun invokeAsync() {
        runBlocking {
            EmptyAction.invoke(emptyMap())
        }
    }

    @Test(expected = IOException::class)
    fun invokeCustomAsync() {
        runBlocking {
            EmptyAction.invokeCustom(emptyMap())
        }
    }
}
