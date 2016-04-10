/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import java.net.DatagramPacket;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class SsdpNotifyReceiver extends SsdpServer {
    public interface NotifyListener {
        void onReceiveNotify(SsdpRequestMessage message);
    }

    private NotifyListener mListener;

    public SsdpNotifyReceiver(NetworkInterface ni) {
        super(ni, SsdpServer.PORT);
    }

    public void setNotifyListener(NotifyListener listener) {
        mListener = listener;
    }

    @Override
    protected void onReceive(InterfaceAddress addr, DatagramPacket dp) {
        final SsdpRequestMessage message = new SsdpRequestMessage(addr, dp);
        if (mListener != null) {
            mListener.onReceiveNotify(message);
        }
    }
}
