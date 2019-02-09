/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message;

import net.mm2d.upnp.Http;
import net.mm2d.upnp.Http.Status;
import net.mm2d.upnp.HttpMessage;
import net.mm2d.upnp.internal.message.HttpMessageDelegate.StartLineDelegate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HTTPレスポンスメッセージを表現するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class HttpResponse implements HttpMessage {
    @Nonnull
    private final HttpMessageDelegate mDelegate;
    @Nonnull
    private final StartLine mStartLine;

    static class StartLine implements StartLineDelegate {
        @Nonnull
        private Status mStatus;

        private int mStatusCode;
        @Nonnull
        private String mReasonPhrase;
        @Nonnull
        private String mVersion;

        StartLine() {
            mStatus = Status.HTTP_INVALID;
            mStatusCode = 0;
            mReasonPhrase = "";
            mVersion = Http.DEFAULT_HTTP_VERSION;
        }

        StartLine(@Nonnull final StartLine original) {
            mStatus = original.mStatus;
            mStatusCode = original.mStatusCode;
            mReasonPhrase = original.mReasonPhrase;
            mVersion = original.mVersion;
        }

        int getStatusCode() {
            return mStatusCode;
        }

        void setStatusCode(final int code) {
            mStatus = Status.valueOf(code);
            if (mStatus == Status.HTTP_INVALID) {
                throw new IllegalArgumentException("unexpected status code:" + code);
            }
            mStatusCode = code;
            mReasonPhrase = mStatus.getPhrase();
        }

        @Nonnull
        String getReasonPhrase() {
            return mReasonPhrase;
        }

        void setReasonPhrase(@Nonnull final String reasonPhrase) {
            mReasonPhrase = reasonPhrase;
        }

        void setStatus(@Nonnull final Status status) {
            mStatus = status;
            mStatusCode = status.getCode();
            mReasonPhrase = status.getPhrase();
        }

        @Nonnull
        public Status getStatus() {
            return mStatus;
        }

        @Nonnull
        @Override
        public String getVersion() {
            return mVersion;
        }

        @Nonnull
        @Override
        public void setVersion(@Nonnull final String version) {
            mVersion = version;
        }

        @Nonnull
        @Override
        public String getStartLine() {
            final StringBuilder sb = new StringBuilder();
            sb.append(getVersion());
            sb.append(' ');
            if (mStatus != Status.HTTP_INVALID) {
                sb.append(mStatus.getCode());
                sb.append(' ');
                sb.append(mStatus.getPhrase());
            } else {
                sb.append(mStatusCode);
                sb.append(' ');
                sb.append(getReasonPhrase());
            }
            return sb.toString();
        }

        @Override
        public void setStartLine(@Nonnull final String line) {
            final String[] params = line.split(" ", 3);
            if (params.length < 3) {
                throw new IllegalArgumentException();
            }
            setVersion(params[0]);
            setStatusCode(Integer.parseInt(params[1]));
            setReasonPhrase(params[2]);
        }
    }

    /**
     * インスタンス作成。
     */
    public static HttpResponse create() {
        final StartLine startLine = new StartLine();
        final HttpMessageDelegate delegate = new HttpMessageDelegate(startLine);
        return new HttpResponse(startLine, delegate);
    }

    public static HttpResponse copy(@Nonnull final HttpResponse original) {
        final StartLine startLine = new StartLine(original.mStartLine);
        final HttpMessageDelegate delegate = new HttpMessageDelegate(startLine, original.mDelegate);
        return new HttpResponse(startLine, delegate);
    }

    // VisibleForTesting
    HttpResponse(
            @Nonnull final StartLine startLine,
            @Nonnull final HttpMessageDelegate delegate) {
        mStartLine = startLine;
        mDelegate = delegate;
    }

    @Nonnull
    @Override
    public HttpResponse setStartLine(@Nonnull final String line) {
        mStartLine.setStartLine(line);
        return this;
    }

    @Nonnull
    @Override
    public String getStartLine() {
        return mStartLine.getStartLine();
    }

    /**
     * ステータスコードを返す
     *
     * @return ステータスコード
     * @see #getStatus()
     */
    public int getStatusCode() {
        return mStartLine.getStatusCode();
    }

    /**
     * ステータスコードを設定する。
     *
     * @param code ステータスコード
     * @return HttpResponse
     * @see #setStatus(net.mm2d.upnp.Http.Status)
     */
    @Nonnull
    public HttpResponse setStatusCode(final int code) {
        mStartLine.setStatusCode(code);
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
        return mStartLine.getReasonPhrase();
    }

    /**
     * レスポンスフレーズを設定する。
     *
     * @param reasonPhrase レスポンスフレーズ
     * @return HttpResponse
     * @see #setStatus(net.mm2d.upnp.Http.Status)
     */
    @SuppressWarnings("UnusedReturnValue")
    @Nonnull
    public HttpResponse setReasonPhrase(@Nonnull final String reasonPhrase) {
        mStartLine.setReasonPhrase(reasonPhrase);
        return this;
    }

    /**
     * ステータスを設定する。
     *
     * @param status ステータス
     * @return HttpResponse
     */
    @Nonnull
    public HttpResponse setStatus(@Nonnull final Status status) {
        mStartLine.setStatus(status);
        return this;
    }

    /**
     * ステータスを取得する。
     *
     * @return ステータス
     */
    @Nonnull
    public Status getStatus() {
        return mStartLine.getStatus();
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
    public void writeData(@Nonnull final OutputStream outputStream) throws IOException {
        mDelegate.writeData(outputStream);
    }

    @Nonnull
    @Override
    public HttpResponse readData(@Nonnull final InputStream inputStream) throws IOException {
        mDelegate.readData(inputStream);
        return this;
    }

    @Nonnull
    @Override
    public String toString() {
        return mDelegate.toString();
    }
}
