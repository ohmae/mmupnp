/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InterfaceAddress;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * SSDPレスポンスメッセージを表現するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class SsdpResponse implements SsdpMessage {
    @Nonnull
    private final HttpResponse mHttpResponse;
    @Nonnull
    private final SsdpMessageDelegate mDelegate;

    /**
     * 受信した情報からインスタンス作成。
     *
     * @param address 受信したInterfaceAddress
     * @param data    受信したデータ
     * @param length  受信したデータの長さ
     * @throws IOException 入出力エラー
     */
    public SsdpResponse(
            @Nonnull final InterfaceAddress address,
            @Nonnull final byte[] data,
            final int length)
            throws IOException {
        mHttpResponse = new HttpResponse()
                .readData(new ByteArrayInputStream(data, 0, length));
        mDelegate = new SsdpMessageDelegate(mHttpResponse, address);
    }

    // VisibleForTesting
    SsdpResponse(
            @Nonnull final HttpResponse response,
            @Nonnull final SsdpMessageDelegate delegate) {
        mHttpResponse = response;
        mDelegate = delegate;
    }

    @Nonnull
    protected HttpMessage getMessage() {
        return mHttpResponse;
    }

    /**
     * ステータスコードを返す。
     *
     * @return ステータスコード
     * @see #getStatus()
     */
    public int getStatusCode() {
        return mHttpResponse.getStatusCode();
    }

    /**
     * ステータスコードを設定する。
     *
     * @param code ステータスコード
     * @see #setStatus(net.mm2d.upnp.Http.Status)
     */
    public void setStatusCode(final int code) {
        mHttpResponse.setStatusCode(code);
    }

    /**
     * レスポンスフレーズを取得する。
     *
     * @return レスポンスフレーズ
     * @see #getStatus()
     */
    @Nonnull
    public String getReasonPhrase() {
        return mHttpResponse.getReasonPhrase();
    }

    /**
     * レスポンスフレーズを設定する。
     *
     * @param reasonPhrase レスポンスフレーズ
     * @see #setStatus(net.mm2d.upnp.Http.Status)
     */
    public void setReasonPhrase(@Nonnull final String reasonPhrase) {
        mHttpResponse.setReasonPhrase(reasonPhrase);
    }

    /**
     * ステータスを設定する。
     *
     * @param status ステータス
     */
    public void setStatus(@Nonnull final Http.Status status) {
        mHttpResponse.setStatus(status);
    }

    /**
     * ステータスを取得する。
     *
     * @return ステータス
     */
    @Nonnull
    public Http.Status getStatus() {
        return mHttpResponse.getStatus();
    }

    @Nullable
    @Override
    public InterfaceAddress getInterfaceAddress() {
        return mDelegate.getInterfaceAddress();
    }

    @Nullable
    @Override
    public String getHeader(@Nonnull final String name) {
        return mDelegate.getHeader(name);
    }

    @Override
    public void setHeader(
            @Nonnull final String name,
            @Nonnull final String value) {
        mDelegate.setHeader(name, value);
    }

    @Nonnull
    @Override
    public String getUuid() {
        return mDelegate.getUuid();
    }

    @Nonnull
    @Override
    public String getType() {
        return mDelegate.getType();
    }

    @Nullable
    @Override
    public String getNts() {
        return mDelegate.getNts();
    }

    @Override
    public int getMaxAge() {
        return mDelegate.getMaxAge();
    }

    @Override
    public long getExpireTime() {
        return mDelegate.getExpireTime();
    }

    @Nullable
    @Override
    public String getLocation() {
        return mDelegate.getLocation();
    }

    @Override
    public void writeData(@Nonnull final OutputStream os) throws IOException {
        mDelegate.writeData(os);
    }

    @Override
    public String toString() {
        return mDelegate.toString();
    }
}
