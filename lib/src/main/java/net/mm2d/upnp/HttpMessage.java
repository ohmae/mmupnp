/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.Log;
import net.mm2d.util.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HTTPのメッセージを表現するクラスの親クラス。
 *
 * <p>ResponseとRequestでStart Lineのフォーマットが異なるため
 * その部分の実装はサブクラスに任せている。
 *
 * <p>UPnPの通信でよく利用される小さなデータのやり取りに特化したもので、
 * 長大なデータのやり取りは想定していない。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 * @see HttpResponse
 * @see HttpRequest
 */
public abstract class HttpMessage {
    private static final int BUFFER_SIZE = 1500;
    private static final int DEFAULT_CHUNK_SIZE = 1024;
    private static final int CR = 0x0d;
    private static final int LF = 0x0a;
    private static final byte[] CRLF = new byte[]{(byte) CR, (byte) LF};
    private static final String EOL = "\r\n";
    private static final String CHARSET = "utf-8";

    @Nullable
    private InetAddress mAddress;
    private int mPort;
    @Nonnull
    private final HttpHeader mHeaders;
    @Nonnull
    private String mVersion = Http.DEFAULT_HTTP_VERSION;
    @Nullable
    private byte[] mBodyBinary;
    @Nullable
    private String mBody;

    /**
     * インスタンス作成
     */
    public HttpMessage() {
        mHeaders = new HttpHeader();
    }

    /**
     * インスタンス作成
     *
     * @param socket 受信したsocket
     */
    public HttpMessage(@Nonnull final Socket socket) {
        this();
        mAddress = socket.getInetAddress();
        mPort = socket.getPort();
    }

    /**
     * 引数のインスタンスと同一の内容を持つインスタンスを作成する。
     *
     * @param original コピー元
     */
    public HttpMessage(@Nonnull final HttpMessage original) {
        mAddress = original.mAddress;
        mPort = original.mPort;
        mHeaders = new HttpHeader(original.mHeaders);
        mVersion = original.mVersion;
        if (original.mBodyBinary != null) {
            mBodyBinary = Arrays.copyOf(original.mBodyBinary, original.mBodyBinary.length);
        } else {
            mBodyBinary = null;
        }
        mBody = original.mBody;
    }

    /**
     * 宛先アドレス情報を返す。
     *
     * @return 宛先アドレス情報。
     */
    @Nullable
    public InetAddress getAddress() {
        return mAddress;
    }

    /**
     * 宛先アドレスを登録する。
     *
     * @param address 宛先アドレス。
     */
    protected void setAddress(@Nullable final InetAddress address) {
        mAddress = address;
    }

    /**
     * 宛先ポート番号を返す。
     *
     * @return 宛先ポート番号
     */
    public int getPort() {
        return mPort;
    }

    /**
     * 宛先ポート番号を設定する。
     *
     * @param port 宛先ポート番号
     */
    protected void setPort(final int port) {
        mPort = port;
    }

    /**
     * アドレスとポート番号の組み合わせ文字列を返す。
     *
     * @return アドレスとポート番号の組み合わせ文字列
     */
    @Nonnull
    public String getAddressString() throws IllegalStateException {
        if (mAddress == null) {
            throw new IllegalStateException("address must be set");
        }
        if (mPort == Http.DEFAULT_PORT || mPort <= 0) {
            return mAddress.getHostAddress();
        }
        return mAddress.getHostAddress() + ":" + String.valueOf(mPort);
    }

    /**
     * 宛先SocketAddressを返す
     *
     * @return 宛先SocketAddress
     */
    @Nonnull
    public SocketAddress getSocketAddress() throws IllegalStateException {
        if (mAddress == null) {
            throw new IllegalStateException("address must be set");
        }
        return new InetSocketAddress(mAddress, mPort);
    }

    /**
     * Start Lineを返す。
     *
     * @return Start Line
     */
    @Nullable
    public abstract String getStartLine();

    /**
     * Start Lineを設定する。
     *
     * @param line Start Line
     */
    public abstract void setStartLine(@Nonnull String line) throws IllegalArgumentException;

    /**
     * HTTPバージョンの値を返す。
     *
     * @return HTTPバージョン
     */
    @Nonnull
    public String getVersion() {
        return mVersion;
    }

    /**
     * HTTPバージョンを設定する。
     *
     * @param version HTTPバージョン
     */
    public void setVersion(@Nonnull final String version) {
        mVersion = version;
    }

    /**
     * ヘッダを設定する。
     *
     * @param name  ヘッダ名
     * @param value 値
     */
    public void setHeader(@Nonnull final String name, @Nonnull final String value) {
        mHeaders.put(name, value);
    }

