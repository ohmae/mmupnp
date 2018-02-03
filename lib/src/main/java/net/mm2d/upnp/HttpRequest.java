/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.TextUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HTTPリクエストメッセージを表現するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class HttpRequest extends HttpMessage {
    @Nonnull
    private String mMethod = "GET";
    @Nonnull
    private String mUri = "";

    /**
     * インスタンス作成。
     */
    public HttpRequest() {
        super();
    }

    /**
     * インスタンス作成
     *
     * @param socket 受信したsocket
     */
    public HttpRequest(@Nonnull final Socket socket) {
        super(socket);
    }

    /**
     * 引数のインスタンスと同一の内容を持つインスタンスを作成する。
     *
     * @param original コピー元
     */
    public HttpRequest(@Nonnull final HttpRequest original) {
        super(original);
        mMethod = original.mMethod;
        mUri = original.mUri;
    }

    @Override
    public void setStartLine(@Nonnull final String line) throws IllegalArgumentException {
        setRequestLine(line);
    }

    /**
     * リクエストラインを設定する。
     *
     * <p>{@link #setStartLine(String)}のエイリアス。
     *
     * @param line リクエストライン
     * @see #setStartLine(String)
     */
    public void setRequestLine(@Nonnull final String line) throws IllegalArgumentException {
        final String[] params = line.split(" ", 3);
        if (params.length < 3) {
            throw new IllegalArgumentException();
        }
        setMethod(params[0]);
        setUri(params[1]);
        setVersion(params[2]);
    }

    @Override
    @Nonnull
    public String getStartLine() {
        return getMethod() + " " + getUri() + " " + getVersion();
    }

    /**
     * 送信先URLを設定する。
     *
     * @param url 接続先URL
     * @return HttpRequest
     * @throws IOException http以外を指定した場合、URLのパースエラー
     */
    public HttpRequest setUrl(@Nonnull final URL url) throws IOException {
        setUrl(url, false);
        return this;
    }

    /**
     * 接続先URLを設定する。
     *
     * @param url            接続先URL
     * @param withHostHeader trueを指定するとURLにもとづいてHOSTヘッダの設定も行う
     * @return HttpRequest
     * @throws IOException http以外を指定した場合、URLのパースエラー
     */
    public HttpRequest setUrl(
            @Nonnull final URL url,
            final boolean withHostHeader) throws IOException {
        if (!TextUtils.equals(url.getProtocol(), "http")) {
            throw new IOException("unsupported protocol." + url.getProtocol());
        }
        final String host = url.getHost();
        setAddress(InetAddress.getByName(host));
        int port = url.getPort();
        if (port > 65535) {
            throw new IOException("port number is too large. port=" + port);
        }
        if (port < 0) {
            port = Http.DEFAULT_PORT;
        }
        setPort(port);
        setUri(url.getFile());
        if (withHostHeader) {
            setHeader(Http.HOST, getAddressString());
        }
        return this;
    }

    /**
     * リクエストメソッドを返す。
     *
     * @return リクエストメソッド
     */
    @Nonnull
    public String getMethod() {
        return mMethod;
    }

    /**
     * リクエストメソッドを設定する。
     *
     * @param method リクエストメソッド
     * @return HttpRequest
     */
    public HttpRequest setMethod(@Nonnull final String method) {
        mMethod = method;
        return this;
    }

    /**
     * URI（リクエストパス）を返す。
     *
     * @return URI
     */
    @Nonnull
    public String getUri() {
        return mUri;
    }

    /**
     * URI（リクエストパス）を設定する。
     *
     * <p>接続先の設定ではなくパスのみの設定</p>
     *
     * @param uri URI
     * @return HttpRequest
     * @see #setUrl(URL)
     * @see #setUrl(URL, boolean)
     */
    public HttpRequest setUri(@Nonnull final String uri) {
        mUri = uri;
        return this;
    }

    @Override
    public HttpRequest setVersion(@Nonnull final String version) {
        super.setVersion(version);
        return this;
    }

    @Override
    public HttpRequest setHeader(
            @Nonnull final String name,
            @Nonnull final String value) {
        super.setHeader(name, value);
        return this;
    }

    @Override
    public HttpRequest setBody(@Nullable final String body) {
        super.setBody(body);
        return this;
    }

    @Override
    public HttpRequest setBody(
            @Nullable final String body,
            final boolean withContentLength) {
        super.setBody(body, withContentLength);
        return this;
    }

    @Override
    public HttpRequest setBodyBinary(@Nullable final byte[] body) {
        super.setBodyBinary(body);
        return this;
    }

    @Override
    public HttpRequest setBodyBinary(
            @Nullable final byte[] body,
            final boolean withContentLength) {
        super.setBodyBinary(body, withContentLength);
        return this;
    }
}
