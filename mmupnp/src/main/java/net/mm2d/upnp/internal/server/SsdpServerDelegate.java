/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server;

import net.mm2d.log.Logger;
import net.mm2d.upnp.Http;
import net.mm2d.upnp.SsdpMessage;
import net.mm2d.upnp.internal.thread.TaskExecutors;
import net.mm2d.upnp.util.IoUtils;
import net.mm2d.upnp.util.NetworkUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.FutureTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * SsdpServerの共通処理を実装するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
// TODO: SocketChannelを使用した受信(MulticastChannelはAndroid N以降のため保留)
class SsdpServerDelegate implements SsdpServer, Runnable {
    interface Receiver {
        /**
         * メッセージ受信後の処理、サブクラスにより実装する。
         *
         * @param sourceAddress 送信元アドレス
         * @param data          受信したデータ
         * @param length        受信したデータの長さ
         */
        void onReceive(
                @Nonnull InetAddress sourceAddress,
                @Nonnull byte[] data,
                int length);
    }

    @Nonnull
    private final TaskExecutors mTaskExecutors;
    @Nonnull
    private final InterfaceAddress mInterfaceAddress;
    @Nonnull
    private final Receiver mReceiver;
    @Nonnull
    private final Address mAddress;
    @Nonnull
    private final NetworkInterface mInterface;

    private final int mBindPort;
    @Nullable
    private FutureTask<?> mFutureTask;

    private boolean mReady;
    @Nullable
    private MulticastSocket mSocket;

    /**
     * 使用するインターフェースを指定してインスタンス作成。
     *
     * <p>使用するポートは自動割当となる。
     *
     * @param receiver         パケット受信時にコールされるreceiver
     * @param networkInterface 使用するインターフェース
     * @param address          モード
     */
    SsdpServerDelegate(
            @Nonnull final TaskExecutors executors,
            @Nonnull final Receiver receiver,
            @Nonnull final Address address,
            @Nonnull final NetworkInterface networkInterface) {
        this(executors, receiver, address, networkInterface, 0);
    }

    /**
     * 使用するインターフェースとポート指定してインスタンス作成。
     *
     * @param receiver         パケット受信時にコールされるreceiver
     * @param networkInterface 使用するインターフェース
     * @param bindPort         使用するポート
     * @param address          モード
     */
    SsdpServerDelegate(
            @Nonnull final TaskExecutors executors,
            @Nonnull final Receiver receiver,
            @Nonnull final Address address,
            @Nonnull final NetworkInterface networkInterface,
            final int bindPort) {
        mInterfaceAddress = address == Address.IP_V4 ?
                findInet4Address(networkInterface.getInterfaceAddresses()) :
                findInet6Address(networkInterface.getInterfaceAddresses());
        mTaskExecutors = executors;
        mReceiver = receiver;
        mAddress = address;
        mInterface = networkInterface;
        mBindPort = bindPort;
    }

    /**
     * SsdpMessageのLocationに正常なURLが記述されており、
     * 記述のアドレスとパケットの送信元アドレスに不一致がないか検査する。
     *
     * @param message       確認するSsdpMessage
     * @param sourceAddress 送信元アドレス
     * @return true:送信元との不一致を含めてLocationに不正がある場合。false:それ以外
     */
    public static boolean isInvalidLocation(
            @Nonnull final SsdpMessage message,
            @Nonnull final InetAddress sourceAddress) {
        return !isValidLocation(message, sourceAddress);
    }

    private static boolean isValidLocation(
            @Nonnull final SsdpMessage message,
            @Nonnull final InetAddress sourceAddress) {
        final String location = message.getLocation();
        if (!Http.isHttpUrl(location)) {
            return false;
        }
        try {
            final InetAddress locationAddress = InetAddress.getByName(new URL(location).getHost());
            return sourceAddress.equals(locationAddress);
        } catch (final MalformedURLException | UnknownHostException ignored) {
        }
        return false;
    }

    /**
     * マルチキャストアドレスを返す。
     *
     * @return マルチキャストアドレス
     */
    @Nonnull
    Address getAddress() {
        return mAddress;
    }

    /**
     * SSDPに使用するアドレス。
     *
     * @return SSDPで使用するInetAddress
     */
    @Nonnull
    InetAddress getSsdpInetAddress() {
        return mAddress.getInetAddress();
    }

    /**
     * SSDPに使用するアドレス＋ポートの文字列。
     *
     * @return SSDPに使用するアドレス＋ポートの文字列。
     */
    @Nonnull
    String getSsdpAddressString() {
        return mAddress.getAddressString();
    }

    // VisibleForTesting
    @Nonnull
    static InterfaceAddress findInet4Address(@Nonnull final List<InterfaceAddress> addressList) {
        for (final InterfaceAddress address : addressList) {
            if (address.getAddress() instanceof Inet4Address) {
                return address;
            }
        }
        throw new IllegalArgumentException("ni does not have IPv4 address.");
    }

