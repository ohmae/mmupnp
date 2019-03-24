/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server;

import net.mm2d.log.Logger;
import net.mm2d.upnp.Http;
import net.mm2d.upnp.HttpRequest;
import net.mm2d.upnp.HttpResponse;
import net.mm2d.upnp.Property;
import net.mm2d.upnp.internal.thread.TaskExecutors;
import net.mm2d.upnp.internal.util.IoUtils;
import net.mm2d.upnp.internal.util.StringPair;
import net.mm2d.upnp.util.TextParseUtils;
import net.mm2d.upnp.util.TextUtils;
import net.mm2d.upnp.util.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.FutureTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

/**
 * イベント購読によって通知されるEventを受信するクラス。
 *
 * <p>HTTPのサーバとしてリクエストの受付のみを行う。
 * HTTPメッセージのパースはリスナーの実装側が行う。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class EventReceiver implements Runnable {
    /**
     * イベントデータの受信を受け取るリスナー。
     */
    public interface EventMessageListener {
        /**
         * イベント受信時にコール。
         *
         * @param sid        Subscribe ID
         * @param seq        SEQヘッダの値
         * @param properties プロパティ
         * @return HTTPメッセージが正常であればtrue
         */
        boolean onEventReceived(
                @Nonnull String sid,
                long seq,
                @Nonnull List<StringPair> properties);
    }

    private static final long PREPARE_TIMEOUT = 1000L;

    @Nullable
    private ServerSocket mServerSocket;
    @Nonnull
    private final TaskExecutors mTaskExecutors;
    @Nullable
    private final EventMessageListener mListener;
    @Nonnull
    private final List<ClientTask> mClientList = Collections.synchronizedList(new LinkedList<>());
    @Nullable
    private FutureTask<?> mFutureTask;

    private boolean mReady;

    /**
     * インスタンス作成。
     *
     * @param executors スレッドのExecutorを与える
     * @param listener  イベントを通知するリスナー
     */
    public EventReceiver(
            @Nonnull final TaskExecutors executors,
            @Nullable final EventMessageListener listener) {
        mTaskExecutors = executors;
        mListener = listener;
    }

    /**
     * サーバソケットのオープンと受信スレッドの開始を行う。
     */
    @SuppressWarnings("Duplicates")
    public void start() {
        if (mFutureTask != null) {
            stop();
        }
        synchronized (this) {
            mReady = false;
        }
        final FutureTask<?> task = new FutureTask<>(this, null);
        mFutureTask = task;
        mTaskExecutors.server(mFutureTask);
    }

    /**
     * 受信スレッドを終了させる。
     */
    @SuppressWarnings("Duplicates")
    public void stop() {
        final FutureTask<?> task = mFutureTask;
        if (task == null) {
            return;
        }
        task.cancel(false);
        mFutureTask = null;
        IoUtils.closeQuietly(mServerSocket);
        synchronized (mClientList) {
            for (final ClientTask client : mClientList) {
                client.stop();
            }
            mClientList.clear();
        }
    }

    // VisibleForTesting
    @Nonnull
    ServerSocket createServerSocket() throws IOException {
        return new ServerSocket(0);
    }

    /**
     * サーバーソケットに割り当てられたポート番号を返す。
     *
     * @return サーバソケットのポート番号
     */
    public int getLocalPort() {
        if (waitReady()) {
            final ServerSocket socket = mServerSocket;
            if (socket != null) {
                return socket.getLocalPort();
            }
        }
        return 0;
    }

    @SuppressWarnings("Duplicates")
    private synchronized boolean waitReady() {
        final FutureTask<?> task = mFutureTask;
        if (task == null || task.isDone()) {
            return false;
        }
        if (!mReady) {
            try {
                wait(PREPARE_TIMEOUT);
            } catch (final InterruptedException ignored) {
            }
        }
        return mReady;
    }

    private synchronized void notifyReady() {
        mReady = true;
        notifyAll();
    }

    private boolean isCancelled() {
        final FutureTask<?> task = mFutureTask;
        return task == null || task.isCancelled();
    }

    @Override
    public void run() {
        final Thread thread = Thread.currentThread();
        thread.setName(thread.getName() + "-event-receiver");
        try {
            mServerSocket = createServerSocket();
            notifyReady();
            while (!isCancelled()) {
                final Socket sock = mServerSocket.accept();
                sock.setSoTimeout(Property.DEFAULT_TIMEOUT);
                final ClientTask client = new ClientTask(this, sock);
                mClientList.add(client);
                client.start(mTaskExecutors);
            }
        } catch (final IOException ignored) {
        } finally {
            IoUtils.closeQuietly(mServerSocket);
        }
    }

    /**
     * Clientスレッドからの終了通知
     *
     * @param client 終了したClientスレッド
     */
    void notifyClientFinished(@Nonnull final ClientTask client) {
        mClientList.remove(client);
    }

    /**
     * イベントリスナーのコール
     *
     * @param sid     Subscribe ID
     * @param request 受信したHTTPメッセージ
     * @return HTTPメッセージが正常であればtrue
     */
    // VisibleForTesting
    synchronized boolean notifyEvent(
            @Nonnull final String sid,
            @Nonnull final HttpRequest request) {
        if (mListener == null) {
            return false;
        }
        final List<StringPair> list = parsePropertyPairs(request);
        if (list.isEmpty()) {
            return false;
        }
        final long seq = TextParseUtils.parseLongSafely(request.getHeader(Http.SEQ), 0);
        return mListener.onEventReceived(sid, seq, list);
    }

    // VisibleForTesting
    @Nonnull
    static List<StringPair> parsePropertyPairs(@Nonnull final HttpRequest request) {
        final String xml = request.getBody();
        if (TextUtils.isEmpty(xml)) {
            return Collections.emptyList();
        }
        try {
            final Document doc = XmlUtils.newDocument(true, xml);
            final Node propertySetNode = doc.getDocumentElement();
            if (!propertySetNode.getLocalName().equals("propertyset")) {
                return Collections.emptyList();
            }
            final List<StringPair> list = new ArrayList<>();
            Node propertyNode = propertySetNode.getFirstChild();
            for (; propertyNode != null; propertyNode = propertyNode.getNextSibling()) {
                if (!TextUtils.equals(getTagName(propertyNode), "property")) {
                    continue;
                }
                Node node = propertyNode.getFirstChild();
                for (; node != null; node = node.getNextSibling()) {
                    final String name = getTagName(node);
                    if (TextUtils.isEmpty(name)) {
                        continue;
                    }
                    final String value = node.getTextContent();
                    list.add(new StringPair(name, value));
                }
            }
            return list;
        } catch (final IOException | SAXException | ParserConfigurationException ignored) {
        }
        return Collections.emptyList();
    }

    @Nullable
    private static String getTagName(@Nonnull final Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }
        return node.getLocalName();
    }

    // VisibleForTesting
    static class ClientTask implements Runnable {
        private static final HttpResponse RESPONSE_OK;
        private static final HttpResponse RESPONSE_BAD;
        private static final HttpResponse RESPONSE_FAIL;

        static {
            RESPONSE_OK = HttpResponse.create();
            RESPONSE_OK.setStatus(Http.Status.HTTP_OK);
            RESPONSE_OK.setHeader(Http.SERVER, Property.SERVER_VALUE);
            RESPONSE_OK.setHeader(Http.CONNECTION, Http.CLOSE);
            RESPONSE_OK.setHeader(Http.CONTENT_LENGTH, "0");
            RESPONSE_BAD = HttpResponse.create();
            RESPONSE_BAD.setStatus(Http.Status.HTTP_BAD_REQUEST);
            RESPONSE_BAD.setHeader(Http.SERVER, Property.SERVER_VALUE);
            RESPONSE_BAD.setHeader(Http.CONNECTION, Http.CLOSE);
            RESPONSE_BAD.setHeader(Http.CONTENT_LENGTH, "0");
            RESPONSE_FAIL = HttpResponse.create();
            RESPONSE_FAIL.setStatus(Http.Status.HTTP_PRECON_FAILED);
            RESPONSE_FAIL.setHeader(Http.SERVER, Property.SERVER_VALUE);
            RESPONSE_FAIL.setHeader(Http.CONNECTION, Http.CLOSE);
            RESPONSE_FAIL.setHeader(Http.CONTENT_LENGTH, "0");
        }

        @Nonnull
        private final EventReceiver mEventReceiver;
        @Nonnull
        private final Socket mSocket;
        @Nullable
        private FutureTask<?> mFutureTask;

        /**
         * インスタンス作成
         *
         * @param eventReceiver サーバスレッド
         * @param sock          クライアントソケット
         */
        ClientTask(
                @Nonnull final EventReceiver eventReceiver,
                @Nonnull final Socket sock) {
            mEventReceiver = eventReceiver;
            mSocket = sock;
        }

        /**
         * スレッドを作成し開始する。
         */
        void start(@Nonnull final TaskExecutors taskExecutors) {
            final FutureTask<?> task = new FutureTask<>(this, null);
            mFutureTask = task;
            taskExecutors.io(task);
        }

        /**
         * スレッドを終了させ、ソケットのクローズを行う。
         */
        @SuppressWarnings("Duplicates")
        void stop() {
            final FutureTask<?> task = mFutureTask;
            if (task == null) {
                return;
            }
            task.cancel(false);
            mFutureTask = null;
            IoUtils.closeQuietly(mSocket);
        }

        private boolean notifyEvent(
                @Nonnull final String sid,
                @Nonnull final HttpRequest request) {
            return mEventReceiver.notifyEvent(sid, request);
        }

        @Override
        public void run() {
            InputStream is = null;
            OutputStream os = null;
            try {
                is = mSocket.getInputStream();
                os = mSocket.getOutputStream();
                receiveAndReply(is, os);
            } catch (final IOException e) {
                Logger.w(e);
            } finally {
                IoUtils.closeQuietly(is);
                IoUtils.closeQuietly(os);
                IoUtils.closeQuietly(mSocket);
                mEventReceiver.notifyClientFinished(this);
            }
        }

        // VisibleForTesting
        void receiveAndReply(
                @Nonnull final InputStream is,
                @Nonnull final OutputStream os)
                throws IOException {
            final HttpRequest request = HttpRequest.create();
            request.readData(is);
            Logger.v(() -> "receive event:\n" + request);
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
                if (notifyEvent(sid, request)) {
                    RESPONSE_OK.writeData(os);
                } else {
                    RESPONSE_FAIL.writeData(os);
                }
            }
        }
    }
}
