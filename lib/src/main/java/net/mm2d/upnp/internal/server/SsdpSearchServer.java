/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server;

import net.mm2d.upnp.Http;
import net.mm2d.upnp.SsdpMessage;
import net.mm2d.upnp.internal.message.SsdpRequest;
import net.mm2d.upnp.internal.message.SsdpResponse;
import net.mm2d.util.TextUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * SSDP M-SEARCHとそのレスポンス受信を行うクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class SsdpSearchServer implements SsdpServer {
    /**
     * ST(SearchType) 全機器。
     */
    public static final String ST_ALL = "ssdp:all";
    /**
     * ST(SearchType) rootdevice。
     */
    public static final String ST_ROOTDEVICE = "upnp:rootdevice";

    /**
     * M-SEARCHによるレスポンス受信を受け取るリスナー。
     */
    public interface ResponseListener {
        /**
         * M-SEARCHレスポンス受信時にコール。
         *
         * @param message 受信したレスポンスメッセージ
         */
        void onReceiveResponse(@Nonnull SsdpMessage message);
    }

    @Nonnull
    private final SsdpServerDelegate mDelegate;
    @Nullable
    private ResponseListener mListener;

    /**
     * インスタンス作成。
     *
     * @param ni 使用するインターフェース
     */
    SsdpSearchServer(
            @Nonnull final Address address,
            @Nonnull final NetworkInterface ni) {
        mDelegate = new SsdpServerDelegate(this::onReceive, address, ni);
    }

    // VisibleForTesting
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

    @Nonnull
    private SsdpRequest makeSearchMessage(@Nonnull final String st) {
        final SsdpRequest message = new SsdpRequest();
        message.setMethod(SsdpMessage.M_SEARCH);
        message.setUri("*");
        message.setHeader(Http.HOST, mDelegate.getSsdpAddressString());
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER);
        message.setHeader(Http.MX, "1");
        message.setHeader(Http.ST, st);
        return message;
    }

    @Nonnull
    private InetAddress getLocalAddress() {
        return mDelegate.getLocalAddress();
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
    void onReceive(
            @Nonnull final InetAddress sourceAddress,
            @Nonnull final byte[] data,
            final int length) {
        try {
            final SsdpResponse message = new SsdpResponse(getLocalAddress(), data, length);
            if (mDelegate.isInvalidLocation(message, sourceAddress)) {
                return;
            }
            if (mListener != null) {
                mListener.onReceiveResponse(message);
            }
        } catch (final IOException ignored) {
        }
    }
}
