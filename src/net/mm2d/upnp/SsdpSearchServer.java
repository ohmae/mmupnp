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
public class SsdpSearchServer extends SsdpServer {
    public static final String ST_ALL = "ssdp:all";
    public static final String ST_ROOTDEVICE = "upnp:rootdevice";
    private static final SsdpRequestMessage M_SEARCH = new SsdpRequestMessage();
    static {
        M_SEARCH.setMethod(SsdpMessage.M_SEARCH);
        M_SEARCH.setUri("*");
        M_SEARCH.setHeader(Http.HOST, "239.255.255.250:1900");
        M_SEARCH.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER);
        M_SEARCH.setHeader(Http.MX, "1");
        M_SEARCH.setHeader(Http.ST, ST_ALL);
    }

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
        M_SEARCH.setHeader(Http.ST, st);
        send(M_SEARCH);
    }

    @Override
    protected void onReceive(InterfaceAddress addr, DatagramPacket dp) {
        final SsdpResponseMessage message = new SsdpResponseMessage(addr, dp);
        if (mListener != null) {
            mListener.onReceiveResponse(message);
        }
    }
}
