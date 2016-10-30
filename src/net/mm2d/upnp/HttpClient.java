/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

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
 * UPnPの通信でよく利用される小さなデータのやり取りに特化したもの。
 * 長大なデータのやり取りは想定していない。
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
     */
    public HttpClient() {
    }

    /**
     * インスタンス作成
     *
     * @param keepAlive keep-alive通信を行う場合
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
     * @param keepAlive keep-aliveを行う場合true
     */
    public void setKeepAlive(boolean keepAlive) {
        mKeepAlive = keepAlive;
    }

    /**
     * リクエストを送信し、レスポンスを受信する
     *
     * @param request 送信するリクエスト
     * @return 受信したレスポンス
     * @throws IOException 通信エラー
     */
    @Nonnull
    public HttpResponse post(HttpRequest request) throws IOException {
        if (mSocket != null) {
            if (!mSocket.getInetAddress().equals(request.getAddress())
                    || mSocket.getPort() != request.getPort()) {
                closeSocket();
            }
        }
        try {
            if (mSocket == null) {
                openSocket(request);
            }
            request.writeData(mOutputStream);
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

    private void openSocket(@Nonnull HttpRequest request) throws IOException {
        mSocket = new Socket();
        mSocket.connect(request.getSocketAddress(), Property.DEFAULT_TIMEOUT);
        mSocket.setSoTimeout(Property.DEFAULT_TIMEOUT);
        mInputStream = new BufferedInputStream(mSocket.getInputStream());
        mOutputStream = new BufferedOutputStream(mSocket.getOutputStream());
    }

    private void closeSocket() {
        try {
            if (mInputStream != null) {
                mInputStream.close();
            }
        } catch (final IOException ignored) {
        }
        mInputStream = null;
        try {
            if (mOutputStream != null) {
                mOutputStream.close();
            }
        } catch (final IOException ignored) {
        }
        mOutputStream = null;
        try {
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (final IOException ignored) {
        }
        mSocket = null;
    }

    /**
     * ソケットのクローズを行う。
     */
    public void close() {
        closeSocket();
    }
}
