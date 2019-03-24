/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException

@RunWith(JUnit4::class)
class EmptyActionTest {

    @Test
    fun getService() {
        val action = EmptyAction
        assertThat(action.service).isNotNull()
    }

    @Test
    fun getName() {
        val action = EmptyAction
        assertThat(action.name).isNotNull()
    }

    @Test
    fun getArgumentList() {
        val action = EmptyAction
        assertThat(action.argumentList).isNotNull()
    }

    @Test
    fun findArgument() {
        val action = EmptyAction
        assertThat(action.findArgument("")).isNull()
    }

    @Test(expected = IOException::class)
    operator fun invoke() {
        val action = EmptyAction
        action.invokeSync(emptyMap())
    }

    @Test(expected = IOException::class)
    fun invoke1() {
        val action = EmptyAction
        action.invokeSync(emptyMap(), false)
    }

    @Test(expected = IOException::class)
    fun invoke2() {
        val action = EmptyAction
        action.invokeCustomSync(
                emptyMap(),
                customNamespace = emptyMap(),
                customArguments = emptyMap()
        )
    }

    @Test(expected = IOException::class)
    fun invoke3() {
        val action = EmptyAction
        action.invokeCustomSync(
                emptyMap(),
                customNamespace = emptyMap(),
                customArguments = emptyMap(),
                returnErrorResponse = false
        )
    }
}
