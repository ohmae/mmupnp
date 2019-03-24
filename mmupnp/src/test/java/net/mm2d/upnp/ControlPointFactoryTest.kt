/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import net.mm2d.upnp.util.NetworkUtils

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class ControlPointFactoryTest {
    @Test
    fun create_引数無しでコール() {
        ControlPointFactory.create()
    }

    @Test
    @Throws(Exception::class)
    fun create_インターフェース指定() {
        ControlPointFactory.create(
            interfaces = NetworkUtils.availableInet4Interfaces
        )
        ControlPointFactory.create(
            interfaces = emptyList()
        )
    }

    @Test
    fun create_インターフェース選別() {
        ControlPointFactory.create(
            protocol = Protocol.IP_V4_ONLY,
            interfaces = NetworkUtils.networkInterfaceList
        )
        ControlPointFactory.create(
            protocol = Protocol.IP_V6_ONLY,
            interfaces = NetworkUtils.networkInterfaceList
        )
        ControlPointFactory.create(
            protocol = Protocol.DUAL_STACK,
            interfaces = NetworkUtils.networkInterfaceList
        )
    }

    @Test
    fun create_プロトコルスタックのみ指定() {
        ControlPointFactory.create(
            protocol = Protocol.IP_V4_ONLY
        )
        ControlPointFactory.create(
            protocol = Protocol.IP_V6_ONLY
        )
        ControlPointFactory.create(
            protocol = Protocol.DUAL_STACK
        )
    }

    @Test
    fun create_Params() {
        ControlPointFactory.create(
            protocol = Protocol.DEFAULT,
            interfaces = NetworkUtils.networkInterfaceList,
            notifySegmentCheckEnabled = true
        )
    }
}
