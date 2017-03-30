/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
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
    public void setTimeToLive(int ttl) throws IOException {
    }

    @Override
    public void joinGroup(InetAddress mcastaddr) throws IOException {
    }

    @Override
    public void leaveGroup(InetAddress mcastaddr) throws IOException {
    }

    @Override
    public void setNetworkInterface(NetworkInterface netIf) throws SocketException {
    }

    @Override
    public void send(DatagramPacket p) throws IOException {
        mDatagramPacket = p;
    }

    public DatagramPacket getSendPacket() {
        return mDatagramPacket;
    }

    public void setReceiveData(InetAddress address, byte[] data, long wait) {
        mInetAddress = address;
        mReceiveData = data;
        mWait = wait;
    }

    @Override
    public synchronized void receive(DatagramPacket p) throws IOException {
        if (mReceiveData == null) {
            try {
                Thread.sleep(100000L);
            } catch (InterruptedException e) {
                throw new IOException();
            }
        }
        try {
            Thread.sleep(mWait);
        } catch (InterruptedException e) {
            throw new IOException();
        }
        System.arraycopy(mReceiveData, 0, p.getData(), 0, mReceiveData.length);
        p.setAddress(mInetAddress);
        mReceiveData = null;
    }
}
