/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.SsdpServerDelegate.Receiver;
import net.mm2d.util.TextUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * SSDP M-SEARCHとそのレスポンス受信を行うクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class SsdpSearchServer implements SsdpServer, Receiver {
    /**
     * ST(SearchType) 全機器。
     */
    static final String ST_ALL = "ssdp:all";
    /**
     * ST(SearchType) rootdevice。
     */
    static final String ST_ROOTDEVICE = "upnp:rootdevice";

    /**
     * M-SEARCHによるレスポンス受信を受け取るリスナー。
     */
    public interface ResponseListener {
        /**
         * M-SEARCHレスポンス受信時にコール。
         *
         * @param message 受信したレスポンスメッセージ
         */
        void onReceiveResponse(@Nonnull SsdpResponseMessage message);
    }

    @Nonnull
    private final SsdpServer mDelegate;
    @Nullable
    private ResponseListener mListener;

    /**
     * インスタンス作成。
     *
     * @param ni 使用するインターフェース
     */
    SsdpSearchServer(@Nonnull final NetworkInterface ni) {
        mDelegate = new SsdpServerDelegate(this, ni);
    }

    SsdpSearchServer(@Nonnull final SsdpServerDelegate delegate) {
        mDelegate = delegate;
    }

    /**
     * レスポンス受信リスナーを登録する。
     *
     * @param listener リスナー
     */
    void setResponseListener(@Nullable final ResponseListener listener) {
        mListener = listener;
    }

    /**
     * M-SEARCHを実行する。
     *
     * <p>STはssdp:allで実行する。
     */
    void search() {
        search(null);
    }

    /**
     * M-SEARCHを実行する。
     *
     * @param st STの値
     */
    void search(@Nullable final String st) {
        send(makeSearchMessage(TextUtils.isEmpty(st) ? ST_ALL : st));
    }

    private SsdpRequestMessage makeSearchMessage(@Nonnull final String st) {
        final SsdpRequestMessage message = new SsdpRequestMessage();
        message.setMethod(SsdpMessage.M_SEARCH);
        message.setUri("*");
        message.setHeader(Http.HOST, SSDP_ADDR + ":" + String.valueOf(SSDP_PORT));
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER);
        message.setHeader(Http.MX, "1");
        message.setHeader(Http.ST, st);
        return message;
    }

    @Nonnull
    @Override
    public InterfaceAddress getInterfaceAddress() {
        return mDelegate.getInterfaceAddress();
    }

    @Override
    public void open() throws IOException {
        mDelegate.open();
    }

    @Override
    public void close() {
        mDelegate.close();
    }

    @Override
    public void start() {
        mDelegate.start();
    }

    @Override
    public void stop() {
        mDelegate.stop();
    }

    @Override
    public void send(@Nonnull final SsdpMessage message) {
        mDelegate.send(message);
    }

    // VisibleForTesting
    @Override
    public void onReceive(
            @Nonnull final InetAddress sourceAddress,
            @Nonnull final byte[] data,
            final int length) {
        try {
            final SsdpResponseMessage message = new SsdpResponseMessage(getInterfaceAddress(), data, length);
            if (message.hasInvalidLocation(sourceAddress)) {
                return;
            }
            if (mListener != null) {
                mListener.onReceiveResponse(message);
            }
        } catch (final IOException ignored) {
        }
    }
}
