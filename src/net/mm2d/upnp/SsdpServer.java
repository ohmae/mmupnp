/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public abstract class SsdpServer {
    protected static final String MCAST_ADDR = "239.255.255.250";
    protected static final int PORT = 1900;
    private final NetworkInterface mInterface;
    private InterfaceAddress mInterfaceAddress;
    private final int mBindPort;
    private MulticastSocket mSocket;
    private InetAddress mMulticastAddress;
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
        try {
            mMulticastAddress = InetAddress.getByName(MCAST_ADDR);
        } catch (final UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void open() throws IOException {
        if (mSocket == null) {
            close();
        }
        mSocket = new MulticastSocket(mBindPort);
        mSocket.setSoTimeout(1000);
        mSocket.setNetworkInterface(mInterface);
        mSocket.setTimeToLive(4);
    }

    public void close() {
        if (mSocket != null) {
            mSocket.close();
            mSocket = null;
        }
    }

    public void start() {
        if (mThread != null) {
            stop();
        }
        mThread = new ReceiveThread();
        mThread.start();
    }

    public void stop() {
        if (mThread != null) {
            mThread.shutdownRequest();
            mThread = null;
        }
    }

    public void send(SsdpMessage message) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            message.getMessage().writeData(baos);
            send(baos.toByteArray());
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void send(byte[] message) {
        final DatagramPacket dp = new DatagramPacket(message, message.length,
                mMulticastAddress, PORT);
        try {
            mSocket.send(dp);
            mSocket.send(dp);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract void onReceive(InterfaceAddress addr, DatagramPacket dp);

    private class ReceiveThread extends Thread {
        private volatile boolean mShutdownRequest;

        public void shutdownRequest() {
            mShutdownRequest = true;
            interrupt();
        }

        @Override
        public void run() {
            try {
                mSocket.joinGroup(mMulticastAddress);
                while (!mShutdownRequest) {
                    final byte[] buf = new byte[1024];
                    final DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    try {
                        mSocket.receive(dp);
                        onReceive(mInterfaceAddress, dp);
                    } catch (final SocketTimeoutException e) {
                    }
                }
                mSocket.leaveGroup(mMulticastAddress);
            } catch (final UnknownHostException e) {
            } catch (final SocketException e) {
            } catch (final IOException e) {
            }
        }
    }
}
