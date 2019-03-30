/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import net.mm2d.upnp.util.NetworkUtils;

import java.net.NetworkInterface;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * 使用するプロトコルスタックを指定するためのenum。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public enum Protocol {
    /**
     * IPv4のみ使用します。
     */
    IP_V4_ONLY {
        @Nonnull
        @Override
        public List<NetworkInterface> getAvailableInterfaces() {
            return NetworkUtils.getAvailableInet4Interfaces();
        }
    },
    /**
     * IPv6のみ使用します。
     *
     * <p>UPnPではIPv4の対応がMUSTなので検証用としてのみ使用します。
     *
     * <p>IPv6のアドレス体系としてはリンクローカルアドレスのみ使用します。
     * サイトローカルアドレスでのマルチキャストには対応していません。
     */
    IP_V6_ONLY {
        @Nonnull
        @Override
        public List<NetworkInterface> getAvailableInterfaces() {
            return NetworkUtils.getAvailableInet6Interfaces();
        }
    },
    /**
     * IPv4/IPv6のデュアルスタックで動作します。
     *
     * <p>IPv6よりIPv4を優先しますが、
     * IPv4アドレスがリンクローカルアドレス（APIPAによる割り当て）だった場合はIPv6を優先します。
     *
     * <p>IPv6のアドレス体系としてはリンクローカルアドレスのみ使用します。
     * サイトローカルアドレスでのマルチキャストには対応していません。
     */
    DUAL_STACK {
        @Nonnull
        @Override
        public List<NetworkInterface> getAvailableInterfaces() {
            return NetworkUtils.getAvailableInterfaces();
        }
    },
    ;

    /**
     * デフォルトのプロトコルスタック。
     */
    @Nonnull
    public static final Protocol DEFAULT = DUAL_STACK;

    /**
     * 該当するプロトコルスタックで使用可能なNetworkInterfaceを返す。
     *
     * @return 使用可能なNetworkInterface
     */
    @Nonnull
    public abstract List<NetworkInterface> getAvailableInterfaces();
}
