/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.TextUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * SSDP M-SEARCHとそのレスポンス受信を行うクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class SsdpSearchServer extends SsdpServer {
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
        void onReceiveResponse(@Nonnull SsdpResponseMessage message);
    }

    private ResponseListener mListener;

    /**
     * インスタンス作成。
     *
     * @param ni 使用するインターフェース
     */
    public SsdpSearchServer(@Nonnull NetworkInterface ni) {
        super(ni);
    }

    /**
     * レスポンス受信リスナーを登録する。
     *
     * @param listener リスナー
     */
    public void setResponseListener(@Nullable ResponseListener listener) {
        mListener = listener;
    }

    /**
     * M-SEARCHを実行する。
     *
     * <p>STはssdp:allで実行する。
     */
    public void search() {
        search(null);
    }

    /**
     * M-SEARCHを実行する。
     *
     * @param st STの値
     */
    public void search(@Nullable String st) {
        if (TextUtils.isEmpty(st)) {
            st = ST_ALL;
        }
        final SsdpRequestMessage message = new SsdpRequestMessage();
        message.setMethod(SsdpMessage.M_SEARCH);
        message.setUri("*");
        message.setHeader(Http.HOST, SSDP_ADDR + ":" + String.valueOf(SSDP_PORT));
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER);
        message.setHeader(Http.MX, "1");
        message.setHeader(Http.ST, st);
        send(message);
    }

    @Override
    protected void onReceive(@Nonnull InterfaceAddress addr, @Nonnull DatagramPacket dp) {
        try {
            final SsdpResponseMessage message = new SsdpResponseMessage(addr, dp);
            if (!message.hasValidLocation()) {
                return;
            }
            if (mListener != null) {
                mListener.onReceiveResponse(message);
            }
        } catch (final IOException ignored) {
        }
    }
}
