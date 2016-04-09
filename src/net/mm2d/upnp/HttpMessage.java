/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public abstract class HttpMessage {
    private static final int BUFFER_SIZE = 1500;
    private static final int CR = 0x0d;
    private static final int LF = 0x0a;
    private static final String CRLF = "\r\n";
    private static final String CHARSET = "utf-8";

    private InetAddress mAddress;
    private int mPort;
    private final HttpHeader mHeaders;
    private String mVersion = Http.DEFAULT_HTTP_VERSION;
    private byte[] mBodyBin;
    private String mBody;

    public HttpMessage() {
        mHeaders = new HttpHeader();
    }

    public InetAddress getAddress() {
        return mAddress;
    }

    public void setAddress(InetAddress address) {
        mAddress = address;
    }

    public int getPort() {
        return mPort;
    }

    public void setPort(int port) {
        mPort = port;
    }

    public String getAddressString() {
        return mAddress.getHostAddress() + ":" + String.valueOf(mPort);
    }

    public SocketAddress getSocketAddress() {
        return new InetSocketAddress(mAddress, mPort);
    }

    public abstract String getStartLine();

    public abstract void setStartLine(String line);

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        mVersion = version;
    }

    public void setHeader(String name, String value) {
        mHeaders.put(name, value);
    }

    public void setHeaderLine(String line) {
        final int pos = line.indexOf(':');
        if (pos < 0) {
            return;
        }
        final String name = line.substring(0, pos).trim();
        final String value = line.substring(pos + 1).trim();
        setHeader(name, value);
    }

    public String getHeader(String name) {
        return mHeaders.get(name);
    }

    public boolean isChunked() {
        return mHeaders.containsValue(Http.TRANSFER_ENCODING, Http.CHUNKED);
    }

    public boolean isKeepAlive() {
        if (mVersion.equals(Http.HTTP_1_0)) {
            return mHeaders.containsValue(Http.CONNECTION, Http.KEEP_ALIVE);
        }
        return !mHeaders.containsValue(Http.CONNECTION, Http.CLOSE);
    }

    public int getContentLength() {
        final String len = mHeaders.get(Http.CONTENT_LENGTH);
        if (len != null) {
            try {
                return Integer.parseInt(len);
            } catch (final NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public void setBody(String body, boolean withContentLength) {
        setBody(body);
        if (withContentLength) {
            final int length = mBodyBin == null ? 0 : mBodyBin.length;
            setHeader(Http.CONTENT_LENGTH, String.valueOf(length));
        }
    }

    public void setBodyBin(byte[] body, boolean withContentLength) {
        setBodyBin(body);
        if (withContentLength) {
            final int length = mBodyBin == null ? 0 : mBodyBin.length;
            setHeader(Http.CONTENT_LENGTH, String.valueOf(length));
        }
    }

    public void setBody(String body) {
        mBody = body;
        if (body == null || body.isEmpty()) {
            mBodyBin = null;
        } else {
            try {
                mBodyBin = body.getBytes(CHARSET);
            } catch (final UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    public String getBody() {
        if (mBody == null && mBodyBin != null) {
            try {
                mBody = new String(mBodyBin, CHARSET);
            } catch (final UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return mBody;
    }

    public void setBodyBin(byte[] body) {
        mBodyBin = body;
        mBody = null;
    }

    public byte[] getBodyBin() {
        return mBodyBin;
    }

    @Override
    public String toString() {
        return getMessageString();
    }

    public String getHeaderString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getStartLine());
        sb.append(CRLF);
        for (final HttpHeader.Entry entry : mHeaders.entrySet()) {
            sb.append(entry.getName());
            sb.append(": ");
            sb.append(entry.getValue());
            sb.append(CRLF);
        }
        sb.append(CRLF);
        return sb.toString();
    }

    public String getMessageString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getStartLine());
        sb.append(CRLF);
        for (final HttpHeader.Entry entry : mHeaders.entrySet()) {
            sb.append(entry.getName());
            sb.append(": ");
            sb.append(entry.getValue());
            sb.append(CRLF);
        }
        sb.append(CRLF);
        final String body = getBody();
        if (body != null) {
            sb.append(body);
        }
        return sb.toString();
    }

    public void writeData(OutputStream os) throws IOException {
        os.write(getHeaderString().getBytes(CHARSET));
        if (mBodyBin != null) {
            os.write(mBodyBin);
        }
        os.flush();
    }

    public boolean readData(InputStream is) throws IOException {
        final String startLine = readLine(is);
        if (startLine == null || startLine.length() == 0) {
            return false;
        }
        setStartLine(startLine);
        while (true) {
            final String line = readLine(is);
            if (line == null) {
                return false;
            }
            if (line.isEmpty()) {
                break;
            }
            setHeaderLine(line);
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (isChunked()) {
            while (true) {
                int length = readChunkSize(is);
                if (length == 0) {
                    readLine(is);
                    break;
                }
                final byte[] buffer = new byte[BUFFER_SIZE];
                while (length > 0) {
                    int size = length > buffer.length ? buffer.length : length;
                    size = is.read(buffer, 0, size);
                    baos.write(buffer, 0, size);
                    length -= size;
                }
                readLine(is);
            }
        } else {
            int length = getContentLength();
            final byte[] buffer = new byte[BUFFER_SIZE];
            while (length > 0) {
                int size = length > buffer.length ? buffer.length : length;
                size = is.read(buffer, 0, size);
                baos.write(buffer, 0, size);
                length -= size;
            }
        }
        setBodyBin(baos.toByteArray());
        return true;
    }

    private int readChunkSize(InputStream is) throws IOException {
        final String line = readLine(is);
        if (line == null || line.isEmpty()) {
            throw new IOException("Can not read chunk size!");
        }
        final String chunkSize = line.split(";", 2)[0];
        try {
            return Integer.parseInt(chunkSize, 16);
        } catch (final NumberFormatException e) {
            throw new IOException("Chunk format error!");
        }
    }

    private static String readLine(InputStream is) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (true) {
            final int b = is.read();
            if (b < 0) {
                if (baos.size() == 0) {
                    return null;
                }
                break;
            }
            if (b == LF) {
                break;
            }
            if (b == CR) {
                continue;
            }
            baos.write(b);
        }
        return baos.toString(CHARSET);
    }
}
