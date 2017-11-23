/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
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
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * SSDPパケットの受信を行うクラスの親クラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
// TODO: SocketChannelを使用した受信(MulticastChannelはAndroid N以降のため保留)
abstract class SsdpServer {
    /**
     * SSDPに使用するアドレス。
     */
    static final String SSDP_ADDR = "239.255.255.250";
    /**
     * SSDPに使用するポート番号
     */
    static final int SSDP_PORT = 1900;
    private static final InetSocketAddress SSDP_SO_ADDR = new InetSocketAddress(SSDP_ADDR, SSDP_PORT);
    private static final InetAddress SSDP_INET_ADDR = SSDP_SO_ADDR.getAddress();
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
     * @param networkInterface 使用するインターフェース
     */
    SsdpServer(@Nonnull final NetworkInterface networkInterface) {
        this(networkInterface, 0);
    }

    /**
     * 使用するインターフェースとポート指定してインスタンス作成。
     *
     * @param networkInterface 使用するインターフェース
     * @param bindPort         使用するポート
     */
    SsdpServer(
            @Nonnull final NetworkInterface networkInterface,
            final int bindPort) {
        mInterfaceAddress = findInet4Address(networkInterface);
        mBindPort = bindPort;
        mInterface = networkInterface;
    }

    @Nonnull
    private static InterfaceAddress findInet4Address(@Nonnull final NetworkInterface networkInterface) {
        final List<InterfaceAddress> addressList = networkInterface.getInterfaceAddresses();
        for (final InterfaceAddress address : addressList) {
            if (address.getAddress() instanceof Inet4Address) {
                return address;
            }
        }
        throw new IllegalArgumentException("ni does not have IPv4 address.");
    }

    /**
     * BindされたInterfaceのアドレスを返す。
     *
     * @return BindされたInterfaceのアドレス
     */
    @Nonnull
    protected InterfaceAddress getInterfaceAddress() {
        return mInterfaceAddress;
    }

    /**
     * ソケットのオープンを行う。
     *
     * @throws IOException ソケット作成に失敗
     */
    void open() throws IOException {
        if (mSocket != null) {
            close();
        }
        mSocket = createMulticastSocket(mBindPort);
        mSocket.setNetworkInterface(mInterface);
        mSocket.setTimeToLive(4);
    }

    @Nonnull
        // VisibleForTesting
    MulticastSocket createMulticastSocket(final int port) throws IOException {
        return new MulticastSocket(port);
    }

    /**
     * ソケットのクローズを行う
     */
    void close() {
        stop(false);
        IoUtils.closeQuietly(mSocket);
        mSocket = null;
    }

    /**
     * 受信スレッドの開始を行う。
     */
    void start() {
        if (mReceiveTask != null) {
            stop();
        }
        assert mSocket != null;
        mReceiveTask = new ReceiveTask(this, mSocket, mBindPort);
        mReceiveTask.start();
    }

    /**
     * 受信スレッドの停止を行う。
     */
    void stop() {
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
    void stop(final boolean join) {
        if (mReceiveTask == null) {
            return;
        }
        mReceiveTask.shutdownRequest(join);
        mReceiveTask = null;
    }

    /**
     * このソケットを使用してメッセージ送信を行う。
     *
     * @param message 送信するメッセージ
     */
    void send(@Nonnull final SsdpMessage message) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.getMessage().writeData(baos);
            send(baos.toByteArray());
        } catch (final IOException e) {
            Log.w(e);
        }
    }

    /**
     * このソケットを使用してメッセージ送信を行う。
     *
     * @param message 送信するメッセージ
     */
    private void send(@Nonnull final byte[] message) throws IOException {
        assert mSocket != null;
        mSocket.send(new DatagramPacket(message, message.length, SSDP_SO_ADDR));
    }

    /**
     * メッセージ受信後の処理、サブクラスにより実装する。
     *
     * @param sourceAddress 送信元アドレス
     * @param data          受信したデータ
     * @param length        受信したデータの長さ
     */
    protected abstract void onReceive(
            @Nonnull InetAddress sourceAddress,
            @Nonnull byte[] data,
            int length);

    private static class ReceiveTask implements Runnable {
        @Nonnull
        private final SsdpServer mSsdpServer;
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
                @Nonnull final SsdpServer ssdpServer,
                @Nonnull final MulticastSocket socket,
                final int port) {
            mSsdpServer = ssdpServer;
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
        private void receiveLoop() throws IOException {
            final byte[] buf = new byte[1500];
            while (!mShutdownRequest) {
                try {
                    final DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    mSocket.receive(dp);
                    if (mShutdownRequest) {
                        break;
                    }
                    mSsdpServer.onReceive(dp.getAddress(), dp.getData(), dp.getLength());
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
        private void joinGroup() throws IOException {
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
        private void leaveGroup() throws IOException {
            if (mBindPort != 0) {
                mSocket.leaveGroup(SSDP_INET_ADDR);
            }
        }
    }
}
