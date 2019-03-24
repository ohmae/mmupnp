/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.parser

import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class ServiceParserTest {
    @Test(expected = IOException::class)
    fun loadDescription_パラメータがとれないとException() {
        ServiceParser.loadDescription(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
    }
}