    /**
     * ヘッダの各行からヘッダの設定を行う
     *
     * @param line ヘッダの1行
     */
    public void setHeaderLine(@Nonnull final String line) {
        final int pos = line.indexOf(':');
        if (pos < 0) {
            return;
        }
        final String name = line.substring(0, pos).trim();
        final String value = line.substring(pos + 1).trim();
        setHeader(name, value);
    }

    /**
     * ヘッダの値を返す。
     *
     * @param name ヘッダ名
     * @return ヘッダの値
     */
    @Nullable
    public String getHeader(@Nonnull final String name) {
        return mHeaders.get(name);
    }

    /**
     * ヘッダの値からチャンク伝送か否かを返す。
     *
     * @return チャンク伝送の場合true
     */
    public boolean isChunked() {
        return mHeaders.containsValue(Http.TRANSFER_ENCODING, Http.CHUNKED);
    }

    /**
     * ヘッダの値からKeepAliveか否かを返す。
     *
     * <p>HTTP/1.0の場合、Connection: keep-aliveの場合に、
     * HTTP/1.1の場合、Connection: closeでない場合に、
     * KeepAliveと判定し、trueを返す。
     *
     * @return KeepAliveの場合true
     */
    public boolean isKeepAlive() {
        if (mVersion.equals(Http.HTTP_1_0)) {
            return mHeaders.containsValue(Http.CONNECTION, Http.KEEP_ALIVE);
        }
        return !mHeaders.containsValue(Http.CONNECTION, Http.CLOSE);
    }

    /**
     * Content-Lengthの値を返す。
     *
     * <p>不明な場合0
     *
     * @return Content-Lengthの値
     */
    public int getContentLength() {
        final String len = mHeaders.get(Http.CONTENT_LENGTH);
        if (len != null) {
            try {
                return Integer.parseInt(len);
            } catch (final NumberFormatException e) {
                Log.w(e);
            }
        }
        return 0;
    }

    /**
     * メッセージボディを設定する。
     *
     * @param body メッセージボディ
     */
    public void setBody(@Nullable final String body) {
        setBody(body, false);
    }

    /**
     * メッセージボディを設定する。
     *
     * @param body              メッセージボディ
     * @param withContentLength trueを指定すると登録されたボディの値からContent-Lengthを合わせて登録する。
     */
    public void setBody(@Nullable final String body, final boolean withContentLength) {
        setBodyInner(body, null, withContentLength);
    }

    /**
     * メッセージボディを設定する。
     *
     * <p>取扱注意：メモリ節約のためバイナリデータは外部と共有させる。
     *
     * @param body メッセージボディ
     */
    public void setBodyBinary(@Nullable final byte[] body) {
        setBodyBinary(body, false);
    }

    /**
     * メッセージボディを設定する。
     *
     * @param body              メッセージボディ
     * @param withContentLength trueを指定すると登録されたボディの値からContent-Lengthを合わせて登録する。
     */
    public void setBodyBinary(@Nullable final byte[] body, final boolean withContentLength) {
        setBodyInner(null, body, withContentLength);
    }

    private void setBodyInner(@Nullable final String string,
                              @Nullable final byte[] binary,
                              final boolean withContentLength) {
        mBody = string;
        if (TextUtils.isEmpty(string)) {
            mBodyBinary = binary;
        } else {
            try {
                mBodyBinary = string.getBytes(CHARSET);
            } catch (final UnsupportedEncodingException e) {
                Log.w(e);
            }
        }
        if (withContentLength) {
            final int length = mBodyBinary == null ? 0 : mBodyBinary.length;
            setHeader(Http.CONTENT_LENGTH, String.valueOf(length));
        }
    }

    /**
     * メッセージボディを返す。
     *
     * @return メッセージボディ
     */
    @Nullable
    public String getBody() {
        if (mBody == null && mBodyBinary != null) {
            try {
                mBody = new String(mBodyBinary, CHARSET);
            } catch (final UnsupportedEncodingException e) {
                Log.w(e);
            }
        }
        return mBody;
    }

    /**
     * メッセージボディを返す。
     *
     * <p>取扱注意：メモリ節約のためバイナリデータは外部と共有させる。
     *
     * @return メッセージボディ
     */
    @Nullable
    public byte[] getBodyBinary() {
        return mBodyBinary;
    }

    @Override
    @Nonnull
    public String toString() {
        return getMessageString();
    }

