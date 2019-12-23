/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.parser

import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class DevicePropertyParserTest {

    @Test(expected = IOException::class)
    fun parseDescription_deviceノードのないXMLを渡すとException() {
        DevicePropertyParser.parse(
            mockk(relaxed = true),
            "<?xml version=\"1.0\"?>\n" +
                "<root xmlns=\"urn:schemas-upnp-org:device-1-0\">\n" +
                "</root>"
        )
    }
}
