/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;

import javax.annotation.Nonnull;

/**
 * SSDPパケットの受信を行うインターフェース
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
interface SsdpServer {
    /**
     * SSDPに使用するアドレス文字列。
     */
    @Nonnull
    String SSDP_ADDR = "239.255.255.250";
    /**
     * SSDPに使用するポート番号。
     */
    int SSDP_PORT = 1900;
    /**
     * SSDPに使用するSocketAddress。
     */
    @Nonnull
    InetSocketAddress SSDP_SO_ADDR = new InetSocketAddress(SSDP_ADDR, SSDP_PORT);
    /**
     * SSDPに使用するアドレス。
     */
    @Nonnull
    InetAddress SSDP_INET_ADDR = SSDP_SO_ADDR.getAddress();

    /**
     * BindされたInterfaceのアドレスを返す。
     *
     * @return BindされたInterfaceのアドレス
     */
    @Nonnull
    InterfaceAddress getInterfaceAddress();

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
