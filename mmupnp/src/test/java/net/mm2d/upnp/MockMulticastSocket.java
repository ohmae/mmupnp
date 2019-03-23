/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;

public class MockMulticastSocket extends MulticastSocket {
    private DatagramPacket mDatagramPacket;
    private InetAddress mInetAddress;
    private byte[] mReceiveData;
    private long mWait;

    public MockMulticastSocket() throws IOException {
        super();
    }

    @Override
    public void setTimeToLive(final int ttl) {
    }

    @Override
    public void joinGroup(final InetAddress mcastaddr) {
    }

    @Override
    public void leaveGroup(final InetAddress mcastaddr) {
    }

    @Override
    public void setNetworkInterface(final NetworkInterface netIf) {
    }

    @Override
    public void send(final DatagramPacket p) {
        mDatagramPacket = p;
    }

    public DatagramPacket getSendPacket() {
        return mDatagramPacket;
    }

    public void setReceiveData(
            final InetAddress address,
            final byte[] data,
            final long wait) {
        mInetAddress = address;
        mReceiveData = data;
        mWait = wait;
    }

    @Override
    public synchronized void receive(final DatagramPacket p) throws IOException {
        if (mReceiveData == null) {
            try {
                Thread.sleep(100000L);
            } catch (final InterruptedException e) {
                throw new IOException();
            }
        }
        try {
            Thread.sleep(mWait);
        } catch (final InterruptedException e) {
            throw new IOException();
        }
        System.arraycopy(mReceiveData, 0, p.getData(), 0, mReceiveData.length);
        p.setLength(mReceiveData.length);
        p.setAddress(mInetAddress);
        mReceiveData = null;
    }
}
