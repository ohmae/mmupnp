/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 */

package net.mm2d.upnp

import com.google.common.truth.Truth.assertThat
import net.mm2d.upnp.util.isAvailableInet4Interface
import net.mm2d.upnp.util.isAvailableInet6Interface
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProtocolTest {
    @Test
    @Throws(Exception::class)
    fun getAvailableInterfaces() {
        assertThat(Protocol.IP_V4_ONLY.availableInterfaces.any { !it.isAvailableInet4Interface() }).isFalse()
        assertThat(Protocol.IP_V6_ONLY.availableInterfaces.any { !it.isAvailableInet6Interface() }).isFalse()
        assertThat(Protocol.DUAL_STACK.availableInterfaces.any {
            !it.isAvailableInet4Interface() && !it.isAvailableInet6Interface()
        }).isFalse()
    }
}
