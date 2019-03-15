/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.internal.message.HttpMessageDelegate;
import net.mm2d.upnp.internal.message.HttpMessageDelegate.StartLineDelegate;
import net.mm2d.upnp.util.NetworkUtils;
import net.mm2d.upnp.util.TextUtils;

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
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class HttpRequest implements HttpMessage {
    @Nonnull
    private final HttpMessageDelegate mDelegate;
    @Nullable
    private InetAddress mAddress;

    private int mPort;
    @Nonnull
    private final StartLine mStartLine;

    static class StartLine implements StartLineDelegate {
        @Nonnull
        private String mMethod;
        @Nonnull
        private String mUri;
        @Nonnull
        private String mVersion;

        StartLine() {
            mMethod = Http.GET;
            mUri = "";
            mVersion = Http.DEFAULT_HTTP_VERSION;
        }

        StartLine(@Nonnull final StartLine original) {
            mMethod = original.mMethod;
            mUri = original.mUri;
            mVersion = original.mVersion;
        }

        @Nonnull
        public String getMethod() {
            return mMethod;
        }

        public void setMethod(@Nonnull final String method) {
            mMethod = method;
        }

        @Nonnull
        public String getUri() {
            return mUri;
        }

        public void setUri(@Nonnull final String uri) {
            mUri = uri;
        }

        @Nonnull
        @Override
        public String getVersion() {
            return mVersion;
        }

        @Override
        public void setVersion(@Nonnull final String version) {
            mVersion = version;
        }

        @Override
        public void setStartLine(@Nonnull final String line) {
            final String[] params = line.split(" ", 3);
            if (params.length < 3) {
                throw new IllegalArgumentException();
            }
            setMethod(params[0]);
            setUri(params[1]);
            setVersion(params[2]);
        }

        @Nonnull
        @Override
        public String getStartLine() {
            return getMethod() + " " + getUri() + " " + getVersion();
        }
    }

    /**
     * インスタンス作成。
     *
     * @return インスタンス
     */
    @Nonnull
    public static HttpRequest create() {
        final StartLine startLine = new StartLine();
        final HttpMessageDelegate delegate = new HttpMessageDelegate(startLine);
        return new HttpRequest(startLine, delegate);
    }

    /**
     * 引数のインスタンスと同一の内容を持つインスタンスを作成する。
     *
     * @param original コピー元
     * @return インスタンス
     */
    @Nonnull
    public static HttpRequest copy(@Nonnull final HttpRequest original) {
        final StartLine startLine = new StartLine(original.mStartLine);
        final HttpMessageDelegate delegate = new HttpMessageDelegate(startLine, original.mDelegate);
        final HttpRequest request = new HttpRequest(startLine, delegate);
        request.mAddress = original.mAddress;
        request.mPort = original.mPort;
        return request;
    }

    // VisibleForTesting
    HttpRequest(
            @Nonnull final StartLine startLine,
            @Nonnull final HttpMessageDelegate delegate) {
        mStartLine = startLine;
        mDelegate = delegate;
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
    // VisibleForTesting
    void setAddress(@Nullable final InetAddress address) {
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
    // VisibleForTesting
    void setPort(final int port) {
        mPort = port;
    }

    /**
     * アドレスとポート番号の組み合わせ文字列を返す。
     *
     * @return アドレスとポート番号の組み合わせ文字列
     */
    // VisibleForTesting
    @Nonnull
    String getAddressString() throws IllegalStateException {
        if (mAddress == null) {
            throw new IllegalStateException("address must be set");
        }
        return NetworkUtils.getAddressString(mAddress, mPort);
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
    public void setStartLine(@Nonnull final String line) {
        mStartLine.setStartLine(line);
    }

    @Nonnull
    @Override
    public String getStartLine() {
        return mStartLine.getStartLine();
    }

    /**
     * 送信先URLを設定する。
     *
     * @param url 接続先URL
     * @throws IOException http以外を指定した場合、URLのパースエラー
     */
    public void setUrl(@Nonnull final URL url) throws IOException {
        setUrl(url, false);
    }

    /**
     * 接続先URLを設定する。
     *
     * @param url            接続先URL
     * @param withHostHeader trueを指定するとURLにもとづいてHOSTヘッダの設定も行う
     * @throws IOException http以外を指定した場合、URLのパースエラー
     */
    public void setUrl(
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
    }

    /**
     * リクエストメソッドを返す。
     *
     * @return リクエストメソッド
     */
    @Nonnull
    public String getMethod() {
        return mStartLine.getMethod();
    }

    /**
     * リクエストメソッドを設定する。
     *
     * @param method リクエストメソッド
     */
    public void setMethod(@Nonnull final String method) {
        mStartLine.setMethod(method);
    }

    /**
     * URI（リクエストパス）を返す。
     *
     * @return URI
     */
    @Nonnull
    public String getUri() {
        return mStartLine.getUri();
    }

    /**
     * URI（リクエストパス）を設定する。
     *
     * <p>接続先の設定ではなくパスのみの設定</p>
     *
     * @param uri URI
     * @see #setUrl(URL)
     * @see #setUrl(URL, boolean)
     */
    public void setUri(@Nonnull final String uri) {
        mStartLine.setUri(uri);
    }

    @Nonnull
    @Override
    public String getVersion() {
        return mDelegate.getVersion();
    }

    @Override
    public void setVersion(@Nonnull final String version) {
        mDelegate.setVersion(version);
    }

    @Override
    public void setHeader(
            @Nonnull final String name,
            @Nonnull final String value) {
        mDelegate.setHeader(name, value);
    }

    @Override
    public void setHeaderLine(@Nonnull final String line) {
        mDelegate.setHeaderLine(line);
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

    @Override
    public void setBody(@Nullable final String body) {
        mDelegate.setBody(body);
    }

    @Override
    public void setBody(
            @Nullable final String body,
            final boolean withContentLength) {
        mDelegate.setBody(body, withContentLength);
    }

    @Override
    public void setBodyBinary(@Nullable final byte[] body) {
        mDelegate.setBodyBinary(body);
    }

    @Override
    public void setBodyBinary(
            @Nullable final byte[] body,
            final boolean withContentLength) {
        mDelegate.setBodyBinary(body, withContentLength);
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

    @Override
    public void readData(@Nonnull final InputStream inputStream) throws IOException {
        mDelegate.readData(inputStream);
    }

    @Nonnull
    @Override
    public String toString() {
        return mDelegate.toString();
    }
}

