/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.Http.Status;

import java.net.Socket;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HTTPレスポンスメッセージを表現するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class HttpResponse extends HttpMessage {
    @Nonnull
    private Http.Status mStatus = Status.HTTP_INVALID;
    private int mStatusCode;
    @Nonnull
    private String mReasonPhrase = "";

    /**
     * インスタンス作成。
     */
    public HttpResponse() {
        super();
    }

    /**
     * インスタンス作成
     *
     * @param socket 受信したsocket
     */
    public HttpResponse(@Nonnull final Socket socket) {
        super(socket);
    }

    @Override
    public void setStartLine(@Nonnull final String line) throws IllegalArgumentException {
        setStatusLine(line);
    }

    /**
     * ステータスラインを設定する。
     *
     * <p>{@link #setStartLine(String)}のエイリアス。
     *
     * @param line ステータスライン
     * @see #setStartLine(String)
     */
    public void setStatusLine(@Nonnull final String line) throws IllegalArgumentException {
        final String[] params = line.split(" ");
        if (params.length < 3) {
            throw new IllegalArgumentException();
        }
        setVersion(params[0]);
        setStatusCode(Integer.parseInt(params[1]));
        setReasonPhrase(params[2]);
    }

    @Override
    @Nonnull
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

    @Override
    public HttpResponse setVersion(@Nonnull final String version) {
        return (HttpResponse) super.setVersion(version);
    }

    @Override
    public HttpResponse setHeader(
            @Nonnull final String name,
            @Nonnull final String value) {
        return (HttpResponse) super.setHeader(name, value);
    }

    @Override
    public HttpResponse setBody(@Nullable final String body) {
        return (HttpResponse) super.setBody(body);
    }

    @Override
    public HttpResponse setBody(
            @Nullable final String body,
            final boolean withContentLength) {
        return (HttpResponse) super.setBody(body, withContentLength);
    }

    @Override
    public HttpResponse setBodyBinary(@Nullable final byte[] body) {
        return (HttpResponse) super.setBodyBinary(body);
    }

    @Override
    public HttpResponse setBodyBinary(
            @Nullable final byte[] body,
            final boolean withContentLength) {
        return (HttpResponse) super.setBodyBinary(body, withContentLength);
    }
}
