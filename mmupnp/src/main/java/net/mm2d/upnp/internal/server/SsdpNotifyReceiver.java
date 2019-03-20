/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server;

import net.mm2d.log.Logger;
import net.mm2d.upnp.SsdpMessage;
import net.mm2d.upnp.internal.message.SsdpRequest;
import net.mm2d.upnp.internal.thread.TaskExecutors;
import net.mm2d.upnp.util.TextUtils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * SSDP NOTIFYを受信するクラス
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class SsdpNotifyReceiver implements SsdpServer {
    /**
     * NOTIFY受信を受け取るリスナー。
     */
    public interface NotifyListener {
        /**
         * NOTIFY受信時にコール。
         *
         * @param message 受信したNOTIFYメッセージ
         */
        void onReceiveNotify(@Nonnull SsdpMessage message);
    }

    @Nonnull
    private final SsdpServerDelegate mDelegate;
    @Nullable
    private NotifyListener mListener;
    private boolean mSegmentCheckEnabled;

    /**
     * インスタンス作成。
     *
     * @param ni 使用するインターフェース
     */
    SsdpNotifyReceiver(
            @Nonnull final TaskExecutors executors,
            @Nonnull final Address address,
            @Nonnull final NetworkInterface ni) {
        mDelegate = new SsdpServerDelegate(executors, this::onReceive, address, ni, SSDP_PORT);
    }

    // VisibleForTesting
    SsdpNotifyReceiver(@Nonnull final SsdpServerDelegate delegate) {
        mDelegate = delegate;
    }

    void setSegmentCheckEnabled(final boolean enabled) {
        mSegmentCheckEnabled = enabled;
    }

    /**
     * NOTIFY受信リスナーを登録する。
     *
     * @param listener リスナー
     */
    void setNotifyListener(@Nullable final NotifyListener listener) {
        mListener = listener;
    }

    // VisibleForTesting
    @Nonnull
    InterfaceAddress getInterfaceAddress() {
        return mDelegate.getInterfaceAddress();
    }

    @Nonnull
    private InetAddress getLocalAddress() {
        return mDelegate.getLocalAddress();
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
        if (invalidAddress(sourceAddress)) {
            return;
        }
        try {
            final SsdpRequest message = createSsdpRequestMessage(data, length);
            // M-SEARCHパケットは無視する
            if (TextUtils.equals(message.getMethod(), SsdpMessage.M_SEARCH)) {
                return;
            }
            Logger.v(() -> "receive ssdp notify from " + sourceAddress +
                    " in " + mDelegate.getLocalAddress() + ":\n" + message);
            // ByeByeは通信を行わないためアドレスの問題有無にかかわらず受け入れる
            if (!TextUtils.equals(message.getNts(), SsdpMessage.SSDP_BYEBYE)
                    && mDelegate.isInvalidLocation(message, sourceAddress)) {
                return;
            }
            if (mListener != null) {
                mListener.onReceiveNotify(message);
            }
        } catch (final IOException ignored) {
        }
    }

    // VisibleForTesting
    @Nonnull
    SsdpRequest createSsdpRequestMessage(
            @Nonnull final byte[] data,
            final int length)
            throws IOException {
        return SsdpRequest.create(getLocalAddress(), data, length);
    }

    // VisibleForTesting
    boolean invalidAddress(@Nonnull final InetAddress sourceAddress) {
        if (invalidVersion(sourceAddress)) {
            Logger.w(() -> "IP version mismatch:" + sourceAddress + " " + getInterfaceAddress());
            return true;
        }
        // アドレス設定が間違っている場合でもマルチキャストパケットの送信はできてしまう。
        // セグメント情報が間違っており、マルチキャスト以外のやり取りができない相手からのパケットは
        // 受け取っても無駄なので破棄する。
        if (mSegmentCheckEnabled
                && mDelegate.getAddress() == Address.IP_V4
                && invalidSegment(getInterfaceAddress(), sourceAddress)) {
            Logger.w(() -> "Invalid segment:" + sourceAddress + " " + getInterfaceAddress());
            return true;
        }
        return false;
    }

    private boolean invalidVersion(@Nonnull final InetAddress sourceAddress) {
        if (mDelegate.getAddress() == Address.IP_V4) {
            return sourceAddress instanceof Inet6Address;
        }
        return sourceAddress instanceof Inet4Address;
    }

    private boolean invalidSegment(
            @Nonnull final InterfaceAddress interfaceAddress,
            @Nonnull final InetAddress sourceAddress) {
        final byte[] a = interfaceAddress.getAddress().getAddress();
        final byte[] b = sourceAddress.getAddress();
        final int pref = interfaceAddress.getNetworkPrefixLength();
        final int bytes = pref / 8;
        final int bits = pref % 8;
        for (int i = 0; i < bytes; i++) {
            if (a[i] != b[i]) {
                return true;
            }
        }
        if (bits != 0) {
            final byte mask = (byte) (0xff << (8 - bits));
            return (a[bytes] & mask) != (b[bytes] & mask);
        }
        return false;
    }
}
