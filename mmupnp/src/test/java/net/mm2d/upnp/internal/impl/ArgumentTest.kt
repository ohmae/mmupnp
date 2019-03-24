/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.mm2d.upnp.StateVariable
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("NonAsciiCharacters", "TestFunctionName")
@RunWith(JUnit4::class)
class ArgumentTest {
    @Test(expected = IllegalStateException::class)
    fun build_Nameを設定していないとException() {
        ArgumentImpl.Builder()
                .build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_RelatedStateVariableを設定していないとException() {
        ArgumentImpl.Builder()
                .setName("")
                .build()
    }

    @Test
    fun getRelatedStateVariableName_setした値が返る() {
        val name = "name"
        val builder = ArgumentImpl.Builder()
                .setRelatedStateVariableName(name)
        assertThat(builder.getRelatedStateVariableName()).isEqualTo(name)
    }

    @Test
    fun build_Builderで指定した値が得られる() {
        val name = "name"
        val stateVariable: StateVariable = mockk(relaxed = true)
        val argument = ArgumentImpl.Builder()
                .setName(name)
                .setDirection("in")
                .setRelatedStateVariable(stateVariable)
                .build()
        assertThat(argument.relatedStateVariable).isEqualTo(stateVariable)
        assertThat(argument.name).isEqualTo(name)
        assertThat(argument.isInputDirection).isTrue()
    }

    @Test
    fun isInputDirection_Builderでoutを指定した場合false() {
        val name = "name"
        val stateVariable: StateVariable = mockk(relaxed = true)
        val argument = ArgumentImpl.Builder()
                .setName(name)
                .setDirection("out")
                .setRelatedStateVariable(stateVariable)
                .build()
        assertThat(argument.isInputDirection).isFalse()
    }
}
