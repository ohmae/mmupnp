/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class SsdpPacket {
    public enum Method {
        RESPONSE("HTTP"),
        M_SEARCH("M-SEARCH"),
        NOTIFY("NOTIFY");
        private String mName;

        private Method(String name) {
            mName = name;
        }

        private boolean match(String line) {
            final String name = line.substring(0, mName.length());
            return mName.equalsIgnoreCase(name);
        }

        public static Method getType(String line) {
            for (final Method t : values()) {
                if (t.match(line)) {
                    return t;
                }
            }
            return null;
        }
    }

    /**
     * Notification Type Sub
     */
    public enum Nts {
        ALIVE("ssdp:alive"),
        BYEBYE("ssdp:byebye"),
        UPDATE("ssdp:update"),
        PROPCHANGE("upnp:propchange");
        private static Map<String, Nts> mMap;
        private String mMessage;

        private Nts(String message) {
            mMessage = message;
        }

        static {
            mMap = new HashMap<>();
            for (final Nts nts : Nts.values()) {
                mMap.put(nts.mMessage, nts);
            }
        }

        public static Nts fromMessage(String message) {
            return mMap.get(message);
        }
    }

    private static final int DEFAULT_MAX_AGE = 1800;

    private final Method mMethod;
    private Nts mNts;
    private int mMaxAge;
    private final long mExpireTime;
    private String mUuid;
    private String mType;
    private final String mLocation;
    private final Map<String, String> mHeaders = new HashMap<>();
    private final String mRequestHeader; // Responseと同じクラスで表現するのがまずいか？
    private final InterfaceAddress mInterfaceAddress;
    private final InetSocketAddress mSourceAddress;
    private final boolean mValidSegment;

    public SsdpPacket(InterfaceAddress addr, DatagramPacket dp) {
        mInterfaceAddress = addr;
        mSourceAddress = (InetSocketAddress) dp.getSocketAddress();
        mValidSegment = isSameSegment(mInterfaceAddress, mSourceAddress);
        final String message = new String(dp.getData(), 0, dp.getLength());
        final String[] lines = message.split("\r\n");
        if (lines.length < 1) {
            throw new IllegalArgumentException();
        }
        mRequestHeader = lines[0];
        mMethod = Method.getType(mRequestHeader);
        if (mMethod == null) {
            throw new IllegalArgumentException();
        }
        for (int i = 1; i < lines.length; i++) {
            final String line = lines[i];
            final int index = line.indexOf(':');
            if (index < 0) {
                continue;
            }
            final String key = line.substring(0, index).trim();
            String value;
            if (index + 1 == line.length()) {
                value = "";
            } else {
                value = line.substring(index + 1).trim();
            }
            mHeaders.put(key.toUpperCase(), value);
        }
        parseCacheControl();
        mExpireTime = mMaxAge * 1000 + System.currentTimeMillis();
        parseUsn();
        parseNts();
        mLocation = mHeaders.get(Http.LOCATION);
    }

    private boolean isSameSegment(InterfaceAddress ifa, InetSocketAddress sa) {
        final byte[] a = ifa.getAddress().getAddress();
        final byte[] b = sa.getAddress().getAddress();
        final int pref = ifa.getNetworkPrefixLength();
        final int bytes = pref / 8;
        final int bits = pref % 8;
        for (int i = 0; i < bytes; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        if (bits != 0) {
            final byte mask = (byte) (0xff << (8 - bits));
            if ((a[bytes] & mask) != (b[bytes] & mask)) {
                return false;
            }
        }
        return true;
    }

    public InterfaceAddress getInterfaceAddress() {
        return mInterfaceAddress;
    }

    private void parseCacheControl() {
        mMaxAge = DEFAULT_MAX_AGE;
        final String age = mHeaders.get(Http.CACHE_CONTROL);
        if (age == null || !age.toLowerCase().startsWith("max-age")) {
            return;
        }
        final int pos = age.indexOf('=');
        if (pos < 0 || pos + 1 == age.length()) {
            return;
        }
        try {
            mMaxAge = Integer.parseInt(age.substring(pos + 1));
        } catch (final NumberFormatException e) {
            return;
        }
    }

    private void parseUsn() {
        final String usn = mHeaders.get(Http.USN);
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

    private void parseNts() {
        if (mMethod != Method.NOTIFY) {
            return;
        }
        mNts = Nts.fromMessage(mHeaders.get(Http.NTS));
    }

    public boolean isValidSegment() {
        return mValidSegment;
    }

    public Method getMethod() {
        return mMethod;
    }

    public String getHeaderValue(String key) {
        return mHeaders.get(key);
    }

    public String getUuid() {
        return mUuid;
    }

    public String getType() {
        return mType;
    }

    public Nts getNts() {
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
}
