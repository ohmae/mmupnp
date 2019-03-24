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
 * ネットワーク関係のユーティリティメソッドを提供する。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object NetworkUtils {
    /**
     * 外部と通信可能なIPv4アドレスをもつインターフェースを返す。
     *
     * @return 外部と通信可能なIPv4アドレスを持つインターフェース。
     */
    @JvmStatic
    val availableInterfaces: List<NetworkInterface>
        get() = networkInterfaceList.filter { it.isConnectedToNetwork() }

    /**
     * 外部と通信可能なIPv4アドレスをもつインターフェースを返す。
     *
     * @return 外部と通信可能なIPv4アドレスを持つインターフェース。
     */
    @JvmStatic
    val availableInet4Interfaces: List<NetworkInterface>
        get() = networkInterfaceList.filter { it.isAvailableInet4Interface() }

    /**
     * 外部と通信可能なIPv4アドレスをもつインターフェースを返す。
     *
     * @return 外部と通信可能なIPv4アドレスを持つインターフェース。
     */
    @JvmStatic
    val availableInet6Interfaces: List<NetworkInterface>
        get() = networkInterfaceList.filter { it.isAvailableInet6Interface() }

    /**
     * システムのすべてのネットワークインターフェースのリストを返す。
     *
     * [java.net.NetworkInterface.getNetworkInterfaces]
     * の戻り値を[java.util.Enumeration]ではなく[java.util.List]にしたもの。
     * インターフェースがない場合、及び、[java.net.SocketException]が発生するような場合は、
     * 空のListが返り、nullが返ることはない。
     *
     * @return システムのすべてのネットワークインターフェース
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
 * 外部と通信可能なIPv4アドレスを持つか否かを返す。
 *
 * @receiver 検査するNetworkInterface
 * @return true:外部と通信可能なIPv4アドレスを持つ。false:それ以外
 */
fun NetworkInterface.isAvailableInet4Interface(): Boolean {
    return isConnectedToNetwork() && hasInet4Address()
}

/**
 * 外部と通信可能なIPv6アドレスを持つか否かを返す。
 *
 * @receiver 検査するNetworkInterface
 * @return true:外部と通信可能なIPv4アドレスを持つ。false:それ以外
 */
fun NetworkInterface.isAvailableInet6Interface(): Boolean {
    return isConnectedToNetwork() && hasInet6Address()
}

/**
 * ネットワークに接続している状態か否かを返す。
 *
 * @receiver 検査するNetworkInterface
 * @return true:ネットワークに接続している。false:それ以外
 */
private fun NetworkInterface.isConnectedToNetwork(): Boolean {
    return try {
        !isLoopback && isUp && supportsMulticast()
    } catch (ignored: SocketException) {
        false
    }
}

/**
 * IPv4のアドレスを持つか否かを返す。
 *
 * @receiver 検査するNetworkInterface
 * @return true:IPv4アドレスを持つ。false:それ以外
 */
private fun NetworkInterface.hasInet4Address(): Boolean {
    return interfaceAddresses.any { it.address is Inet4Address }
}

/**
 * IPv6のアドレスを持つか否かを返す。
 *
 * @receiver 検査するNetworkInterface
 * @return true:IPv6アドレスを持つ。false:それ以外
 */
private fun NetworkInterface.hasInet6Address(): Boolean {
    return interfaceAddresses.any { it.address is Inet6Address }
}

/**
 * アドレスとポート番号の組み合わせ文字列を返す。
 *
 * @receiver 変換するアドレス
 * @return アドレスとポート番号の組み合わせ文字列
 */
@Throws(IllegalStateException::class)
fun InetSocketAddress.toAddressString(): String {
    return address.toAddressString(port)
}

/**
 * アドレスとポート番号の組み合わせ文字列を返す。
 *
 * @receiver 変換するアドレス
 * @param port    ポート番号
 * @return アドレスとポート番号の組み合わせ文字列
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
