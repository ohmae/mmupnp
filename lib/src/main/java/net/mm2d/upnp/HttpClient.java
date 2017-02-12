/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.IoUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;

import javax.annotation.Nonnull;

/**
 * HTTP通信を行うクライアントソケット。
 *
 * <p>UPnPの通信でよく利用される小さなデータのやり取りに特化したもの。
 * 長大なデータのやり取りは想定していない。
 * 手軽に利用できることを優先し、効率などはあまり考慮されていない。
 *
 * <p>相手の応答がkeep-alive可能な応答であった場合はコネクションを切断せず、
 * 継続して利用するという、消極的なkeep-alive機能も提供する。
 *
 * <p>keep-alive状態であっても、post時に維持したコネクションと同一のホスト・ポートでない場合は
 * 切断、再接続を行う。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class HttpClient {
    private static final int REDIRECT_MAX = 2;
    private Socket mSocket;
    private boolean mKeepAlive;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

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
    public HttpClient(boolean keepAlive) {
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
    public void setKeepAlive(boolean keepAlive) {
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
    public HttpResponse post(@Nonnull HttpRequest request) throws IOException {
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
    private HttpResponse post(@Nonnull HttpRequest request, int redirectDepth) throws IOException {
        if (!isClosed() && !canReuse(request)) {
            closeSocket();
        }
        try {
            if (isClosed()) {
                openSocket(request);
                request.writeData(mOutputStream);
            } else {
                try {
                    request.writeData(mOutputStream);
                } catch (final IOException e) {
                    // コネクションを再利用した場合は
                    // peerから既に切断されていた可能性があるためリトライを行う
                    closeSocket();
                    openSocket(request);
                    request.writeData(mOutputStream);
                }
            }
            final HttpResponse response = new HttpResponse(mSocket);
            response.readData(mInputStream);
            if (!isKeepAlive() || !response.isKeepAlive()) {
                closeSocket();
            }
            if (redirectDepth < REDIRECT_MAX
                    && isRedirection(response.getStatus())) {
                String location = response.getHeader(Http.LOCATION);
                if (location != null) {
                    return redirect(request, location, redirectDepth);
                }
            }
            return response;
        } catch (final IOException e) {
            closeSocket();
            throw e;
        }
    }

    private static boolean isRedirection(Http.Status status) {
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
    private HttpResponse redirect(@Nonnull HttpRequest request, @Nonnull String location, int redirectDepth)
            throws IOException {
        HttpRequest newRequest = new HttpRequest(request);
        newRequest.setUrl(new URL(location), true);
        newRequest.setHeader(Http.CONNECTION, Http.CLOSE);
        return new HttpClient(false).post(newRequest, redirectDepth + 1);
    }

    private boolean canReuse(@Nonnull HttpRequest request) {
        return mSocket.isConnected()
                && mSocket.getInetAddress().equals(request.getAddress())
                && mSocket.getPort() == request.getPort();
    }

    private boolean isClosed() {
        return mSocket == null;
    }

    private void openSocket(@Nonnull HttpRequest request) throws IOException {
        mSocket = new Socket();
        mSocket.connect(request.getSocketAddress(), Property.DEFAULT_TIMEOUT);
        mSocket.setSoTimeout(Property.DEFAULT_TIMEOUT);
        mInputStream = new BufferedInputStream(mSocket.getInputStream());
        mOutputStream = new BufferedOutputStream(mSocket.getOutputStream());
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
}
