/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.Log;

import java.io.IOException;
import java.net.URL;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class Icon {
    public static class Builder {
        private Device mDevice;
        private String mMimeType;
        private int mHeight;
        private int mWidth;
        private int mDepth;
        private String mUrl;
        private byte[] mBinary;

        public Builder() {
        }

        public void setDevice(Device device) {
            mDevice = device;
        }

        public void setMimeType(String mimeType) {
            mMimeType = mimeType;
        }

        public void setHeight(String height) {
            mHeight = Integer.parseInt(height);
        }

        public void setWidth(String width) {
            mWidth = Integer.parseInt(width);
        }

        public void setDepth(String depth) {
            mDepth = Integer.parseInt(depth);
        }

        public void setUrl(String url) {
            mUrl = url;
        }

        public void setBinary(byte[] binary) {
            mBinary = binary;
        }

        public Icon build() {
            return new Icon(this);
        }
    }

    private static final String TAG = "Icon";
    private final Device mDevice;
    private final String mMimeType;
    private final int mHeight;
    private final int mWidth;
    private final int mDepth;
    private final String mUrl;
    private byte[] mBinary;

    private Icon(Builder builder) {
        mDevice = builder.mDevice;
        mMimeType = builder.mMimeType;
        mHeight = builder.mHeight;
        mWidth = builder.mWidth;
        mDepth = builder.mDepth;
        mUrl = builder.mUrl;
        mBinary = builder.mBinary;
    }

    public Device getDevice() {
        return mDevice;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getDepth() {
        return mDepth;
    }

    public String getUrl() {
        return mUrl;
    }

    public void loadBinary(HttpClient client) throws IOException {
        final URL url = mDevice.getAbsoluteUrl(mUrl);
        final HttpRequest request = new HttpRequest();
        request.setMethod(Http.GET);
        request.setUrl(url, true);
        request.setHeader(Http.USER_AGENT, Http.USER_AGENT_VALUE);
        request.setHeader(Http.CONNECTION, Http.KEEP_ALIVE);
        final HttpResponse response = client.post(request);
        if (response.getStatus() != Http.Status.HTTP_OK) {
            Log.i(TAG, response.toString());
            throw new IOException(response.getStartLine());
        }
        mBinary = response.getBodyBinary();
    }

    public byte[] getBinary() {
        return mBinary;
    }
}
