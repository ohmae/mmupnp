/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message;

import net.mm2d.upnp.SsdpMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * SSDPリクエストメッセージを表現するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class SsdpRequest implements SsdpMessage {
    @Nonnull
    private final HttpRequest mHttpRequest;
    @Nonnull
    private final SsdpMessageDelegate mDelegate;

    /**
     * インスタンス作成。
     *
     * @return インスタンス
     */
    public static SsdpRequest create() {
        final HttpRequest httpRequest = HttpRequest.create();
        final SsdpMessageDelegate delegate = new SsdpMessageDelegate(httpRequest);
        return new SsdpRequest(httpRequest, delegate);
    }

    /**
     * 受信した情報からインスタンス作成。
     *
     * @param address 受信したインターフェースのアドレス
     * @param data    受信したデータ
     * @param length  受信したデータの長さ
     * @return インスタンス
     * @throws IOException 入出力エラー
     */
    public static SsdpRequest create(
            @Nonnull final InetAddress address,
            @Nonnull final byte[] data,
            final int length)
            throws IOException {
        final HttpRequest httpRequest = HttpRequest.create();
        httpRequest.readData(new ByteArrayInputStream(data, 0, length));
        final SsdpMessageDelegate delegate = new SsdpMessageDelegate(httpRequest, address);
        return new SsdpRequest(httpRequest, delegate);
    }

    // VisibleForTesting
    SsdpRequest(
            @Nonnull final HttpRequest request,
            @Nonnull final SsdpMessageDelegate delegate) {
        mHttpRequest = request;
        mDelegate = delegate;
    }

    @Nonnull
    public HttpRequest getMessage() {
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
    public void updateLocation() {
        mDelegate.updateLocation();
    }

    @Override
    public boolean isPinned() {
        return mDelegate.isPinned();
    }

    @Override
    public int getScopeId() {
        return mDelegate.getScopeId();
    }

    @Nullable
    @Override
    public InetAddress getLocalAddress() {
        return mDelegate.getLocalAddress();
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

    @Nonnull
    @Override
    public String toString() {
        return mDelegate.toString();
    }
}
