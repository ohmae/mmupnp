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
import javax.annotation.Nullable;

/**
 * SSDPレスポンスメッセージを表現するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class SsdpResponseMessage extends SsdpMessage {
    /**
     * 受信した情報からインスタンス作成。
     *
     * @param address 受信したInterfaceAddress
     * @param data    受信したデータ
     * @param length  受信したデータの長さ
     * @throws IOException 入出力エラー
     */
    public SsdpResponseMessage(@Nonnull final InterfaceAddress address,
                               @Nonnull final byte[] data, final int length)
            throws IOException {
        super(address, data, length);
    }

    @Override
    @Nonnull
    protected HttpMessage newMessage() {
        return new HttpResponse();
    }

    @Override
    @Nonnull
    protected HttpResponse getMessage() {
        return (HttpResponse) super.getMessage();
    }

    /**
     * ステータスコードを返す。
     *
     * @return ステータスコード
     * @see #getStatus()
     */
    public int getStatusCode() {
        return getMessage().getStatusCode();
    }

    /**
     * ステータスコードを設定する。
     *
     * @param code ステータスコード
     * @see #setStatus(net.mm2d.upnp.Http.Status)
     */
    public void setStatusCode(final int code) {
        getMessage().setStatusCode(code);
    }

    /**
     * レスポンスフレーズを取得する。
     *
     * @return レスポンスフレーズ
     * @see #getStatus()
     */
    @Nonnull
    public String getReasonPhrase() {
        return getMessage().getReasonPhrase();
    }

    /**
     * レスポンスフレーズを設定する。
     *
     * @param reasonPhrase レスポンスフレーズ
     * @see #setStatus(net.mm2d.upnp.Http.Status)
     */
    public void setReasonPhrase(@Nonnull final String reasonPhrase) {
        getMessage().setReasonPhrase(reasonPhrase);
    }

    /**
     * ステータスを設定する。
     *
     * @param status ステータス
     */
    public void setStatus(@Nonnull final Http.Status status) {
        getMessage().setStatus(status);
    }

    /**
     * ステータスを取得する。
     *
     * @return ステータス
     */
    @Nullable
    public Http.Status getStatus() {
        return getMessage().getStatus();
    }
}
