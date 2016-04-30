/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public abstract class SsdpMessage {
    public static final String M_SEARCH = "M-SEARCH";
    public static final String NOTIFY = "NOTIFY";
    public static final String SSDP_ALIVE = "ssdp:alive";
    public static final String SSDP_BYEBYE = "ssdp:byebye";
    public static final String SSDP_UPDATE = "ssdp:update";
    public static final String SSDP_DISCOVER = "\"ssdp:discover\"";

    private final HttpMessage mMessage;
    private static final int DEFAULT_MAX_AGE = 1800;
    private int mMaxAge;
    private long mExpireTime;
    private String mUuid;
    private String mType;
    private String mNts;
    private String mLocation;
    private InetAddress mPacketAddress;
    private InterfaceAddress mInterfaceAddress;

    protected abstract HttpMessage newMessage();

    protected HttpMessage getMessage() {
        return mMessage;
    }

    public SsdpMessage() {
        mMessage = newMessage();
    }

    public SsdpMessage(InterfaceAddress addr, DatagramPacket dp) throws IOException {
        mMessage = newMessage();
        mInterfaceAddress = addr;
        mMessage.readData(new ByteArrayInputStream(dp.getData(), 0, dp.getLength()));
        parseMessage();
        mPacketAddress = dp.getAddress();
    }

    public boolean hasValidLocation() {
        if (mLocation == null) {
            return false;
        }
        final String packetAddress = mPacketAddress.getHostAddress();
        try {
            final String locationAddress = new URL(mLocation).getHost();
            if (!packetAddress.equals(locationAddress)) {
                return false;
            }
        } catch (final MalformedURLException e) {
            return false;
        }
        return true;
    }

    public void parseMessage() {
        parseCacheControl();
        parseUsn();
        mExpireTime = mMaxAge * 1000 + System.currentTimeMillis();
        mLocation = mMessage.getHeader(Http.LOCATION);
        mNts = mMessage.getHeader(Http.NTS);
    }

    public InterfaceAddress getInterfaceAddress() {
        return mInterfaceAddress;
    }

    private void parseCacheControl() {
        mMaxAge = DEFAULT_MAX_AGE;
        final String age = mMessage.getHeader(Http.CACHE_CONTROL);
        if (age == null || !age.toLowerCase().startsWith("max-age")) {
            return;
        }
        final int pos = age.indexOf('=');
        if (pos < 0 || pos + 1 == age.length()) {
            return;
        }
        try {
            mMaxAge = Integer.parseInt(age.substring(pos + 1));
        } catch (final NumberFormatException ignored) {
        }
    }

    private void parseUsn() {
        final String usn = mMessage.getHeader(Http.USN);
        if (usn == null || !usn.startsWith("uuid")) {
            return;
        }
        final int pos = usn.indexOf("::");
        if (pos < 0) {
            mUuid = usn;
            return;
        }
        mUuid = usn.substring(0, pos);
        if (pos + 2 < usn.length()) {
            mType = usn.substring(pos + 2);
        }
    }

    public String getHeader(String name) {
        return mMessage.getHeader(name);
    }

    public void setHeader(String name, String value) {
        mMessage.setHeader(name, value);
    }

    public String getUuid() {
        return mUuid;
    }

    public String getType() {
        return mType;
    }

    public String getNts() {
        return mNts;
    }

    public int getMaxAge() {
        return mMaxAge;
    }

    public long getExpireTime() {
        return mExpireTime;
    }

    public String getLocation() {
        return mLocation;
    }

    @Override
    public String toString() {
        return mMessage.toString();
    }
}
