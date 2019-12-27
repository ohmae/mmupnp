/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.util

import io.mockk.every
import io.mockk.mockk
import java.io.InputStream
import java.net.InetAddress
import java.net.InterfaceAddress
import java.nio.charset.StandardCharsets

object TestUtils {
    private val classLoader: ClassLoader
        get() = javaClass.classLoader

    fun getResourceAsStream(name: String): InputStream =
        classLoader.getResourceAsStream(name) ?: error("file not found")

    fun getResourceAsByteArray(name: String): ByteArray =
        getResourceAsStream(name).readAllBytes()

    fun getResourceAsString(name: String): String =
        getResourceAsByteArray(name).toString(StandardCharsets.UTF_8)
}

fun createInterfaceAddress(address: String, broadcast: String, maskLength: Int): InterfaceAddress {
    val interfaceAddress = mockk<InterfaceAddress>(relaxed = true)
    every { interfaceAddress.address } returns InetAddress.getByName(address)
    every { interfaceAddress.broadcast } returns InetAddress.getByName(broadcast)
    every { interfaceAddress.networkPrefixLength } returns maskLength.toShort()
    return interfaceAddress
}