    // VisibleForTesting
    @Nonnull
    static InterfaceAddress findInet6Address(@Nonnull final List<InterfaceAddress> addressList) {
        for (final InterfaceAddress address : addressList) {
            final InetAddress inetAddress = address.getAddress();
            if (inetAddress instanceof Inet6Address) {
                if (inetAddress.isLinkLocalAddress()) {
                    return address;
                }
            }
        }
        throw new IllegalArgumentException("ni does not have IPv6 address.");
    }

    @Nonnull
    public InterfaceAddress getInterfaceAddress() {
        return mInterfaceAddress;
    }

    @Nonnull
    public InetAddress getLocalAddress() {
        return mInterfaceAddress.getAddress();
    }

    @Override
    public void start() {
        if (mFutureTask != null) {
            stop();
        }
        mReady = false;
        mFutureTask = new FutureTask<>(this, null);
        mTaskExecutors.server(mFutureTask);
    }

    @Override
    public void stop() {
        if (mFutureTask == null) {
            return;
        }
        mFutureTask.cancel(false);
        mFutureTask = null;
        IoUtils.closeQuietly(mSocket);
    }

    @Override
    public void send(@Nonnull final SsdpMessage message) {
        mTaskExecutors.io(() -> {
            sendInner(message);
        });
    }

    private void sendInner(@Nonnull final SsdpMessage message) {
        if (!waitReady()) {
            Logger.w("socket is not ready");
            return;
        }
        final MulticastSocket socket = mSocket;
        if (socket == null) {
            return;
        }
        Logger.d(() -> "send from " + mInterfaceAddress + ":\n" + message);
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.writeData(baos);
            final byte[] data = baos.toByteArray();
            socket.send(new DatagramPacket(data, data.length, mAddress.getSocketAddress()));
        } catch (final IOException e) {
            Logger.w(e);
        }
    }

    private synchronized boolean waitReady() {
        final FutureTask<?> task = mFutureTask;
        if (task == null || task.isDone()) {
            return false;
        }
        if (!mReady) {
            try {
                wait(1000);
            } catch (final InterruptedException ignored) {
            }
        }
        return mReady;
    }

    private synchronized void ready() {
        mReady = true;
        notifyAll();
    }

    @Override
    public void run() {
        final String suffix = (mBindPort == 0 ? "-ssdp-notify-" : "-ssdp-search-")
                + mInterface.getName() + "-"
                + NetworkUtils.toSimpleString(mInterfaceAddress.getAddress());
        final Thread thread = Thread.currentThread();
        thread.setName(thread.getName() + suffix);
        if (mFutureTask == null || mFutureTask.isCancelled()) {
            return;
        }
        try {
            mSocket = createMulticastSocket(mBindPort);
            mSocket.setNetworkInterface(mInterface);
            mSocket.setTimeToLive(4);
            joinGroup();
            ready();
            receiveLoop();
        } catch (final IOException ignored) {
        } finally {
            leaveGroup();
            mSocket = null;
        }
    }

    /**
     * 受信処理を行う。
     *
     * @throws IOException 入出力処理で例外発生
     */
    // VisibleForTesting
    void receiveLoop() throws IOException {
        final byte[] buf = new byte[1500];
        while (mFutureTask != null && !mFutureTask.isCancelled()) {
            try {
                final DatagramPacket dp = new DatagramPacket(buf, buf.length);
                mSocket.receive(dp);
                if (mFutureTask == null || mFutureTask.isCancelled()) {
                    break;
                }
                mReceiver.onReceive(dp.getAddress(), dp.getData(), dp.getLength());
            } catch (final SocketTimeoutException ignored) {
            }
        }
    }

    /**
     * Joinを行う。
     *
     * <p>特定ポートにBindしていない（マルチキャスト受信ソケットでない）場合は何も行わない
     *
     * @throws IOException Joinコールにより発生
     */
    // VisibleForTesting
    void joinGroup() throws IOException {
        if (mBindPort != 0) {
            mSocket.joinGroup(getSsdpInetAddress());
        }
    }

    /**
     * Leaveを行う。
     *
     * <p>特定ポートにBindしていない（マルチキャスト受信ソケットでない）場合は何も行わない
     */
    // VisibleForTesting
    void leaveGroup() {
        if (mBindPort != 0) {
            try {
                mSocket.leaveGroup(getSsdpInetAddress());
            } catch (final IOException ignored) {
            }
        }
    }

    // VisibleForTesting
    @Nonnull
    MulticastSocket createMulticastSocket(final int port) throws IOException {
        return new MulticastSocket(port);
    }
}
