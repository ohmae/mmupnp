/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InterfaceAddress;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class SsdpResponseMessage extends SsdpMessage {
    public SsdpResponseMessage(InterfaceAddress addr, DatagramPacket dp) throws IOException {
        super(addr, dp);
    }

    @Override
    protected HttpMessage newMessage() {
        return new HttpResponse();
    }

    @Override
    protected HttpResponse getMessage() {
        return (HttpResponse) super.getMessage();
    }

    public int getStatusCode() {
        return getMessage().getStatusCode();
    }

    public void setStatusCode(int code) {
        getMessage().setStatusCode(code);
    }

    public String getReasonPhrase() {
        return getMessage().getReasonPhrase();
    }

    public void setReasonPhrase(String reasonPhrase) {
        getMessage().setReasonPhrase(reasonPhrase);
    }

    public void setStatus(Http.Status status) {
        getMessage().setStatus(status);
    }

    public Http.Status getStatus() {
        return getMessage().getStatus();
    }
}
