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

/**
 * HTTPリクエストメッセージを表現するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class HttpRequest extends HttpMessage {
    private String mMethod = "GET";
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
    public HttpRequest(Socket socket) {
        super(socket);
    }

    /**
     * 引数のインスタンスと同一の内容を持つインスタンスを作成する。
     *
     * @param original コピー元
     */
    public HttpRequest(HttpRequest original) {
        super(original);
        mMethod = original.mMethod;
        mUri = original.mUri;
    }

    @Override
    public void setStartLine(@Nonnull String line) throws IllegalArgumentException {
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
    public void setRequestLine(@Nonnull String line) throws IllegalArgumentException {
        final String[] params = line.split(" ");
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
     * @throws IOException http以外を指定した場合、URLのパースエラー
     */
    public void setUrl(@Nonnull URL url) throws IOException {
        setUrl(url, false);
    }

    /**
     * 接続先URLを設定する。
     *
     * @param url            接続先URL
     * @param withHostHeader trueを指定するとURLにもとづいてHOSTヘッダの設定も行う
     * @throws IOException http以外を指定した場合、URLのパースエラー
     */
    public void setUrl(@Nonnull URL url, boolean withHostHeader) throws IOException {
        if (!TextUtils.equals(url.getProtocol(), "http")) {
            throw new IOException("unsupported protocol." + url.getProtocol());
        }
        final String host = url.getHost();
        setAddress(InetAddress.getByName(host));
        int port = url.getPort();
        if (port < 0) {
            port = 80;
        }
        setPort(port);
        setUri(url.getFile());
        if (withHostHeader) {
            if (port != Http.DEFAULT_PORT) {
                setHeader(Http.HOST, host + ":" + port);
            } else {
                setHeader(Http.HOST, host);
            }
        }
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
     */
    public void setMethod(@Nonnull String method) {
        mMethod = method;
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
     * @param uri URI
     */
    public void setUri(@Nonnull String uri) {
        mUri = uri;
    }
}
