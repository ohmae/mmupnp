/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class HttpClient {
    private Socket mSocket;
    private boolean mKeepAlive;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

    public HttpClient() {
    }

    public HttpClient(boolean keepAlive) {
        setKeepAlive(keepAlive);
    }

    public boolean isKeepAlive() {
        return mKeepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        mKeepAlive = keepAlive;
    }

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

    private void openSocket(HttpRequest request) throws IOException {
        mSocket = new Socket();
        mSocket.connect(request.getSocketAddress(), Property.DEFAULT_TIMEOUT);
        mSocket.setSoTimeout(Property.DEFAULT_TIMEOUT);
        mInputStream = new BufferedInputStream(mSocket.getInputStream());
        mOutputStream = new BufferedOutputStream(mSocket.getOutputStream());
    }

    private void closeSocket() {
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (final IOException ignored) {
            }
            mInputStream = null;
        }
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (final IOException ignored) {
            }
            mOutputStream = null;
        }
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (final IOException ignored) {
            }
            mSocket = null;
        }
    }

    public void close() {
        closeSocket();
    }
}
