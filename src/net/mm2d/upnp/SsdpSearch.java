/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import java.net.NetworkInterface;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class SsdpSearch extends SsdpServer {
    public static final String ST_ALL = "ssdp:all";
    public static final String ST_ROOTDEVICE = "upnp:rootdevice";

    public interface ResponseListener {
        void onReceiveResponse(SsdpMessage packet);
    }

    private ResponseListener mListener;

    public SsdpSearch(NetworkInterface ni) {
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
        final String message = "M-SEARCH * HTTP/1.1\r\n"
                + "ST: " + st + "\r\n"
                + "MAN: \"ssdp:discover\"\r\n"
                + "MX: 1\r\n"
                + "HOST: 239.255.255.250:1900\r\n\r\n";
        send(message);
    }

    @Override
    protected void onReceive(SsdpMessage packet) {
        if (packet.getMethod() == SsdpMessage.Method.M_SEARCH) {
            return;
        }
        if (mListener != null) {
            mListener.onReceiveResponse(packet);
        }
    }
}
