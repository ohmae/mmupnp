/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InterfaceAddress;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class SsdpRequestMessage extends SsdpMessage {
    public SsdpRequestMessage() {
        super();
    }

    public SsdpRequestMessage(InterfaceAddress addr, DatagramPacket dp) throws IOException {
        super(addr, dp);
    }

    @Override
    protected HttpMessage newMessage() {
        return new HttpRequest();
    }

    @Override
    protected HttpRequest getMessage() {
        return (HttpRequest) super.getMessage();
    }

    public String getMethod() {
        return getMessage().getMethod();
    }

    public void setMethod(String method) {
        getMessage().setMethod(method);
    }

    public String getUri() {
        return getMessage().getUri();
    }

    public void setUri(String uri) {
        getMessage().setUri(uri);
    }
}