    /**
     * ヘッダ部分を文字列として返す。
     *
     * @return ヘッダ文字列
     */
    @Nonnull
    public String getHeaderString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getStartLine());
        sb.append(EOL);
        for (final HttpHeader.Entry entry : mHeaders.entrySet()) {
            sb.append(entry.getName());
            sb.append(": ");
            sb.append(entry.getValue());
            sb.append(EOL);
        }
        sb.append(EOL);
        return sb.toString();
    }

    /**
     * ヘッダ部分をbyte配列として返す。
     *
     * @return ヘッダバイナリ
     */
    @Nonnull
    private byte[] getHeaderBytes() {
        try {
            return getHeaderString().getBytes(CHARSET);
        } catch (final UnsupportedEncodingException e) {
            Log.w(e);
        }
        return new byte[0];
    }

    /**
     * メッセージを文字列として返す。
     *
     * @return メッセージ文字列
     */
    @Nonnull
    public String getMessageString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getStartLine());
        sb.append(EOL);
        for (final HttpHeader.Entry entry : mHeaders.entrySet()) {
            sb.append(entry.getName());
            sb.append(": ");
            sb.append(entry.getValue());
            sb.append(EOL);
        }
        sb.append(EOL);
        final String body = getBody();
        if (!TextUtils.isEmpty(body)) {
            sb.append(body);
        }
        return sb.toString();
    }

    /**
     * 指定されたOutputStreamにメッセージの内容を書き出す。
     *
     * @param os 出力先
     * @throws IOException 入出力エラー
     */
    public void writeData(@Nonnull final OutputStream os) throws IOException {
        os.write(getHeaderBytes());
        if (mBodyBinary != null) {
            if (isChunked()) {
                writeChunkedBody(os, mBodyBinary);
            } else {
                os.write(mBodyBinary);
            }
        }
        os.flush();
    }

    private void writeChunkedBody(@Nonnull final OutputStream os, @Nonnull final byte[] binary)
            throws IOException {
        int offset = 0;
        while (offset < binary.length) {
            final int size = Math.min(DEFAULT_CHUNK_SIZE, binary.length - offset);
            writeChunkSize(os, size);
            os.write(binary, offset, size);
            os.write(CRLF);
            offset += size;
        }
        writeChunkSize(os, 0);
        os.write(CRLF);
    }

    private void writeChunkSize(@Nonnull final OutputStream os, final int size) throws IOException {
        os.write(Integer.toHexString(size).getBytes(CHARSET));
        os.write(CRLF);
    }

    /**
     * 指定されたInputStreamからデータの読み出しを行う。
     *
     * @param is 入力元
     * @throws IOException 入出力エラー
     */
    public void readData(@Nonnull final InputStream is) throws IOException {
        readStartLine(is);
        readHeaders(is);
        if (isChunked()) {
            readChunkedBody(is);
        } else {
            readBody(is);
        }
    }

    private void readStartLine(@Nonnull final InputStream is) throws IOException {
        final String startLine = readLine(is);
        if (TextUtils.isEmpty(startLine)) {
            throw new IOException("Illegal start line:" + startLine);
        }
        try {
            setStartLine(startLine);
        } catch (final IllegalArgumentException e) {
            throw new IOException("Illegal start line:" + startLine);
        }
    }

    private void readHeaders(@Nonnull final InputStream is) throws IOException {
        while (true) {
            final String line = readLine(is);
            if (line.isEmpty()) {
                break;
            }
            setHeaderLine(line);
        }
    }

    private void readBody(@Nonnull final InputStream is) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int length = getContentLength();
        final byte[] buffer = new byte[BUFFER_SIZE];
        while (length > 0) {
            final int stroke = length > buffer.length ? buffer.length : length;
            final int size = is.read(buffer, 0, stroke);
            if (size < 0) {
                throw new IOException("can't read from InputStream");
            }
            baos.write(buffer, 0, size);
            length -= size;
        }
        setBodyBinary(baos.toByteArray());
    }

    private void readChunkedBody(@Nonnull final InputStream is) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (true) {
            int length = readChunkSize(is);
            if (length == 0) {
                readLine(is);
                break;
            }
            final byte[] buffer = new byte[BUFFER_SIZE];
            while (length > 0) {
                final int stroke = length > buffer.length ? buffer.length : length;
                final int size = is.read(buffer, 0, stroke);
                if (size < 0) {
                    throw new IOException("can't read from InputStream");
                }
                baos.write(buffer, 0, size);
                length -= size;
            }
            readLine(is);
        }
        setBodyBinary(baos.toByteArray());
    }

    private int readChunkSize(@Nonnull final InputStream is) throws IOException {
        final String line = readLine(is);
        if (TextUtils.isEmpty(line)) {
            throw new IOException("Can not read chunk size!");
        }
        final String chunkSize = line.split(";", 2)[0];
        try {
            return Integer.parseInt(chunkSize, 16);
        } catch (final NumberFormatException e) {
            throw new IOException("Chunk format error!", e);
        }
    }

    @Nonnull
    private static String readLine(@Nonnull final InputStream is) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (true) {
            final int b = is.read();
            if (b < 0) {
                if (baos.size() == 0) {
                    throw new IOException("can't read from InputStream");
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
