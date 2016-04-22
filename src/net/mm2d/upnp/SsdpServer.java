/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import net.mm2d.util.Log;

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

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
abstract class SsdpServer {
    public static final String SSDP_ADDR = "239.255.255.250";
    public static final int SSDP_PORT = 1900;
    private static final InetSocketAddress SSDP_SO_ADDR =
            new InetSocketAddress(SSDP_ADDR, SSDP_PORT);
    private static final InetAddress SSDP_INET_ADDR = SSDP_SO_ADDR.getAddress();
    private static final String TAG = "SsdpServer";
    private final NetworkInterface mInterface;
    private InterfaceAddress mInterfaceAddress;
    private final int mBindPort;
    private MulticastSocket mSocket;
    private ReceiveThread mThread;

    public SsdpServer(NetworkInterface ni) {
        this(ni, 0);
    }

    public SsdpServer(NetworkInterface ni, int bindPort) {
        mBindPort = bindPort;
        mInterface = ni;
        final List<InterfaceAddress> ifas = mInterface.getInterfaceAddresses();
        for (final InterfaceAddress ifa : ifas) {
            if (ifa.getAddress() instanceof Inet4Address) {
                mInterfaceAddress = ifa;
                break;
            }
        }
        if (mInterfaceAddress == null) {
            throw new IllegalArgumentException("ni does not have IPv4 address.");
        }
    }

    public void open() throws IOException {
        if (mSocket == null) {
            close();
        }
        mSocket = new MulticastSocket(mBindPort);
        mSocket.setNetworkInterface(mInterface);
        mSocket.setTimeToLive(4);
    }

    public void close() {
        stop(false);
        if (mSocket != null) {
            mSocket.close();
            mSocket = null;
        }
    }

    public void start() {
        if (mThread != null) {
            stop(false);
        }
        mThread = new ReceiveThread();
        mThread.start();
    }

    public void stop() {
        stop(false);
    }

    public void stop(boolean join) {
        if (mThread == null) {
            return;
        }
        mThread.shutdownRequest();
        if (join) {
            try {
                mThread.join(1000);
            } catch (final InterruptedException e) {
            }
            mThread = null;
        }
    }

    public void send(SsdpMessage message) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            message.getMessage().writeData(baos);
            send(baos.toByteArray());
        } catch (final IOException e) {
            Log.w(TAG, e);
        }
    }

    public void send(byte[] message) {
        final DatagramPacket dp = new DatagramPacket(message, message.length, SSDP_SO_ADDR);
        try {
            mSocket.send(dp);
        } catch (final IOException e) {
            Log.w(TAG, e);
        }
    }

    protected abstract void onReceive(InterfaceAddress addr, DatagramPacket dp);

    private void joinGroup() throws IOException {
        if (mBindPort != 0) {
            mSocket.joinGroup(SSDP_INET_ADDR);
        }
    }

    private void leaveGroup() throws IOException {
        if (mBindPort != 0) {
            mSocket.leaveGroup(SSDP_INET_ADDR);
        }
    }

    private class ReceiveThread extends Thread {
        private volatile boolean mShutdownRequest;

        public ReceiveThread() {
            super("ReceiveThread");
        }

        public void shutdownRequest() {
            mShutdownRequest = true;
            interrupt();
        }

        @Override
        public void run() {
            try {
                joinGroup();
                while (!mShutdownRequest) {
                    final byte[] buf = new byte[1500];
                    final DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    try {
                        mSocket.receive(dp);
                        onReceive(mInterfaceAddress, dp);
                    } catch (final SocketTimeoutException e) {
                    }
                }
                leaveGroup();
            } catch (final IOException ignored) {
            }
        }
    }
}
