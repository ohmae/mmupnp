/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import net.mm2d.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class SsdpNotifyReceiver extends SsdpServer {
    public interface NotifyListener {
        void onReceiveNotify(SsdpRequestMessage message);
    }

    private static final String TAG = "SsdpNotifyReceiver";
    private NotifyListener mListener;

    public SsdpNotifyReceiver(NetworkInterface ni) {
        super(ni, SsdpServer.PORT);
    }

    public void setNotifyListener(NotifyListener listener) {
        mListener = listener;
    }

    @Override
    protected void onReceive(InterfaceAddress addr, DatagramPacket dp) {
        if (!isSameSegment(addr, dp)) {
            Log.w(TAG, "Invalid segment packet received:" + dp.getAddress().toString());
            return;
        }
        try {
            final SsdpRequestMessage message = new SsdpRequestMessage(addr, dp);
            if (mListener != null) {
                mListener.onReceiveNotify(message);
            }
        } catch (final IOException ignored) {
        }
    }

    private static boolean isSameSegment(InterfaceAddress ifa, DatagramPacket dp) {
        final InetSocketAddress sa = (InetSocketAddress) dp.getSocketAddress();
        final byte[] a = ifa.getAddress().getAddress();
        final byte[] b = sa.getAddress().getAddress();
        final int pref = ifa.getNetworkPrefixLength();
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
