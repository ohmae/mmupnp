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

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class ActionTest {
    @Test(expected = IllegalStateException::class)
    fun build_ServiceをsetしていないとException() {
        ActionImpl.Builder()
            .setName("name")
            .build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_NameをsetしていないとException() {
        ActionImpl.Builder()
            .setService(mockk(relaxed = true))
            .build()
    }

    @Test
    fun addArgumentBuilder_setした値が取得できる() {
        val argumentBuilder: ArgumentImpl.Builder = mockk(relaxed = true)
        val builder = ActionImpl.Builder()
            .addArgumentBuilder(argumentBuilder)
        val list = builder.getArgumentBuilderList()
        assertThat(list).hasSize(1)
        assertThat(list).contains(argumentBuilder)
    }

    @Test
    fun getService_setした値が取得できる() {
        val service: ServiceImpl = mockk(relaxed = true)
        val name = "name"
        val action = ActionImpl.Builder()
            .setService(service)
            .setName(name)
            .build()
        assertThat(action.service).isSameAs(service)
    }

    @Test
    fun getName_setした値が取得できる() {
        val service: ServiceImpl = mockk(relaxed = true)
        val name = "name"
        val action = ActionImpl.Builder()
            .setService(service)
            .setName(name)
            .build()
        assertThat(action.name).isEqualTo(name)
    }

    @Test
    fun getArgumentList_Argumentがない場合はサイズ0() {
        val service: ServiceImpl = mockk(relaxed = true)
        val name = "name"
        val action = ActionImpl.Builder()
            .setService(service)
            .setName(name)
            .build()
        assertThat(action.argumentList.size).isEqualTo(0)
    }

    @Test
    fun getArgumentList_Builderで指定したArgumentが作成されている() {
        val argumentName = "argumentName"
        val stateVariable: StateVariable = mockk(relaxed = true)
        val name = "name"
        val service: ServiceImpl = mockk(relaxed = true)
        val action = ActionImpl.Builder()
            .setService(service)
            .setName(name)
            .addArgumentBuilder(
                ArgumentImpl.Builder()
                    .setName(argumentName)
                    .setDirection("in")
                    .setRelatedStateVariable(stateVariable)
            )
            .build()

        assertThat(action.argumentList.size).isEqualTo(1)
        val argument = action.argumentList[0]
        assertThat(argument.name).isEqualTo(argumentName)
        assertThat(argument.isInputDirection).isEqualTo(true)
        assertThat(argument.relatedStateVariable).isEqualTo(stateVariable)
    }

    @Test
    fun findArgument_名前指定でArgumentが取得でできる() {
        val argumentName = "argumentName"
        val stateVariable: StateVariable = mockk(relaxed = true)
        val name = "name"
        val service: ServiceImpl = mockk(relaxed = true)
        val action = ActionImpl.Builder()
            .setService(service)
            .setName(name)
            .addArgumentBuilder(
                ArgumentImpl.Builder()
                    .setName(argumentName)
                    .setDirection("in")
                    .setRelatedStateVariable(stateVariable)
            )
            .build()
        val argument = action.findArgument(argumentName)

        assertThat(argument!!.name).isEqualTo(argumentName)
        assertThat(argument.isInputDirection).isEqualTo(true)
        assertThat(argument.relatedStateVariable).isEqualTo(stateVariable)
    }

    @Test
    fun createHttpClient() {
        val service: ServiceImpl = mockk(relaxed = true)
        val name = "name"
        val action = ActionImpl.Builder()
            .setService(service)
            .setName(name)
            .build() as ActionImpl
        val client = action.createHttpClient()
        assertThat(client.isKeepAlive).isEqualTo(false)
    }
}