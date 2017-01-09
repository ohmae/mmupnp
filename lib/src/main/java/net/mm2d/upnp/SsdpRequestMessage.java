/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.io.IOException;
import java.net.InterfaceAddress;

import javax.annotation.Nonnull;

/**
 * SSDPリクエストメッセージを表現するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class SsdpRequestMessage extends SsdpMessage {
    /**
     * インスタンス作成
     */
    public SsdpRequestMessage() {
        super();
    }

    /**
     * 受信した情報からインスタンス作成
     *
     * @param address 受信したInterfaceAddress
     * @param data    受信したデータ
     * @param length  受信したデータの長さ
     * @throws IOException 入出力エラー
     */
    public SsdpRequestMessage(@Nonnull InterfaceAddress address, @Nonnull byte[] data, int length)
            throws IOException {
        super(address, data, length);
    }

    @Override
    @Nonnull
    protected HttpMessage newMessage() {
        return new HttpRequest();
    }

    @Override
    @Nonnull
    protected HttpRequest getMessage() {
        return (HttpRequest) super.getMessage();
    }

    /**
     * リクエストメソッドを返す。
     *
     * @return リクエストメソッド
     */
    @Nonnull
    public String getMethod() {
        return getMessage().getMethod();
    }

    /**
     * リクエストメソッドを設定する。
     *
     * @param method リクエストメソッド
     */
    public void setMethod(@Nonnull String method) {
        getMessage().setMethod(method);
    }

    /**
     * URIを返す。
     *
     * @return URI文字列
     */
    @Nonnull
    public String getUri() {
        return getMessage().getUri();
    }

    /**
     * URIを設定する。
     *
     * @param uri URI文字列
     */
    public void setUri(@Nonnull String uri) {
        getMessage().setUri(uri);
    }
}
