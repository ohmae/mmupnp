/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import com.sun.istack.internal.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;

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

    @Override
    public void setStartLine(@NotNull String line) throws IllegalArgumentException {
        setRequestLine(line);
    }

    /**
     * リクエストラインを設定する。
     *
     * {@link #setStartLine(String)}のエイリアス。
     *
     * @param line リクエストライン
     * @see #setStartLine(String)
     */
    public void setRequestLine(@NotNull String line) throws IllegalArgumentException {
        final String[] params = line.split(" ");
        if (params.length < 3) {
            throw new IllegalArgumentException();
        }
        setMethod(params[0]);
        setUri(params[1]);
        setVersion(params[2]);
    }

    @Override
    @NotNull
    public String getStartLine() {
        return getMethod() + " " + getUri() + " " + getVersion();
    }

    /**
     * 送信先URLを設定する。
     *
     * @param url 接続先URL
     * @throws IOException http以外を指定した場合、URLのパースエラー
     */
    public void setUrl(@NotNull URL url) throws IOException {
        setUrl(url, false);
    }

    /**
     * 接続先URLを設定する。
     *
     * @param url 接続先URL
     * @param withHostHeader trueを指定するとURLにもとづいてHOSTヘッダの設定も行う
     * @throws IOException http以外を指定した場合、URLのパースエラー
     */
    public void setUrl(@NotNull URL url, boolean withHostHeader) throws IOException {
        if (!"http".equals(url.getProtocol())) {
            throw new IOException("unsupported protocol." + url.getProtocol());
        }
        setAddress(InetAddress.getByName(url.getHost()));
        int port = url.getPort();
        if (port < 0) {
            port = 80;
        }
        setPort(port);
        setUri(url.getFile());
        if (withHostHeader) {
            setHeader(Http.HOST, getAddressString());
        }
    }

    /**
     * リクエストメソッドを返す。
     *
     * @return リクエストメソッド
     */
    @NotNull
    public String getMethod() {
        return mMethod;
    }

    /**
     * リクエストメソッドを設定する。
     *
     * @param method リクエストメソッド
     */
    public void setMethod(@NotNull String method) {
        mMethod = method;
    }

    /**
     * URI（リクエストパス）を返す。
     *
     * @return URI
     */
    @NotNull
    public String getUri() {
        return mUri;
    }

    /**
     * URI（リクエストパス）を設定する。
     *
     * @param uri URI
     */
    public void setUri(@NotNull String uri) {
        mUri = uri;
    }
}
