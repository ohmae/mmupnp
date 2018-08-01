/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.NetworkUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import javax.annotation.Nonnull;

/**
 * SSDPパケットの受信を行うインターフェース
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
interface SsdpServer {
    /**
     * SSDPに使用するポート番号。
     */
    int SSDP_PORT = 1900;

    /**
     * マルチキャストアドレス。
     */
    enum Address {
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
        IP_V6_SITE_LOCAL("FF05::C"),
        ;

        @Nonnull
        private final InetAddress mInetAddress;
        @Nonnull
        private final InetSocketAddress mInetSocketAddress;

        Address(@Nonnull final String address) {
            mInetAddress = parseAddress(address);
            mInetSocketAddress = new InetSocketAddress(mInetAddress, SSDP_PORT);
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
        String getAddressString() {
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

    /**
     * ソケットのオープンを行う。
     *
     * @throws IOException ソケット作成に失敗
     */
    void open() throws IOException;

    /**
     * ソケットのクローズを行う
     */
    void close();

    /**
     * 受信スレッドの開始を行う。
     */
    void start();

    /**
     * 受信スレッドの停止を行う。
     *
     * <p>停止のリクエストを送るのみで待ち合わせは行わない。
     */
    void stop();

    /**
     * このソケットを使用してメッセージ送信を行う。
     *
     * @param message 送信するメッセージ
     */
    void send(@Nonnull SsdpMessage message);
}
