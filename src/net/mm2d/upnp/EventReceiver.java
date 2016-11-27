/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.IoUtils;
import net.mm2d.util.Log;
import net.mm2d.util.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * イベント購読によって通知されるEventを受信するクラス。
 *
 * <p>HTTPのサーバとしてリクエストの受付のみを行う。
 * HTTPメッセージのパースはリスナーの実装側が行う。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class EventReceiver {
    private static final String TAG = EventReceiver.class.getSimpleName();

    /**
     * イベントデータの受信を受け取るリスナー。
     */
    public interface EventMessageListener {
        /**
         * イベント受信時にコール。
         *
         * @param request 受信したHTTPメッセージ
         * @return HTTPメッセージが正常であればtrue
         */
        boolean onEventReceived(@Nonnull HttpRequest request);
    }

    private ServerSocket mServerSocket;
    private ServerThread mServerThread;
    private EventMessageListener mListener;

    /**
     * インスタンス作成。
     */
    public EventReceiver() {
    }

    /**
     * イベント受信リスナーを登録する。
     *
     * @param listener リスナー
     */
    public void setEventMessageListener(@Nullable EventMessageListener listener) {
        mListener = listener;
        if (mServerThread != null) {
            mServerThread.setEventMessageListener(listener);
        }
    }

    /**
     * サーバソケットのオープンと受信スレッドの開始を行う。
     *
     * @throws IOException ソケットの作成に失敗
     */
    public void open() throws IOException {
        mServerSocket = new ServerSocket(0);
        mServerThread = new ServerThread(mServerSocket);
        mServerThread.setEventMessageListener(mListener);
        mServerThread.start();
    }

    /**
     * サーバーソケットに割り当てられたポート番号を返す。
     *
     * @return サーバソケットのポート番号
     */
    public int getLocalPort() {
        return mServerSocket.getLocalPort();
    }

    /**
     * 受信スレッドを終了させる。
     */
    public void close() {
        mServerThread.shutdownRequest();
        mServerThread = null;
    }

    private static class ServerThread extends Thread {
        private volatile boolean mShutdownRequest = false;
        private final ServerSocket mServerSocket;
        private final List<ClientThread> mClientList;
        private EventMessageListener mListener;

        /**
         * サーバソケットを指定してインスタンス作成。
         *
         * @param sock サーバソケット
         */
        public ServerThread(@Nonnull ServerSocket sock) {
            super("EventReceiver::ServerThread");
            mServerSocket = sock;
            mClientList = Collections.synchronizedList(new LinkedList<ClientThread>());
        }

        /**
         * 受信スレッドを終了させ、サーバソケットのクローズを行う。
         * クライアントからの接続がある場合は、
         * それらの受信スレッドを終了させ、クライアントソケットのクローズも行う。
         */
        public void shutdownRequest() {
            mShutdownRequest = true;
            interrupt();
            IoUtils.closeQuietly(mServerSocket);
            synchronized (mClientList) {
                for (final ClientThread client : mClientList) {
                    client.shutdownRequest();
                }
                mClientList.clear();
            }
        }

        /**
         * Clientスレッドからの終了通知
         *
         * @param client 終了したClientスレッド
         */
        public void notifyClientFinish(@Nonnull ClientThread client) {
            mClientList.remove(client);
        }

        /**
         * イベントリスナーの登録
         *
         * @param listener リスナー
         */
        public void setEventMessageListener(@Nonnull EventMessageListener listener) {
            mListener = listener;
        }

        /**
         * イベントリスナーのコール
         *
         * @param request 受信したHTTPメッセージ
         * @return HTTPメッセージが正常であればtrue
         */
        private boolean notifyEvent(@Nonnull HttpRequest request) {
            return mListener != null && mListener.onEventReceived(request);
        }

        @Override
        public void run() {
            try {
                while (!mShutdownRequest) {
                    final Socket sock = mServerSocket.accept();
                    sock.setSoTimeout(Property.DEFAULT_TIMEOUT);
                    final ClientThread client = new ClientThread(this, sock);
                    mClientList.add(client);
                    client.start();
                }
            } catch (final IOException ignored) {
            } finally {
                IoUtils.closeQuietly(mServerSocket);
            }
        }
    }

    private static class ClientThread extends Thread {
        private final ServerThread mServer;
        private final Socket mSocket;
        private static final HttpResponse RESPONSE_OK = new HttpResponse();
        private static final HttpResponse RESPONSE_BAD = new HttpResponse();
        private static final HttpResponse RESPONSE_FAIL = new HttpResponse();

        static {
            RESPONSE_OK.setStatus(Http.Status.HTTP_OK);
            RESPONSE_OK.setHeader(Http.SERVER, Http.SERVER_VALUE);
            RESPONSE_OK.setHeader(Http.CONNECTION, Http.CLOSE);
            RESPONSE_OK.setHeader(Http.CONTENT_LENGTH, "0");
            RESPONSE_BAD.setStatus(Http.Status.HTTP_BAD_REQUEST);
            RESPONSE_BAD.setHeader(Http.SERVER, Http.SERVER_VALUE);
            RESPONSE_BAD.setHeader(Http.CONNECTION, Http.CLOSE);
            RESPONSE_BAD.setHeader(Http.CONTENT_LENGTH, "0");
            RESPONSE_FAIL.setStatus(Http.Status.HTTP_PRECON_FAILED);
            RESPONSE_FAIL.setHeader(Http.SERVER, Http.SERVER_VALUE);
            RESPONSE_FAIL.setHeader(Http.CONNECTION, Http.CLOSE);
            RESPONSE_FAIL.setHeader(Http.CONTENT_LENGTH, "0");
        }

        /**
         * インスタンス作成
         *
         * @param server サーバスレッド
         * @param sock クライアントソケット
         */
        public ClientThread(@Nonnull ServerThread server, @Nonnull Socket sock) {
            super("EventReceiver::ClientThread");
            mServer = server;
            mSocket = sock;
        }

        /**
         * スレッドを終了させ、ソケットのクローズを行う。
         */
        public void shutdownRequest() {
            interrupt();
            IoUtils.closeQuietly(mSocket);
        }

        private boolean notifyEvent(@Nonnull HttpRequest request) {
            return mServer.notifyEvent(request);
        }

        @Override
        public void run() {
            try (final InputStream is = new BufferedInputStream(mSocket.getInputStream());
                    final OutputStream os = new BufferedOutputStream(mSocket.getOutputStream())) {
                final HttpRequest request = new HttpRequest();
                request.setAddress(mSocket.getInetAddress());
                request.setPort(mSocket.getPort());
                if (!request.readData(is)) {
                    return;
                }
                final String nt = request.getHeader(Http.NT);
                final String nts = request.getHeader(Http.NTS);
                final String sid = request.getHeader(Http.SID);
                if (TextUtils.isEmpty(nt) || TextUtils.isEmpty(nts)) {
                    RESPONSE_BAD.writeData(os);
                } else if (TextUtils.isEmpty(sid)
                        || !nt.equals(Http.UPNP_EVENT)
                        || !nts.equals(Http.UPNP_PROPCHANGE)) {
                    RESPONSE_FAIL.writeData(os);
                } else {
                    if (notifyEvent(request)) {
                        RESPONSE_OK.writeData(os);
                    } else {
                        RESPONSE_FAIL.writeData(os);
                    }
                }
            } catch (final IOException e) {
                Log.w(TAG, e);
            } finally {
                IoUtils.closeQuietly(mSocket);
                mServer.notifyClientFinish(this);
            }
        }
    }
}
