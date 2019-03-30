/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.log.Logger;
import net.mm2d.upnp.internal.util.IoUtils;
import net.mm2d.upnp.util.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HTTP通信を行うクライアントソケット。
 *
 * <p>UPnPの通信でよく利用される小さなデータのやり取りに特化したもの。
 * 長大なデータのやり取りは想定していない。
 * 手軽に利用できることを優先し、効率などはあまり考慮されていない。
 * 原則同一のサーバに対する一連の通信ごとにインスタンスを作成する想定。
 *
 * <p>相手の応答がkeep-alive可能な応答であった場合はコネクションを切断せず、
 * 継続して利用するという、消極的なkeep-alive機能も提供する。
 *
 * <p>keep-alive状態であっても、post時に維持したコネクションと
 * 同一のホスト・ポートでない場合は切断、再接続を行う。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class HttpClient {
    private static final int REDIRECT_MAX = 2;
    @Nullable
    private Socket mSocket;
    private boolean mKeepAlive;
    @Nullable
    private InputStream mInputStream;
    @Nullable
    private OutputStream mOutputStream;
    @Nullable
    private InetAddress mLocalAddress;

    /**
     * インスタンス作成
     *
     * @see #setKeepAlive(boolean)
     */
    public HttpClient() {
        setKeepAlive(true);
    }

    /**
     * インスタンス作成
     *
     * @param keepAlive keep-alive通信を行う場合true
     * @see #setKeepAlive(boolean)
     */
    public HttpClient(final boolean keepAlive) {
        setKeepAlive(keepAlive);
    }

    /**
     * keep-alive設定がなされているか否かを返す。
     *
     * @return keep-alive設定がなされている場合true
     */
    public boolean isKeepAlive() {
        return mKeepAlive;
    }

    /**
     * keep-alive設定を行う。
     *
     * <p>デフォルトはtrue。
     * trueを指定した場合、応答がkeep-alive可能なものであればコネクションを継続する。
     * trueを指定しても、応答がkeep-alive可能でない場合はコネクションを切断する。
     * falseを指定した場合、応答の内容によらずコネクションは切断する。
     *
     * <p>また、true/falseどちらを指定した場合であっても、
     * postの引数で渡された{@link HttpRequest}の内容を変更することはない。
     * ヘッダへkeep-aliveの記述が必要な場合はpostをコールする前に、
     * {@link HttpRequest}へ設定しておく必要がある。
     *
     * @param keepAlive keep-aliveを行う場合true
     */
    public final void setKeepAlive(final boolean keepAlive) {
        mKeepAlive = keepAlive;
    }

    /**
     * リクエストを送信し、レスポンスを受信する。
     *
     * <p>利用するHTTPメソッドは引数に依存する。
     *
     * @param request 送信するリクエスト
     * @return 受信したレスポンス
     * @throws IOException 通信エラー
     */
    @Nonnull
    public HttpResponse post(@Nonnull final HttpRequest request) throws IOException {
        return post(request, 0);
    }

    /**
     * リクエストを送信し、レスポンスを受信する。
     *
     * <p>利用するHTTPメソッドは引数に依存する。
     *
     * @param request       送信するリクエスト
     * @param redirectDepth リダイレクトの深さ
     * @return 受信したレスポンス
     * @throws IOException 通信エラー
     */
    @Nonnull
    private HttpResponse post(
            @Nonnull final HttpRequest request,
            final int redirectDepth) throws IOException {
        confirmReuseSocket(request);
        final HttpResponse response;
        try {
            response = doRequest(request);
        } catch (final IOException e) {
            closeSocket();
            throw e;
        }
        if (!isKeepAlive() || !response.isKeepAlive()) {
            closeSocket();
        }
        return redirectIfNeeded(request, response, redirectDepth);
    }

    private void confirmReuseSocket(@Nonnull final HttpRequest request) {
        if (!canReuse(request)) {
            closeSocket();
        }
    }

    @Nonnull
    private HttpResponse doRequest(@Nonnull final HttpRequest request) throws IOException {
        if (isClosed()) {
            openSocket(request);
            return writeAndRead(request);
        } else {
            try {
                return writeAndRead(request);
            } catch (final IOException e) {
                // コネクションを再利用した場合はpeerから既に切断されていた可能性がある。
                // KeepAliveできないサーバである可能性があるのでKeepAliveを無効にしてリトライ
                Logger.w(() -> "retry:\n" + e.getMessage());
                setKeepAlive(false);
                closeSocket();
                openSocket(request);
                return writeAndRead(request);
            }
        }
    }

    @SuppressWarnings("ConstantConditions") // open状態でのみコールする
    @Nonnull
    private HttpResponse writeAndRead(@Nonnull final HttpRequest request) throws IOException {
        request.writeData(mOutputStream);
        final HttpResponse response = HttpResponse.create();
        response.readData(mInputStream);
        return response;
    }

    @Nonnull
    private HttpResponse redirectIfNeeded(
            @Nonnull final HttpRequest request,
            @Nonnull final HttpResponse response,
            final int redirectDepth)
            throws IOException {
        if (needToRedirect(response) && redirectDepth < REDIRECT_MAX) {
            final String location = response.getHeader(Http.LOCATION);
            if (!TextUtils.isEmpty(location)) {
                return redirect(request, location, redirectDepth);
            }
        }
        return response;
    }

    private boolean needToRedirect(@Nonnull final HttpResponse response) {
        final Http.Status status = response.getStatus();
        switch (status) {
            case HTTP_MOVED_PERM:
            case HTTP_FOUND:
            case HTTP_SEE_OTHER:
            case HTTP_TEMP_REDIRECT:
                return true;
            default:
                return false;
        }
    }

    @Nonnull
    private HttpResponse redirect(
            @Nonnull final HttpRequest request,
            @Nonnull final String location,
            final int redirectDepth)
            throws IOException {
        final HttpRequest newRequest = HttpRequest.copy(request);
        newRequest.setUrl(new URL(location), true);
        newRequest.setHeader(Http.CONNECTION, Http.CLOSE);
        return new HttpClient(false).post(newRequest, redirectDepth + 1);
    }

    // VisibleForTesting
    boolean canReuse(@Nonnull final HttpRequest request) {
        return mSocket != null
                && mSocket.isConnected()
                && mSocket.getInetAddress().equals(request.getAddress())
                && mSocket.getPort() == request.getPort();
    }

    // VisibleForTesting
    boolean isClosed() {
        return mSocket == null;
    }

    private void openSocket(@Nonnull final HttpRequest request) throws IOException {
        mSocket = new Socket();
        mSocket.connect(request.getSocketAddress(), Property.DEFAULT_TIMEOUT);
        mSocket.setSoTimeout(Property.DEFAULT_TIMEOUT);
        mInputStream = new BufferedInputStream(mSocket.getInputStream());
        mOutputStream = new BufferedOutputStream(mSocket.getOutputStream());
        mLocalAddress = mSocket.getLocalAddress();
    }

    private void closeSocket() {
        IoUtils.closeQuietly(mInputStream);
        IoUtils.closeQuietly(mOutputStream);
        IoUtils.closeQuietly(mSocket);
        mInputStream = null;
        mOutputStream = null;
        mSocket = null;
    }

    /**
     * ソケットのクローズを行う。
     */
    public void close() {
        closeSocket();
    }

    /**
     * ソケットが使用したローカルアドレスを返す。
     *
     * <p>connectの後保存し、次の接続まで保持される。
     *
     * @return ソケットが使用したローカルアドレス
     */
    @Nullable
    public InetAddress getLocalAddress() {
        return mLocalAddress;
    }

    /**
     * 単純なHTTP GETにより文字列を取得する。
     *
     * @param url 取得先URL
     * @return 取得できた文字列
     * @throws IOException 取得に問題があった場合
     */
    @SuppressWarnings("ConstantConditions") // download()の中でgetBody()がnullで無いことはチェック済み
    @Nonnull
    public String downloadString(@Nonnull final URL url) throws IOException {
        return download(url).getBody();
    }

    /**
     * 単純なHTTP GETによりバイナリを取得する。
     *
     * @param url 取得先URL
     * @return 取得できたバイナリ
     * @throws IOException 取得に問題があった場合
     */
    @SuppressWarnings("ConstantConditions") // download()の中でgetBody()がnullで無いことはチェック済み
    @Nonnull
    public byte[] downloadBinary(@Nonnull final URL url) throws IOException {
        return download(url).getBodyBinary();
    }

    /**
     * 単純なHTTP GETを実行する。
     *
     * @param url 取得先URL
     * @return HTTPレスポンス
     * @throws IOException 取得に問題があった場合
     */
    @Nonnull
    public HttpResponse download(@Nonnull final URL url) throws IOException {
        final HttpRequest request = makeHttpRequest(url);
        final HttpResponse response = post(request);
        // response bodyがemptyであることは正常
        if (response.getStatus() != Http.Status.HTTP_OK || response.getBody() == null) {
            Logger.i(() -> "request:\n" + request + "\nresponse:\n" + response);
            throw new IOException(response.getStartLine());
        }
        return response;
    }

    @Nonnull
    private HttpRequest makeHttpRequest(@Nonnull final URL url) throws IOException {
        final HttpRequest request = HttpRequest.create();
        request.setMethod(Http.GET);
        request.setUrl(url, true);
        request.setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE);
        request.setHeader(Http.CONNECTION, isKeepAlive() ? Http.KEEP_ALIVE : Http.CLOSE);
        return request;
    }
}
