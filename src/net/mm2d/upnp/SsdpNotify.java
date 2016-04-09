/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import java.net.NetworkInterface;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class SsdpNotify extends SsdpServer {
    public interface NotifyListener {
        void onReceiveNotify(SsdpPacket packet);
    }

    private NotifyListener mListener;

    public SsdpNotify(NetworkInterface ni) {
        super(ni, SsdpServer.PORT);
    }

    public void setNotifyListener(NotifyListener listener) {
        mListener = listener;
    }

    @Override
    protected void onReceive(SsdpPacket packet) {
        if (packet.getMethod() == SsdpPacket.Method.M_SEARCH) {
            return;
        }
        if (mListener != null) {
            mListener.onReceiveNotify(packet);
        }
    }
}
