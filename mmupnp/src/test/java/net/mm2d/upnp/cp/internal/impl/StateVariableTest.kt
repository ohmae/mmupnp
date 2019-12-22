/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.impl

import com.google.common.truth.Truth.assertThat
import net.mm2d.upnp.common.internal.property.StateVariableProperty
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class StateVariableTest {
    @Test
    fun getName() {
        val property = StateVariableProperty(
            name = "name",
            dataType = "ui4",
            isSendEvents = true
        )
        val stateVariable = StateVariableImpl(property)
        assertThat(stateVariable.name).isEqualTo(property.name)
        assertThat(stateVariable.dataType).isEqualTo(property.dataType)
        assertThat(stateVariable.isSendEvents).isEqualTo(property.isSendEvents)
        assertThat(stateVariable.isMulticast).isEqualTo(property.isMulticast)
        assertThat(stateVariable.allowedValueList).isEqualTo(property.allowedValueList)
        assertThat(stateVariable.defaultValue).isEqualTo(property.defaultValue)
        assertThat(stateVariable.minimum).isEqualTo(property.minimum)
        assertThat(stateVariable.maximum).isEqualTo(property.maximum)
        assertThat(stateVariable.step).isEqualTo(property.step)
    }
}
