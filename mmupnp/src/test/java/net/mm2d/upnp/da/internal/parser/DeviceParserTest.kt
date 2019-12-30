/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da.internal.parser

import net.mm2d.upnp.util.TestUtils
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DeviceParserTest {
    @Test
    fun `test`() {
        val deviceDescription = TestUtils.getResourceAsString("device.xml")
        val cds = TestUtils.getResourceAsString("cds.xml")
        val cms = TestUtils.getResourceAsString("cms.xml")
        val mmupnp = TestUtils.getResourceAsString("mmupnp.xml")
        val device = DeviceParser.parse(deviceDescription, mapOf(
            "urn:upnp-org:serviceId:ConnectionManager" to cms,
            "urn:upnp-org:serviceId:ContentDirectory" to cds,
            "urn:upnp-org:serviceId:X_mmupnp" to mmupnp
        ))
        println(device.createDescription())
        device.serviceList.forEach {
            println(it.createDescription())
        }
    }
}
