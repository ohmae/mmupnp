/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.property

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class StateVariablePropertyBuilderTest {
    @Test
    fun `build nameとdataTypeがあれば成功`() {
        StateVariableProperty.Builder().also {
            it.name = "name"
            it.dataType = "ui4"
        }.build()
    }

    @Test(expected = IllegalStateException::class)
    fun `build Nameを指定していなければException`() {
        StateVariableProperty.Builder().also {
            it.dataType = "ui4"
        }.build()
    }

    @Test(expected = IllegalStateException::class)
    fun `build DataTypeを指定していなければException`() {
        StateVariableProperty.Builder().also {
            it.name = "name"
        }.build()
    }
}
