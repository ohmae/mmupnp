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
 * SSDPリクエストメッセージを表現するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class SsdpRequestMessage implements SsdpMessage {
    @Nonnull
    private final HttpRequest mHttpRequest;
    @Nonnull
    private final SsdpMessageDelegate mDelegate;

    /**
     * インスタンス作成。
     */
    public SsdpRequestMessage() {
        mHttpRequest = new HttpRequest();
        mDelegate = new SsdpMessageDelegate(mHttpRequest);
    }

    /**
     * 受信した情報からインスタンス作成。
     *
     * @param address 受信したInterfaceAddress
     * @param data    受信したデータ
     * @param length  受信したデータの長さ
     * @throws IOException 入出力エラー
     */
    public SsdpRequestMessage(
            @Nonnull final InterfaceAddress address,
            @Nonnull final byte[] data,
            final int length)
            throws IOException {
        mHttpRequest = new HttpRequest();
        mHttpRequest.readData(new ByteArrayInputStream(data, 0, length));
        mDelegate = new SsdpMessageDelegate(mHttpRequest, address);
    }

    @Nonnull
    protected HttpRequest getMessage() {
        return mHttpRequest;
    }

    /**
     * リクエストメソッドを返す。
     *
     * @return リクエストメソッド
     */
    @Nonnull
    public String getMethod() {
        return mHttpRequest.getMethod();
    }

    /**
     * リクエストメソッドを設定する。
     *
     * @param method リクエストメソッド
     */
    public void setMethod(@Nonnull final String method) {
        mHttpRequest.setMethod(method);
    }

    /**
     * URIを返す。
     *
     * @return URI文字列
     */
    @Nonnull
    public String getUri() {
        return mHttpRequest.getUri();
    }

    /**
     * URIを設定する。
     *
     * @param uri URI文字列
     */
    public void setUri(@Nonnull final String uri) {
        mHttpRequest.setUri(uri);
    }

    // VisibleForTesting
    void updateLocation() {
        mDelegate.updateLocation();
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
