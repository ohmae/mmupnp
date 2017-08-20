/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.Log;
import net.mm2d.util.TextUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * SSDP NOTIFYを受信するクラス
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class SsdpNotifyReceiver extends SsdpServer {
    /**
     * NOTIFY受信を受け取るリスナー。
     */
    interface NotifyListener {
        /**
         * NOTIFY受信時にコール。
         *
         * @param message 受信したNOTIFYメッセージ
         */
        void onReceiveNotify(@Nonnull SsdpRequestMessage message);
    }

    @Nullable
    private NotifyListener mListener;

    /**
     * インスタンス作成。
     *
     * @param ni 使用するインターフェース
     */
    SsdpNotifyReceiver(@Nonnull final NetworkInterface ni) {
        super(ni, SSDP_PORT);
    }

    /**
     * NOTIFY受信リスナーを登録する。
     *
     * @param listener リスナー
     */
    void setNotifyListener(@Nullable final NotifyListener listener) {
        mListener = listener;
    }

    @Override
    protected void onReceive(
            @Nonnull final InetAddress sourceAddress,
            @Nonnull final byte[] data,
            final int length) {
        // アドレス設定が間違っている場合でもマルチキャストパケットの送信はできてしまう。
        // セグメント情報が間違っており、マルチキャスト以外のやり取りができない相手からのパケットは
        // 受け取っても無駄なので破棄する。
        if (!isSameSegment(getInterfaceAddress(), sourceAddress)) {
            Log.w("Invalid segment packet received:" + sourceAddress.toString()
                    + " " + getInterfaceAddress().toString());
            return;
        }
        try {
            final SsdpRequestMessage message = new SsdpRequestMessage(getInterfaceAddress(), data, length);
            // M-SEARCHパケットは無視する
            if (TextUtils.equals(message.getMethod(), SsdpMessage.M_SEARCH)) {
                return;
            }
            // ByeByeは通信を行わないためアドレスの問題有無にかかわらず受け入れる
            if (!TextUtils.equals(message.getNts(), SsdpMessage.SSDP_BYEBYE)
                    && message.hasInvalidLocation(sourceAddress)) {
                return;
            }
            if (mListener != null) {
                mListener.onReceiveNotify(message);
            }
        } catch (final IOException ignored) {
        }
    }

    private static boolean isSameSegment(
            @Nonnull final InterfaceAddress interfaceAddress,
            @Nonnull final InetAddress sourceAddress) {
        final byte[] a = interfaceAddress.getAddress().getAddress();
        final byte[] b = sourceAddress.getAddress();
        final int pref = interfaceAddress.getNetworkPrefixLength();
        final int bytes = pref / 8;
        final int bits = pref % 8;
        for (int i = 0; i < bytes; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        if (bits != 0) {
            final byte mask = (byte) (0xff << (8 - bits));
            if ((a[bytes] & mask) != (b[bytes] & mask)) {
                return false;
            }
        }
        return true;
    }
}
