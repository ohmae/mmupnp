/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server;

import net.mm2d.util.NetworkUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import javax.annotation.Nonnull;

/**
 * マルチキャストアドレス。
 */
public enum Address {
    /**
     * IPv4用マルチキャストアドレス
     */
    IP_V4("239.255.255.250"),
    /**
     * IPv6用リンクローカルマルチキャストアドレス
     */
    IP_V6_LINK_LOCAL("FF02::C"),
    /**
     * IPv6用サイトローカルマルチキャストアドレス
     *
     * <p>未使用
     */
    @SuppressWarnings("unused")
    IP_V6_SITE_LOCAL("FF05::C"),
    ;

    @Nonnull
    private final InetAddress mInetAddress;
    @Nonnull
    private final InetSocketAddress mInetSocketAddress;

    Address(@Nonnull final String address) {
        mInetAddress = parseAddress(address);
        mInetSocketAddress = new InetSocketAddress(mInetAddress, SsdpServer.SSDP_PORT);
    }

    @Nonnull
    InetAddress getInetAddress() {
        return mInetAddress;
    }

    @Nonnull
    InetSocketAddress getSocketAddress() {
        return mInetSocketAddress;
    }

    @Nonnull
    public String getAddressString() {
        return NetworkUtils.getAddressString(mInetSocketAddress);
    }

    static InetAddress parseAddress(@Nonnull final String address) {
        try {
            return InetAddress.getByName(address);
        } catch (final UnknownHostException e) {
            throw new AssertionError(e);
        }
    }
}
