/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.impl

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import net.mm2d.upnp.common.internal.property.ActionProperty
import net.mm2d.upnp.common.internal.property.ArgumentProperty
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class ActionTest {
    @Test
    fun getService_setした値が取得できる() {
        val service: ServiceImpl = mockk(relaxed = true)
        val name = "name"
        val action = ActionImpl(
            mockk(relaxed = true),
            ActionProperty(name),
            emptyList()
        )
        action.service = service
        assertThat(action.service).isSameInstanceAs(service)
    }

    @Test
    fun getName_setした値が取得できる() {
        val service: ServiceImpl = mockk(relaxed = true)
        val name = "name"
        val action = ActionImpl(
            mockk(relaxed = true),
            ActionProperty(name),
            emptyList()
        )
        action.service = service
        assertThat(action.name).isEqualTo(name)
    }

    @Test
    fun getArgumentList_Argumentがない場合はサイズ0() {
        val service: ServiceImpl = mockk(relaxed = true)
        val name = "name"
        val action = ActionImpl(
            mockk(relaxed = true),
            ActionProperty(name),
            emptyList()
        )
        action.service = service
        assertThat(action.argumentList.size).isEqualTo(0)
    }

    @Test
    fun getArgumentList_Builderで指定したArgumentが作成されている() {
        val argumentName = "argumentName"
        val stateVariable: StateVariableImpl = mockk(relaxed = true)
        val name = "name"
        val service: ServiceImpl = mockk(relaxed = true)
        val action = ActionImpl(
            mockk(relaxed = true),
            ActionProperty(name),
            listOf(
                ArgumentImpl(
                    ArgumentProperty(argumentName, true, mockk(relaxed = true)),
                    stateVariable
                )
            )
        )
        action.service = service
        assertThat(action.argumentList.size).isEqualTo(1)
        val argument = action.argumentList[0]
        assertThat(argument.name).isEqualTo(argumentName)
        assertThat(argument.isInputDirection).isEqualTo(true)
        assertThat(argument.relatedStateVariable).isEqualTo(stateVariable)
    }

    @Test
    fun findArgument_名前指定でArgumentが取得でできる() {
        val argumentName = "argumentName"
        val stateVariable: StateVariableImpl = mockk(relaxed = true)
        val name = "name"
        val service: ServiceImpl = mockk(relaxed = true)

        val action = ActionImpl(
            mockk(relaxed = true),
            ActionProperty(name),
            listOf(
                ArgumentImpl(
                    ArgumentProperty(argumentName, true, mockk(relaxed = true)),
                    stateVariable
                )
            )
        )
        action.service = service
        val argument = action.findArgument(argumentName)

        assertThat(argument!!.name).isEqualTo(argumentName)
        assertThat(argument.isInputDirection).isEqualTo(true)
        assertThat(argument.relatedStateVariable).isEqualTo(stateVariable)
    }

    @Test
    fun makeAbsoluteControlUrl() {
        mockkObject(ActionImpl)
        every { ActionImpl.createInvokeDelegate(any()) } answers { spyk(ActionInvokeDelegate(arg(0))) }
        val device: DeviceImpl = mockk(relaxed = true)
        val service: ServiceImpl = mockk(relaxed = true)
        every { service.device } returns device

        val action = ActionImpl(
            mockk(relaxed = true),
            ActionProperty("name"),
            emptyList()
        )
        action.service = service
        every { device.baseUrl } returns "http://10.0.0.1:1000/"
        every { device.scopeId } returns 0
        every { service.controlUrl } returns "/control"
        val invokeDelegate = action.invokeDelegate
        assertThat(
            invokeDelegate.makeAbsoluteControlUrl().toString()
        ).isEqualTo(
            "http://10.0.0.1:1000/control"
        )
        unmockkObject(ActionImpl)
    }
}
