/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.impl

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.mm2d.upnp.common.internal.property.ArgumentProperty
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("NonAsciiCharacters", "TestFunctionName")
@RunWith(JUnit4::class)
class ArgumentTest {
    @Test
    fun `propertyの値が返ること`() {
        val property = ArgumentProperty(
            name = "name",
            isInputDirection = true,
            relatedStateVariable = mockk()
        )
        val stateVariable: StateVariableImpl = mockk(relaxed = true)
        val argument = ArgumentImpl(property, stateVariable)
        assertThat(argument.name).isEqualTo(property.name)
        assertThat(argument.isInputDirection).isEqualTo(property.isInputDirection)
        assertThat(argument.relatedStateVariable).isEqualTo(stateVariable)
    }
}
