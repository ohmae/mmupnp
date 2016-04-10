/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

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

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class EventReceiver {
    private ServerSocket mServerSocket;
    private ServerThread mServerThread;
    private EventPacketListener mListener;

    public interface EventPacketListener {
        boolean onEventReceived(HttpRequest request);
    }

    private static class ServerThread extends Thread {
        private volatile boolean mShutdownRequest = false;
        private final ServerSocket mServerSocket;
        private final List<ClientThread> mClientList;
        private EventPacketListener mListener;

        public ServerThread(ServerSocket sock) {
            mServerSocket = sock;
            mClientList = Collections.synchronizedList(new LinkedList<>());
        }

        public void shutdownRequest() {
            mShutdownRequest = true;
            interrupt();
            try {
                mServerSocket.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
            synchronized (mClientList) {
                for (final ClientThread client : mClientList) {
                    client.shutdownRequest();
                }
                mClientList.clear();
            }
        }

        public void notifyClientFinish(ClientThread client) {
            mClientList.remove(client);
        }

        public void setEventPacketListener(EventPacketListener listener) {
            mListener = listener;
        }

        private boolean notifyEvent(HttpRequest request) {
            if (mListener != null) {
                return mListener.onEventReceived(request);
            }
            return false;
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
            } catch (final IOException e) {
            } finally {
                try {
                    mServerSocket.close();
                } catch (final IOException e) {
                }
            }
        }
    }

    private static class ClientThread extends Thread {
        private volatile boolean mShutdownRequest = false;
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

        public ClientThread(ServerThread server, Socket sock) {
            mServer = server;
            mSocket = sock;
        }

        public void shutdownRequest() {
            mShutdownRequest = true;
            interrupt();
            try {
                mSocket.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        private boolean notifyEvent(HttpRequest request) {
            return mServer.notifyEvent(request);
        }

        @Override
        public void run() {
            InputStream is = null;
            OutputStream os = null;
            try {
                is = new BufferedInputStream(mSocket.getInputStream());
                os = new BufferedOutputStream(mSocket.getOutputStream());
                while (!mShutdownRequest) {
                    final HttpRequest request = new HttpRequest();
                    request.setAddress(mSocket.getInetAddress());
                    request.setPort(mSocket.getPort());
                    if (!request.readData(is)) {
                        break;
                    }
                    final String nt = request.getHeader(Http.NT);
                    final String nts = request.getHeader(Http.NTS);
                    final String sid = request.getHeader(Http.SID);
                    if (nt == null || nt.length() == 0
                            || nts == null || nts.length() == 0) {
                        RESPONSE_BAD.writeData(os);
                    } else if (sid == null || sid.length() == 0
                            || !nt.equals("upnp:event")
                            || !nts.equals("upnp:propchange")) {
                        RESPONSE_FAIL.writeData(os);
                    } else {
                        if (notifyEvent(request)) {
                            RESPONSE_OK.writeData(os);
                        } else {
                            RESPONSE_FAIL.writeData(os);
                        }
                    }
                    return;
                }
            } catch (final IOException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (final IOException e) {
                    }
                }
                if (os != null) {
                    try {
                        os.close();
                    } catch (final IOException e) {
                    }
                }
                try {
                    mSocket.close();
                } catch (final IOException e) {
                }
                mServer.notifyClientFinish(this);
            }
        }
    }

    public EventReceiver() {
    }

    public void setEventPacketListener(EventPacketListener listener) {
        mListener = listener;
        if (mServerThread != null) {
            mServerThread.setEventPacketListener(listener);
        }
    }

    public void open() throws IOException {
        mServerSocket = new ServerSocket(0);
        mServerThread = new ServerThread(mServerSocket);
        mServerThread.setEventPacketListener(mListener);
        mServerThread.start();
    }

    public int getLocalPort() {
        return mServerSocket.getLocalPort();
    }

    public void close() {
        mServerThread.shutdownRequest();
        mServerThread = null;
    }
}
