/*
 * Copyright(C)  2018 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.log.Log;
import net.mm2d.util.IoUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * SsdpServerの共通処理を実装するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
// TODO: SocketChannelを使用した受信(MulticastChannelはAndroid N以降のため保留)
class SsdpServerDelegate implements SsdpServer {
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
    private final Receiver mReceiver;
    @Nonnull
    private final NetworkInterface mInterface;
    @Nonnull
    private final InterfaceAddress mInterfaceAddress;

    private final int mBindPort;
    @Nullable
    private MulticastSocket mSocket;
    @Nullable
    private ReceiveTask mReceiveTask;

    /**
     * 使用するインターフェースを指定してインスタンス作成。
     *
     * <p>使用するポートは自動割当となる。
     *
     * @param receiver         パケット受信時にコールされるreceiver
     * @param networkInterface 使用するインターフェース
     */
    SsdpServerDelegate(
            @Nonnull final Receiver receiver,
            @Nonnull final NetworkInterface networkInterface) {
        this(receiver, networkInterface, 0);
    }

    /**
     * 使用するインターフェースとポート指定してインスタンス作成。
     *
     * @param receiver         パケット受信時にコールされるreceiver
     * @param networkInterface 使用するインターフェース
     * @param bindPort         使用するポート
     */
    SsdpServerDelegate(
            @Nonnull final Receiver receiver,
            @Nonnull final NetworkInterface networkInterface,
            final int bindPort) {
        mInterface = networkInterface;
        mInterfaceAddress = findInet4Address(networkInterface.getInterfaceAddresses());
        mBindPort = bindPort;
        mReceiver = receiver;
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

    @Override
    @Nonnull
    public InterfaceAddress getInterfaceAddress() {
        return mInterfaceAddress;
    }

    @Override
    public void open() throws IOException {
        if (mSocket != null) {
            close();
        }
        mSocket = createMulticastSocket(mBindPort);
        mSocket.setNetworkInterface(mInterface);
        mSocket.setTimeToLive(4);
    }

    // VisibleForTesting
    @Nonnull
    MulticastSocket createMulticastSocket(final int port) throws IOException {
        return new MulticastSocket(port);
    }

    @Override
    public void close() {
        stop();
        IoUtils.closeQuietly(mSocket);
        mSocket = null;
    }

    @Override
    public void start() {
        if (mReceiveTask != null) {
            stop();
        }
        mReceiveTask = new ReceiveTask(mReceiver, mSocket, mBindPort);
        mReceiveTask.start();
    }

    @Override
    public void stop() {
        stop(false);
    }

    /**
     * 受信スレッドの停止と必要があればJoinを行う。
     *
     * <p>現在の実装ではIO待ちに割り込むことはできないため、
     * joinを指定しても偶然ソケットタイムアウトやソケット受信が発生しないかぎりjoinできない。
     *
     * @param join trueの時スレッドのJoin待ちを行う。
     */
    // VisibleForTesting
    void stop(final boolean join) {
        if (mReceiveTask == null) {
            return;
        }
        mReceiveTask.shutdownRequest(join);
        mReceiveTask = null;
    }

    @Override
    public void send(@Nonnull final SsdpMessage message) {
        if (mSocket == null) {
            return;
        }
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.writeData(baos);
            final byte[] data = baos.toByteArray();
            mSocket.send(new DatagramPacket(data, data.length, SSDP_SO_ADDR));
        } catch (final IOException e) {
            Log.w(e);
        }
    }

    /**
     * SsdpMessageのLocationに正常なURLが記述されており、
     * 記述のアドレスとパケットの送信元アドレスに不一致がないか検査する。
     *
     * @param message       確認するSsdpMessage
     * @param sourceAddress 送信元アドレス
     * @return true:送信元との不一致を含めてLocationに不正がある場合。false:それ以外
     */
    public boolean isInvalidLocation(
            @Nonnull final SsdpMessage message,
            @Nonnull final InetAddress sourceAddress) {
        return !isValidLocation(message, sourceAddress);
    }

    private boolean isValidLocation(
            @Nonnull final SsdpMessage message,
            @Nonnull final InetAddress sourceAddress) {
        final String location = message.getLocation();
        if (!Http.isHttpUrl(location)) {
            return false;
        }
        try {
            final InetAddress locationAddress = InetAddress.getByName(new URL(location).getHost());
            return sourceAddress.equals(locationAddress);
        } catch (MalformedURLException | UnknownHostException ignored) {
        }
        return false;
    }

    // VisibleForTesting
    static class ReceiveTask implements Runnable {
        @Nonnull
        private final Receiver mReceiver;
        @Nonnull
        private final MulticastSocket mSocket;
        private final int mBindPort;

        private volatile boolean mShutdownRequest;
        @Nullable
        private Thread mThread;

        /**
         * インスタンス作成
         */
        ReceiveTask(
                @Nonnull final Receiver receiver,
                @Nonnull final MulticastSocket socket,
                final int port) {
            mReceiver = receiver;
            mSocket = socket;
            mBindPort = port;
        }

        /**
         * スレッドを作成して処理を開始する。
         */
        synchronized void start() {
            mShutdownRequest = false;
            mThread = new Thread(this, getClass().getSimpleName());
            mThread.start();
        }

        /**
         * 割り込みを行い、スレッドを終了させる。
         *
         * <p>現在はSocketを使用しているため割り込みは効果がない。
         *
         * @param join Threadのjoin待ちを行う場合はtrue
         */
        synchronized void shutdownRequest(final boolean join) {
            mShutdownRequest = true;
            if (mThread == null) {
                return;
            }
            mThread.interrupt();
            if (join) {
                try {
                    mThread.join(1000);
                } catch (final InterruptedException ignored) {
                }
            }
            mThread = null;
        }

        @Override
        public void run() {
            if (mShutdownRequest) {
                return;
            }
            try {
                joinGroup();
                receiveLoop();
                leaveGroup();
            } catch (final IOException ignored) {
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
            while (!mShutdownRequest) {
                try {
                    final DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    mSocket.receive(dp);
                    if (mShutdownRequest) {
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
                mSocket.joinGroup(SSDP_INET_ADDR);
            }
        }

        /**
         * Leaveを行う。
         *
         * <p>特定ポートにBindしていない（マルチキャスト受信ソケットでない）場合は何も行わない
         *
         * @throws IOException Leaveコールにより発生
         */
        // VisibleForTesting
        void leaveGroup() throws IOException {
            if (mBindPort != 0) {
                mSocket.leaveGroup(SSDP_INET_ADDR);
            }
        }
    }
}
