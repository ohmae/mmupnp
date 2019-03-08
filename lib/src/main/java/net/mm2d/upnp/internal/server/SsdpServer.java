/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server;

import net.mm2d.upnp.SsdpMessage;

import java.io.IOException;

import javax.annotation.Nonnull;

/**
 * SSDPパケットの受信を行うインターフェース
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public interface SsdpServer {
    /**
     * SSDPに使用するポート番号。
     */
    int SSDP_PORT = 1900;

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
