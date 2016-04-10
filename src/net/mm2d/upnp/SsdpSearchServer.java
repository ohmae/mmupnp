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
class SsdpSearchServer extends SsdpServer {
    public static final String ST_ALL = "ssdp:all";
    public static final String ST_ROOTDEVICE = "upnp:rootdevice";

    public interface ResponseListener {
        void onReceiveResponse(SsdpResponseMessage message);
    }

    private ResponseListener mListener;

    public SsdpSearchServer(NetworkInterface ni) {
        super(ni);
    }

    public void setResponseListener(ResponseListener listener) {
        mListener = listener;
    }

    public void search() {
        search(null);
    }

    public void search(String st) {
        if (st == null) {
            st = ST_ALL;
        }
        final SsdpRequestMessage message = new SsdpRequestMessage();
        message.setMethod(SsdpMessage.M_SEARCH);
        message.setUri("*");
        message.setHeader(Http.HOST, "239.255.255.250:1900");
        message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER);
        message.setHeader(Http.MX, "1");
        message.setHeader(Http.ST, st);
        send(message);
    }

    @Override
    protected void onReceive(InterfaceAddress addr, DatagramPacket dp) {
        final SsdpResponseMessage message = new SsdpResponseMessage(addr, dp);
        if (mListener != null) {
            mListener.onReceiveResponse(message);
        }
    }
}
