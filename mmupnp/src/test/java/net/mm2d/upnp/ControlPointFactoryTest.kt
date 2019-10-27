/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import io.mockk.mockk
import net.mm2d.upnp.common.Protocol
import net.mm2d.upnp.common.util.NetworkUtils
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
            interfaces = NetworkUtils.getAvailableInet4Interfaces()
        )
        ControlPointFactory.create(
            interfaces = emptyList()
        )
    }

    @Test
    fun create_インターフェース選別() {
        ControlPointFactory.create(
            protocol = Protocol.IP_V4_ONLY,
            interfaces = NetworkUtils.getNetworkInterfaceList()
        )
        ControlPointFactory.create(
            protocol = Protocol.IP_V6_ONLY,
            interfaces = NetworkUtils.getNetworkInterfaceList()
        )
        ControlPointFactory.create(
            protocol = Protocol.DUAL_STACK,
            interfaces = NetworkUtils.getNetworkInterfaceList()
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
    fun create_executorの指定() {
        ControlPointFactory.create(
            callbackExecutor = null,
            callbackHandler = null
        )
        ControlPointFactory.create(
            callbackExecutor = mockk(),
            callbackHandler = null
        )
        ControlPointFactory.create(
            callbackExecutor = null,
            callbackHandler = mockk()
        )
        ControlPointFactory.create(
            callbackExecutor = mockk(),
            callbackHandler = mockk()
        )
    }

    @Test
    fun builder() {
        ControlPointFactory.builder()
            .setProtocol(Protocol.DEFAULT)
            .setInterfaces(NetworkUtils.getNetworkInterfaceList())
            .setNotifySegmentCheckEnabled(true)
            .setSubscriptionEnabled(true)
            .setMulticastEventingEnabled(true)
            .setCallbackExecutor(mockk())
            .setCallbackHandler { true }
            .build()
    }
}
