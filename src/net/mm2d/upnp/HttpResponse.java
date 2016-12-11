/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HTTPレスポンスメッセージを表現するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class HttpResponse extends HttpMessage {
    private Http.Status mStatus;
    private int mStatusCode;
    private String mReasonPhrase = "";

    @Override
    public void setStartLine(@Nonnull String line) throws IllegalArgumentException {
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
    public void setStatusLine(@Nonnull String line) throws IllegalArgumentException {
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
        if (mStatus != null) {
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
     * @see #setStatus(net.mm2d.upnp.Http.Status)
     */
    public void setStatusCode(int code) {
        mStatus = Http.Status.valueOf(code);
        if (mStatus == null) {
            throw new IllegalArgumentException("unexpected status code:" + code);
        }
        mStatusCode = code;
        mReasonPhrase = mStatus.getPhrase();
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
     * @see #setStatus(net.mm2d.upnp.Http.Status)
     */
    public void setReasonPhrase(@Nonnull String reasonPhrase) {
        mReasonPhrase = reasonPhrase;
    }

    /**
     * ステータスを設定する。
     *
     * @param status ステータス
     */
    public void setStatus(@Nonnull Http.Status status) {
        mStatus = status;
        mStatusCode = status.getCode();
        mReasonPhrase = status.getPhrase();
    }

    /**
     * ステータスを取得する。
     *
     * @return ステータス
     */
    @Nullable
    public Http.Status getStatus() {
        return mStatus;
    }
}
