/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.util.IoUtils;

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

public class HttpServerMock {
    public interface ServerCore {
        boolean receiveAndReply(
                @Nonnull Socket socket,
                @Nonnull InputStream is,
                @Nonnull OutputStream os)
                throws IOException;
    }

    @Nullable
    private ServerSocket mServerSocket;
    @Nullable
    private ServerTask mServerTask;
    @Nullable
    private ServerCore mServerCore;

    public HttpServerMock() {
    }

    public void setServerCore(@Nullable final ServerCore serverCore) {
        mServerCore = serverCore;
        if (mServerTask != null) {
            mServerTask.setServerCore(mServerCore);
        }
    }

    void open() throws IOException {
        mServerSocket = new ServerSocket(0);
        mServerTask = new ServerTask(mServerSocket);
        mServerTask.setServerCore(mServerCore);
        mServerTask.start();
    }

    int getLocalPort() {
        if (mServerSocket == null) {
            return 0;
        }
        return mServerSocket.getLocalPort();
    }

    void close() {
        if (mServerTask != null) {
            mServerTask.shutdownRequest();
            mServerTask = null;
        }
    }

    private static class ServerTask implements Runnable {
        private volatile boolean mShutdownRequest = false;
        @Nonnull
        private final ServerSocket mServerSocket;
        @Nonnull
        private final List<ClientTask> mClientList;
        @Nullable
        private Thread mThread;
        @Nullable
        private ServerCore mServerCore;

        ServerTask(@Nonnull final ServerSocket sock) {
            mServerSocket = sock;
            mClientList = Collections.synchronizedList(new LinkedList<>());
        }

        public void setServerCore(@Nullable final ServerCore serverCore) {
            mServerCore = serverCore;
        }

        void start() {
            mThread = new Thread(this, "HttpServerMock::ServerTask");
            mThread.start();
        }

        void shutdownRequest() {
            mShutdownRequest = true;
            if (mThread != null) {
                mThread.interrupt();
                mThread = null;
            }
            IoUtils.closeQuietly(mServerSocket);
            synchronized (mClientList) {
                for (final ClientTask client : mClientList) {
                    client.shutdownRequest();
                }
                mClientList.clear();
            }
        }

        void notifyClientFinished(@Nonnull final ClientTask client) {
            mClientList.remove(client);
        }

        @Override
        public void run() {
            try {
                while (!mShutdownRequest) {
                    final Socket sock = mServerSocket.accept();
                    sock.setSoTimeout(Property.DEFAULT_TIMEOUT);
                    final ClientTask client = new ClientTask(this, sock);
                    mClientList.add(client);
                    client.start();
                }
            } catch (final IOException ignored) {
            } finally {
                IoUtils.closeQuietly(mServerSocket);
            }
        }

        public boolean receiveAndReply(
                @Nonnull final Socket socket,
                @Nonnull final InputStream is,
                @Nonnull final OutputStream os)
                throws IOException {
            if (mServerCore != null) {
                return mServerCore.receiveAndReply(socket, is, os);
            }
            return false;
        }
    }

    private static class ClientTask implements Runnable {
        @Nonnull
        private final ServerTask mServer;
        @Nonnull
        private final Socket mSocket;
        @Nullable
        private Thread mThread;

        ClientTask(
                @Nonnull final ServerTask server,
                @Nonnull final Socket sock) {
            mServer = server;
            mSocket = sock;
        }

        void start() {
            mThread = new Thread(this, "HttpServerMock::ClientTask");
            mThread.start();
        }

        void shutdownRequest() {
            if (mThread != null) {
                mThread.interrupt();
                mThread = null;
            }
            IoUtils.closeQuietly(mSocket);
        }

        @Override
        public void run() {
            InputStream is = null;
            OutputStream os = null;
            try {
                is = mSocket.getInputStream();
                os = mSocket.getOutputStream();
                while (mServer.receiveAndReply(mSocket, is, os)) ;
            } catch (final IOException e) {
            } finally {
                IoUtils.closeQuietly(is);
                IoUtils.closeQuietly(os);
                IoUtils.closeQuietly(mSocket);
                mServer.notifyClientFinished(this);
            }
        }
    }
}
