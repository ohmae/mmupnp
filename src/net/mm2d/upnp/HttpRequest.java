/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class HttpRequest extends HttpMessage {
    private String mMethod;
    private String mUri;

    public HttpRequest() {
        super();
    }

    @Override
    public void setStartLine(String line) {
        setRequestLine(line);
    }

    public void setRequestLine(String line) {
        final String[] params = line.split(" ");
        if (params.length < 3) {
            throw new IllegalArgumentException();
        }
        setMethod(params[0]);
        setUri(params[1]);
        setVersion(params[2]);
    }

    @Override
    public String getStartLine() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getMethod());
        sb.append(' ');
        sb.append(getUri());
        sb.append(' ');
        sb.append(getVersion());
        return sb.toString();
    }

    public void setUrl(URL url) throws IOException {
        setUrl(url, false);
    }

    public void setUrl(URL url, boolean withHostHeader) throws IOException {
        if (!"http".equals(url.getProtocol())) {
            throw new IOException();
        }
        setAddress(InetAddress.getByName(url.getHost()));
        setPort(url.getPort());
        setUri(url.getFile());
        if (withHostHeader) {
            setHeader(Http.HOST, getAddressString());
        }
    }

    public String getMethod() {
        return mMethod;
    }

    public void setMethod(String method) {
        mMethod = method;
    }

    public String getUri() {
        return mUri;
    }

    public void setUri(String uri) {
        mUri = uri;
    }
}
