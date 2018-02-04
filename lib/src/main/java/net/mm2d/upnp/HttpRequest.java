/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.HttpMessageDelegate.StartLineProcessor;
import net.mm2d.util.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HTTPリクエストメッセージを表現するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class HttpRequest implements HttpMessage {
    @Nonnull
    private final HttpMessage mDelegate;
    @Nullable
    private InetAddress mAddress;

    private int mPort;

    @Nonnull
    private String mMethod = "GET";
    @Nonnull
    private String mUri = "";

    private class Processor implements StartLineProcessor {
        @Override
        public void setStartLine(@Nonnull final String line) {
            HttpRequest.this.setStartLine(line);
        }

        @Nonnull
        @Override
        public String getStartLine() {
            return HttpRequest.this.getStartLine();
        }
    }

    ;

    /**
     * インスタンス作成。
     */
    public HttpRequest() {
        mDelegate = new HttpMessageDelegate(new Processor());
    }

    // VisibleForTesting
    HttpRequest(@Nonnull HttpMessage delegate) {
        mDelegate = delegate;
    }

    /**
     * 引数のインスタンスと同一の内容を持つインスタンスを作成する。
     *
     * @param original コピー元
     */
    public HttpRequest(@Nonnull final HttpRequest original) {
        mDelegate = new HttpMessageDelegate(new Processor(), (HttpMessageDelegate) original.mDelegate);
        mAddress = original.mAddress;
        mPort = original.mPort;
        mMethod = original.mMethod;
        mUri = original.mUri;
    }

    /**
     * 宛先アドレス情報を返す。
     *
     * @return 宛先アドレス情報。
     */
    @Nullable
    public InetAddress getAddress() {
        return mAddress;
    }

    /**
     * 宛先アドレスを登録する。
     *
     * @param address 宛先アドレス。
     */
    protected void setAddress(@Nullable final InetAddress address) {
        mAddress = address;
    }

    /**
     * 宛先ポート番号を返す。
     *
     * @return 宛先ポート番号
     */
    public int getPort() {
        return mPort;
    }

    /**
     * 宛先ポート番号を設定する。
     *
     * @param port 宛先ポート番号
     */
    protected void setPort(final int port) {
        mPort = port;
    }

    /**
     * アドレスとポート番号の組み合わせ文字列を返す。
     *
     * @return アドレスとポート番号の組み合わせ文字列
     */
    @Nonnull
    public String getAddressString() throws IllegalStateException {
        if (mAddress == null) {
            throw new IllegalStateException("address must be set");
        }
        if (mPort == Http.DEFAULT_PORT || mPort <= 0) {
            return mAddress.getHostAddress();
        }
        return mAddress.getHostAddress() + ":" + String.valueOf(mPort);
    }

    /**
     * 宛先SocketAddressを返す
     *
     * @return 宛先SocketAddress
     */
    @Nonnull
    public SocketAddress getSocketAddress() throws IllegalStateException {
        if (mAddress == null) {
            throw new IllegalStateException("address must be set");
        }
        return new InetSocketAddress(mAddress, mPort);
    }

    @Override
    public HttpRequest setStartLine(@Nonnull final String line) {
        return setRequestLine(line);
    }

    /**
     * リクエストラインを設定する。
     *
     * <p>{@link #setStartLine(String)}のエイリアス。
     *
     * @param line リクエストライン
     * @see #setStartLine(String)
     */
    public HttpRequest setRequestLine(@Nonnull final String line) {
        final String[] params = line.split(" ", 3);
        if (params.length < 3) {
            throw new IllegalArgumentException();
        }
        setMethod(params[0]);
        setUri(params[1]);
        setVersion(params[2]);
        return this;
    }

    @Nonnull
    @Override
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

    @Nonnull
    @Override
    public String getVersion() {
        return mDelegate.getVersion();
    }

    @Nonnull
    @Override
    public HttpRequest setVersion(@Nonnull final String version) {
        mDelegate.setVersion(version);
        return this;
    }

    @Nonnull
    @Override
    public HttpRequest setHeader(
            @Nonnull final String name,
            @Nonnull final String value) {
        mDelegate.setHeader(name, value);
        return this;
    }

    @Nonnull
    @Override
    public HttpRequest setHeaderLine(@Nonnull final String line) {
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
    public HttpRequest setBody(@Nullable final String body) {
        mDelegate.setBody(body);
        return this;
    }

    @Nonnull
    @Override
    public HttpRequest setBody(
            @Nullable final String body,
            final boolean withContentLength) {
        mDelegate.setBody(body, withContentLength);
        return this;
    }

    @Nonnull
    @Override
    public HttpRequest setBodyBinary(@Nullable final byte[] body) {
        mDelegate.setBodyBinary(body);
        return this;
    }

    @Nonnull
    @Override
    public HttpRequest setBodyBinary(
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

