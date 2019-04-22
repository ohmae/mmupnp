/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.util

import net.mm2d.upnp.Http
import java.net.*
import java.nio.ByteBuffer
import java.util.*

/**
 * Provide network related utility methods.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object NetworkUtils {
    /**
     * Return interfaces with an address that can communicate with the network.
     */
    @JvmStatic
    val availableInterfaces: List<NetworkInterface>
        get() = networkInterfaceList.filter { it.isConnectedToNetwork() }

    /**
     * Return interfaces with an IPv4 address that can communicate with the network.
     */
    @JvmStatic
    val availableInet4Interfaces: List<NetworkInterface>
        get() = networkInterfaceList.filter { it.isAvailableInet4Interface() }

    /**
     * Return interfaces with an IPv6 address that can communicate with the network.
     */
    @JvmStatic
    val availableInet6Interfaces: List<NetworkInterface>
        get() = networkInterfaceList.filter { it.isAvailableInet6Interface() }

    /**
     * Return a list of all network interfaces in the system.
     *
     * The return value of [java.net.NetworkInterface.getNetworkInterfaces]
     * is changed to [java.util.List] instead of [java.util.Enumeration].
     * If there is no interface or if [java.net.SocketException] occurs, an empty List is returned.
     *
     * @return all network interfaces in the system.
     * @see java.net.NetworkInterface.getNetworkInterfaces
     */
    val networkInterfaceList: List<NetworkInterface>
        get() = try {
            NetworkInterface.getNetworkInterfaces()
        } catch (ignored: SocketException) {
            null
        }?.let { Collections.list(it) } ?: emptyList()
}

/**
 * Returns whether receiver has an IPv4 address that can communicate with the network.
 *
 * @receiver NetworkInterface to inspect
 * @return true: receiver has an IPv4 address that can communicate with the network. false: otherwise
 */
fun NetworkInterface.isAvailableInet4Interface(): Boolean {
    return isConnectedToNetwork() && hasInet4Address()
}

/**
 * Returns whether receiver has an IPv6 address that can communicate with the network.
 *
 * @receiver NetworkInterface to inspect
 * @return true: receiver has an IPv6 address that can communicate with the network. false: otherwise
 */
fun NetworkInterface.isAvailableInet6Interface(): Boolean {
    return isConnectedToNetwork() && hasInet6Address()
}

/**
 * Returns whether receiver has an address that can communicate with the network.
 *
 * @receiver NetworkInterface to inspect
 * @return true: receiver has an address that can communicate with the network. false: otherwise
 */
private fun NetworkInterface.isConnectedToNetwork(): Boolean {
    return try {
        !isLoopback && isUp && supportsMulticast()
    } catch (ignored: SocketException) {
        false
    }
}

/**
 * Returns whether receiver has an IPv4 address.
 *
 * @receiver NetworkInterface to inspect
 * @return true: receiver has an IPv4 address false: otherwise
 */
private fun NetworkInterface.hasInet4Address(): Boolean {
    return interfaceAddresses.any { it.address is Inet4Address }
}

/**
 * Returns whether receiver has an IPv6 address.
 *
 * @receiver NetworkInterface to inspect
 * @return true: receiver has an IPv6 address false: otherwise
 */
private fun NetworkInterface.hasInet6Address(): Boolean {
    return interfaceAddresses.any { it.address is Inet6Address }
}

/**
 * Returns a combined string of address and port number.
 *
 * @receiver Address to convert
 * @return a combined string of address and port number
 */
@Throws(IllegalStateException::class)
fun InetSocketAddress.toAddressString(): String {
    return address.toAddressString(port)
}

/**
 * Returns a combined string of address and port number.
 *
 * @receiver Address to convert
 * @param port port number
 * @return a combined string of address and port number
 */
@Throws(IllegalStateException::class)
fun InetAddress.toAddressString(port: Int): String {
    return toAddressString().let {
        if (port == Http.DEFAULT_PORT || port <= 0) it else "$it:$port"
    }
}

private fun InetAddress.toAddressString(): String {
    return if (this is Inet6Address) "[${toNormalizedString()}]" else hostAddress
}

internal fun InetAddress.toSimpleString(): String {
    return if (this is Inet6Address) toNormalizedString() else hostAddress
}

// VisibleForTesting
internal fun Inet6Address.toNormalizedString(): String {
    val buffer = ByteBuffer.wrap(address).asShortBuffer()
    val section = IntArray(8) { buffer.get().toInt() and 0xffff }
    val (start, end) = section.findSectionToOmission()
    val sb = StringBuilder()
    for (i in section.indices) {
        if (i < start || i >= end) {
            if (i != 0 && i != end) sb.append(':')
            sb.append(section[i].toString(16))
        } else if (i == start) {
            sb.append("::")
        }
    }
    return sb.toString()
}

private class Range(var start: Int = -1, var length: Int = 0) {
    fun reset() = set(-1, 0)
    fun set(range: Range) = set(range.start, range.length)
    fun set(s: Int, l: Int) {
        start = s
        length = l
    }
}

private fun IntArray.findSectionToOmission(): Pair<Int, Int> {
    val work = Range()
    val best = Range()
    for (i in indices) {
        if (this[i] == 0) {
            if (work.start < 0) work.start = i
            work.length++
        } else if (work.start >= 0) {
            if (work.length > best.length) best.set(work)
            work.reset()
        }
    }
    if (work.length > best.length)
        best.set(work)
    return if (best.length < 2) -1 to -1
    else best.start to best.start + best.length
}
