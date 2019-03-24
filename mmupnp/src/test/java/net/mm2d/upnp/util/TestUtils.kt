/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.util

import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.io.InputStream
import java.net.InetAddress
import java.net.InterfaceAddress
import java.nio.file.Files

object TestUtils {
    private val classLoader: ClassLoader
        get() = TestUtils::class.java.classLoader

    fun getResourceAsFile(name: String): File {
        return File(classLoader.getResource(name)!!.file)
    }

    fun getResourceAsStream(name: String): InputStream {
        return classLoader.getResourceAsStream(name)
    }

    fun getResourceAsByteArray(name: String): ByteArray {
        return Files.readAllBytes(getResourceAsFile(name).toPath())
    }

    fun getResourceAsString(name: String): String {
        return String(Files.readAllBytes(getResourceAsFile(name).toPath()))
    }
}

fun createInterfaceAddress(address: String, broadcast: String, maskLength: Int): InterfaceAddress {
    val interfaceAddress = mockk<InterfaceAddress>(relaxed = true)
    every { interfaceAddress.address } returns InetAddress.getByName(address)
    every { interfaceAddress.broadcast } returns InetAddress.getByName(broadcast)
    every { interfaceAddress.networkPrefixLength } returns maskLength.toShort()
    return interfaceAddress
}
