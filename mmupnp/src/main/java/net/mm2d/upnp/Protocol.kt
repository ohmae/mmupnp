/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 */

package net.mm2d.upnp

import net.mm2d.upnp.util.NetworkUtils

import java.net.NetworkInterface

/**
 * 使用するプロトコルスタックを指定するためのenum。
 *
 * @author [大前良介(OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
enum class Protocol {
    /**
     * IPv4のみ使用します。
     */
    IP_V4_ONLY {
        override val availableInterfaces: List<NetworkInterface>
            get() = NetworkUtils.availableInet4Interfaces
    },
    /**
     * IPv6のみ使用します。
     *
     * UPnPではIPv4の対応がMUSTなので検証用としてのみ使用します。
     *
     * IPv6のアドレス体系としてはリンクローカルアドレスのみ使用します。
     * サイトローカルアドレスでのマルチキャストには対応していません。
     */
    IP_V6_ONLY {
        override val availableInterfaces: List<NetworkInterface>
            get() = NetworkUtils.availableInet6Interfaces
    },
    /**
     * IPv4/IPv6のデュアルスタックで動作します。
     *
     * IPv6よりIPv4を優先しますが、
     * IPv4アドレスがリンクローカルアドレス（APIPAによる割り当て）だった場合はIPv6を優先します。
     *
     * IPv6のアドレス体系としてはリンクローカルアドレスのみ使用します。
     * サイトローカルアドレスでのマルチキャストには対応していません。
     */
    DUAL_STACK {
        override val availableInterfaces: List<NetworkInterface>
            get() = NetworkUtils.availableInterfaces
    };

    /**
     * 該当するプロトコルスタックで使用可能なNetworkInterfaceを返す。
     *
     * @return 使用可能なNetworkInterface
     */
    internal abstract val availableInterfaces: List<NetworkInterface>

    companion object {
        /**
         * デフォルトのプロトコルスタック。
         */
        val DEFAULT = DUAL_STACK
    }
}
