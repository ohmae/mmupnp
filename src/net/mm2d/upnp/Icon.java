/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import java.io.IOException;
import java.net.URL;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class Icon {
    private final Device mDevice;
    private String mMimeType;
    private int mHeight;
    private int mWidth;
    private int mDepth;
    private String mUrl;
    private byte[] mBinary;

    public Icon(Device device) {
        mDevice = device;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setMimeType(String mimeType) {
        mMimeType = mimeType;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(String height) {
        mHeight = Integer.parseInt(height);
    }

    public int getWidth() {
        return mWidth;
    }

    public void setWidth(String width) {
        mWidth = Integer.parseInt(width);
    }

    public int getDepth() {
        return mDepth;
    }

    public void setDepth(String depth) {
        mDepth = Integer.parseInt(depth);
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public void getIcon(HttpClient client) throws IOException {
        final URL url = mDevice.getAbsoluteUrl(mUrl);
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.GET);
        request.setUrl(url, true);
        request.setHeader(Http.USER_AGENT, Http.USER_AGENT_VALUE);
        request.setHeader(Http.CONNECTION, Http.KEEP_ALIVE);
        final HttpResponse response = client.post(request);
        mBinary = response.getBodyBin();
    }

    public byte[] getBinary() {
        return mBinary;
    }
}
