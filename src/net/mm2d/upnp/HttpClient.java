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

import javax.annotation.Nonnull;

/**
 * HTTP通信を行うクライアントソケット。
 *
 * <p>UPnPの通信でよく利用される小さなデータのやり取りに特化したもの。
 * 長大なデータのやり取りは想定していない。
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
        if (!isClosed()) {
            if (!canReuse(request)) {
                closeSocket();
            }
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
                    // 既に切断されていた可能性があるためリトライを行う
                    closeSocket();
                    openSocket(request);
                    request.writeData(mOutputStream);
                }
            }
            final HttpResponse response = new HttpResponse();
            response.setAddress(mSocket.getInetAddress());
            response.setPort(mSocket.getPort());
            response.readData(mInputStream);
            if (!isKeepAlive() || !response.isKeepAlive()) {
                closeSocket();
            }
            return response;
        } catch (final IOException e) {
            closeSocket();
            throw e;
        }
    }

    private boolean canReuse(HttpRequest request) {
        return mSocket.getInetAddress().equals(request.getAddress())
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
