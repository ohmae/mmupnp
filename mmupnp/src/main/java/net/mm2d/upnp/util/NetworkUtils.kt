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
     * Return interfaces with address that can communicate with the network.
     */
    @JvmStatic
    fun getAvailableInterfaces(): List<NetworkInterface> =
        getNetworkInterfaceList().filter { it.isAvailableInterface() }

    /**
     * Return interfaces with IPv4 address that can communicate with the network.
     */
    @JvmStatic
    fun getAvailableInet4Interfaces(): List<NetworkInterface> =
        getNetworkInterfaceList().filter { it.isAvailableInet4Interface() }

    /**
     * Return interfaces with IPv6 address that can communicate with the network.
     */
    @JvmStatic
    fun getAvailableInet6Interfaces(): List<NetworkInterface> =
        getNetworkInterfaceList().filter { it.isAvailableInet6Interface() }

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
    @JvmStatic
    fun getNetworkInterfaceList(): List<NetworkInterface> = try {
        NetworkInterface.getNetworkInterfaces()
    } catch (ignored: Throwable) {
        null
    }.let { it?.toList() } ?: emptyList()
}

/**
 * Returns whether receiver has IPv4/IPv6 address that can communicate with the network.
 *
 * @receiver NetworkInterface to inspect
 * @return true: receiver has IPv4/IPv6 address that can communicate with the network. false: otherwise
 */
fun NetworkInterface.isAvailableInterface(): Boolean =
    isConnectedToNetwork() && (hasInet4Address() || hasInet6Address())

/**
 * Returns whether receiver has IPv4 address that can communicate with the network.
 *
 * @receiver NetworkInterface to inspect
 * @return true: receiver has IPv4 address that can communicate with the network. false: otherwise
 */
fun NetworkInterface.isAvailableInet4Interface(): Boolean =
    isConnectedToNetwork() && hasInet4Address()

/**
 * Returns whether receiver has IPv6 address that can communicate with the network.
 *
 * @receiver NetworkInterface to inspect
 * @return true: receiver has IPv6 address that can communicate with the network. false: otherwise
 */
fun NetworkInterface.isAvailableInet6Interface(): Boolean =
    isConnectedToNetwork() && hasInet6Address()

/**
 * Returns whether receiver has address that can communicate with the network.
 *
 * @receiver NetworkInterface to inspect
 * @return true: receiver has address that can communicate with the network. false: otherwise
 */
private fun NetworkInterface.isConnectedToNetwork(): Boolean =
    try {
        !isLoopback && isUp && supportsMulticast()
    } catch (ignored: SocketException) {
        false
    }

/**
 * Returns whether receiver has IPv4 address.
 *
 * @receiver NetworkInterface to inspect
 * @return true: receiver has IPv4 address, false: otherwise
 */
private fun NetworkInterface.hasInet4Address(): Boolean =
    interfaceAddresses.any { it.address.isAvailableInet4Address() }

/**
 * Determine if receiver is available Inet4addresses.
 *
 * @receiver InetAddress
 * @return true: receiver is available Inet4Address, false: otherwise
 */
internal fun InetAddress.isAvailableInet4Address() = this is Inet4Address

/**
 * Returns whether receiver has IPv6 address.
 *
 * @receiver NetworkInterface to inspect
 * @return true: receiver has IPv6 address false: otherwise
 */
private fun NetworkInterface.hasInet6Address(): Boolean =
    interfaceAddresses.any { it.address.isAvailableInet6Address() }

/**
 * Determine if receiver is available Inet6addresses.
 *
 * @receiver InetAddress
 * @return true: receiver is available Inet6Address, false: otherwise
 */
internal fun InetAddress.isAvailableInet6Address() = this is Inet6Address && this.isLinkLocalAddress

/**
 * Extract IPv4 address from assigned to the interface.
 *
 * @receiver NetworkInterface
 * @return InterfaceAddress
 * @throws IllegalArgumentException receiver does not have IPv4 address
 */
// VisibleForTesting
internal fun NetworkInterface.findInet4Address(): InterfaceAddress =
    interfaceAddresses.find { it.address.isAvailableInet4Address() }
        ?: throw IllegalArgumentException("$this does not have IPv4 address.")

/**
 * Extract IPv6 address from assigned to the interface
 *
 * @receiver NetworkInterface
 * @return InterfaceAddress
 * @throws IllegalArgumentException receiver does not have IPv6 address
 */
// VisibleForTesting
internal fun NetworkInterface.findInet6Address(): InterfaceAddress =
    interfaceAddresses.find { it.address.isAvailableInet6Address() }
        ?: throw IllegalArgumentException("$this does not have IPv6 address.")

/**
 * Returns a combined string of address and port number.
 *
 * @receiver Address to convert
 * @return a combined string of address and port number
 */
@Throws(IllegalStateException::class)
fun InetSocketAddress.toAddressString(): String =
    address.toAddressString(port)

/**
 * Returns a combined string of address and port number.
 *
 * @receiver Address to convert
 * @param port port number
 * @return a combined string of address and port number
 */
@Throws(IllegalStateException::class)
fun InetAddress.toAddressString(port: Int): String =
    toAddressString().let {
        if (port == Http.DEFAULT_PORT || port <= 0) it else "$it:$port"
    }

private fun InetAddress.toAddressString(): String =
    if (this is Inet6Address) "[${toNormalizedString()}]" else hostAddress

internal fun InetAddress.toSimpleString(): String =
    if (this is Inet6Address) toNormalizedString() else hostAddress

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

private class Range(
    var start: Int = -1,
    var length: Int = 0
)

private fun IntArray.findSectionToOmission(): Pair<Int, Int> {
    var work = Range()
    var best = Range()
    for (i in indices) {
        if (this[i] == 0) {
            if (work.start < 0) work.start = i
            work.length++
        } else if (work.start >= 0) {
            if (work.length > best.length) best = work
            work = Range()
        }
    }
    if (work.length > best.length) best = work
    return if (best.length < 2) -1 to -1 else best.start to best.start + best.length
}
