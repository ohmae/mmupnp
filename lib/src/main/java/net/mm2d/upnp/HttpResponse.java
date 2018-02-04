/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.Http.Status;
import net.mm2d.upnp.HttpMessageDelegate.StartLineProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HTTPレスポンスメッセージを表現するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class HttpResponse implements HttpMessage {
    @Nonnull
    private final HttpMessageDelegate mDelegate;
    @Nonnull
    private Http.Status mStatus = Status.HTTP_INVALID;

    private int mStatusCode;
    @Nonnull
    private String mReasonPhrase = "";

    private class Processor implements StartLineProcessor {
        @Override
        public void setStartLine(@Nonnull final String line) {
            HttpResponse.this.setStartLine(line);
        }

        @Nonnull
        @Override
        public String getStartLine() {
            return HttpResponse.this.getStartLine();
        }
    }

    /**
     * インスタンス作成。
     */
    public HttpResponse() {
        mDelegate = new HttpMessageDelegate(new Processor());
    }

    // VisibleForTesting
    HttpResponse(@Nonnull final HttpMessageDelegate delegate) {
        mDelegate = delegate;
    }

    public HttpResponse(@Nonnull final HttpResponse original) {
        mDelegate = new HttpMessageDelegate(new Processor(), original.mDelegate);
        mStatus = original.mStatus;
        mStatusCode = original.mStatusCode;
        mReasonPhrase = original.mReasonPhrase;
    }

    @Nonnull
    @Override
    public HttpResponse setStartLine(@Nonnull final String line) {
        return setStatusLine(line);
    }

    /**
     * ステータスラインを設定する。
     *
     * <p>{@link #setStartLine(String)}のエイリアス。
     *
     * @param line ステータスライン
     * @see #setStartLine(String)
     */
    public HttpResponse setStatusLine(@Nonnull final String line) {
        final String[] params = line.split(" ", 3);
        if (params.length < 3) {
            throw new IllegalArgumentException();
        }
        setVersion(params[0]);
        setStatusCode(Integer.parseInt(params[1]));
        setReasonPhrase(params[2]);
        return this;
    }

    @Nonnull
    @Override
    public String getStartLine() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getVersion());
        sb.append(' ');
        if (mStatus != Status.HTTP_INVALID) {
            sb.append(String.valueOf(mStatus.getCode()));
            sb.append(' ');
            sb.append(mStatus.getPhrase());
        } else {
            sb.append(String.valueOf(mStatusCode));
            sb.append(' ');
            sb.append(getReasonPhrase());
        }
        return sb.toString();
    }

    /**
     * ステータスコードを返す
     *
     * @return ステータスコード
     * @see #getStatus()
     */
    public int getStatusCode() {
        return mStatusCode;
    }

    /**
     * ステータスコードを設定する。
     *
     * @param code ステータスコード
     * @return HttpResponse
     * @see #setStatus(net.mm2d.upnp.Http.Status)
     */
    public HttpResponse setStatusCode(final int code) {
        mStatus = Status.valueOf(code);
        if (mStatus == Status.HTTP_INVALID) {
            throw new IllegalArgumentException("unexpected status code:" + code);
        }
        mStatusCode = code;
        mReasonPhrase = mStatus.getPhrase();
        return this;
    }

    /**
     * レスポンスフレーズを取得する
     *
     * @return レスポンスフレーズ
     * @see #getStatus()
     */
    @Nonnull
    public String getReasonPhrase() {
        return mReasonPhrase;
    }

    /**
     * レスポンスフレーズを設定する。
     *
     * @param reasonPhrase レスポンスフレーズ
     * @return HttpResponse
     * @see #setStatus(net.mm2d.upnp.Http.Status)
     */
    public HttpResponse setReasonPhrase(@Nonnull final String reasonPhrase) {
        mReasonPhrase = reasonPhrase;
        return this;
    }

    /**
     * ステータスを設定する。
     *
     * @param status ステータス
     * @return HttpResponse
     */
    public HttpResponse setStatus(@Nonnull final Http.Status status) {
        mStatus = status;
        mStatusCode = status.getCode();
        mReasonPhrase = status.getPhrase();
        return this;
    }

    /**
     * ステータスを取得する。
     *
     * @return ステータス
     */
    @Nonnull
    public Http.Status getStatus() {
        return mStatus;
    }

    @Nonnull
    @Override
    public String getVersion() {
        return mDelegate.getVersion();
    }

    @Nonnull
    @Override
    public HttpResponse setVersion(@Nonnull final String version) {
        mDelegate.setVersion(version);
        return this;
    }

    @Nonnull
    @Override
    public HttpResponse setHeader(
            @Nonnull final String name,
            @Nonnull final String value) {
        mDelegate.setHeader(name, value);
        return this;
    }

    @Nonnull
    @Override
    public HttpResponse setHeaderLine(@Nonnull final String line) {
        mDelegate.setHeaderLine(line);
        return this;
    }

    @Nullable
    @Override
    public String getHeader(@Nonnull final String name) {
        return mDelegate.getHeader(name);
    }

    @Override
    public boolean isChunked() {
        return mDelegate.isChunked();
    }

    @Override
    public boolean isKeepAlive() {
        return mDelegate.isKeepAlive();
    }

    @Override
    public int getContentLength() {
        return mDelegate.getContentLength();
    }

    @Nonnull
    @Override
    public HttpResponse setBody(@Nullable final String body) {
        mDelegate.setBody(body);
        return this;
    }

    @Nonnull
    @Override
    public HttpResponse setBody(
            @Nullable final String body,
            final boolean withContentLength) {
        mDelegate.setBody(body, withContentLength);
        return this;
    }

    @Nonnull
    @Override
    public HttpResponse setBodyBinary(@Nullable final byte[] body) {
        mDelegate.setBodyBinary(body);
        return this;
    }

    @Nonnull
    @Override
    public HttpResponse setBodyBinary(
            @Nullable final byte[] body,
            final boolean withContentLength) {
        mDelegate.setBodyBinary(body, withContentLength);
        return this;
    }

    @Nullable
    @Override
    public String getBody() {
        return mDelegate.getBody();
    }

    @Nullable
    @Override
    public byte[] getBodyBinary() {
        return mDelegate.getBodyBinary();
    }

    @Nonnull
    @Override
    public String getMessageString() {
        return mDelegate.getMessageString();
    }

    @Override
    public void writeData(@Nonnull final OutputStream os) throws IOException {
        mDelegate.writeData(os);
    }

    @Override
    public void readData(@Nonnull final InputStream is) throws IOException {
        mDelegate.readData(is);
    }

    @Override
    public String toString() {
        return mDelegate.toString();
    }
}
