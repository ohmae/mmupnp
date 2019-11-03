/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class StateVariableTest {
    @Test(expected = IllegalStateException::class)
    fun build_Nameを指定していなければException() {
        val dataType = "ui4"
        StateVariableImpl.Builder()
            .setDataType(dataType)
            .build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_DataTypeを指定していなければException() {
        val name = "name"
        StateVariableImpl.Builder()
            .setName(name)
            .build()
    }

    @Test
    fun getName() {
        val name = "name"
        val dataType = "ui4"
        val stateVariable = StateVariableImpl.Builder()
            .setName(name)
            .setDataType(dataType)
            .build()
        assertThat(stateVariable.name).isEqualTo(name)
    }

    @Test
    fun getDataType() {
        val name = "name"
        val dataType = "ui4"
        val stateVariable = StateVariableImpl.Builder()
            .setName(name)
            .setDataType(dataType)
            .build()
        assertThat(stateVariable.dataType).isEqualTo(dataType)
    }

    @Test
    fun isSendEvents() {
        val name = "name"
        val dataType = "ui4"
        val stateVariable = StateVariableImpl.Builder()
            .setName(name)
            .setDataType(dataType)
            .setSendEvents("yes")
            .build()
        assertThat(stateVariable.isSendEvents).isTrue()
    }

    @Test
    fun isMulticast() {
        val name = "name"
        val dataType = "ui4"
        val stateVariable = StateVariableImpl.Builder()
            .setName(name)
            .setDataType(dataType)
            .setMulticast("yes")
            .build()
        assertThat(stateVariable.isMulticast).isTrue()
    }

    @Test
    fun getAllowedValueList() {
        val name = "name"
        val dataType = "ui4"
        val value = "1"
        val stateVariable = StateVariableImpl.Builder()
            .setName(name)
            .setDataType(dataType)
            .addAllowedValue(value)
            .build()
        assertThat(stateVariable.allowedValueList).contains(value)
    }

    @Test
    fun getDefaultValue() {
        val name = "name"
        val dataType = "ui4"
        val value = "1"
        val stateVariable = StateVariableImpl.Builder()
            .setName(name)
            .setDataType(dataType)
            .setDefaultValue(value)
            .build()
        assertThat(stateVariable.defaultValue).isEqualTo(value)
    }

    @Test
    fun getMinimum() {
        val name = "name"
        val dataType = "ui4"
        val value = "1"
        val stateVariable = StateVariableImpl.Builder()
            .setName(name)
            .setDataType(dataType)
            .setMinimum(value)
            .build()
        assertThat(stateVariable.minimum).isEqualTo(value)
    }

    @Test
    fun getMaximum() {
        val name = "name"
        val dataType = "ui4"
        val value = "1"
        val stateVariable = StateVariableImpl.Builder()
            .setName(name)
            .setDataType(dataType)
            .setMaximum(value)
            .build()
        assertThat(stateVariable.maximum).isEqualTo(value)
    }

    @Test
    fun getStep() {
        val name = "name"
        val dataType = "ui4"
        val value = "1"
        val stateVariable = StateVariableImpl.Builder()
            .setName(name)
            .setDataType(dataType)
            .setStep(value)
            .build()
        assertThat(stateVariable.step).isEqualTo(value)
    }
}
