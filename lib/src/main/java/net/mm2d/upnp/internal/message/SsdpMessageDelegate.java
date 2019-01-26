/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message;

import net.mm2d.upnp.Http;
import net.mm2d.upnp.HttpMessage;
import net.mm2d.upnp.SsdpMessage;
import net.mm2d.util.TextUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * SsdpMessageの共通実装。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
class SsdpMessageDelegate implements SsdpMessage {
    private static final int DEFAULT_MAX_AGE = 1800;
    @Nonnull
    private final HttpMessage mMessage;

    private final int mMaxAge;

    private final long mExpireTime;
    @Nonnull
    private final String mUuid;
    @Nonnull
    private final String mType;
    @Nullable
    private final String mNts;
    @Nullable
    private String mLocation;
    @Nullable
    private final InetAddress mLocalAddress;

    public SsdpMessageDelegate(@Nonnull final HttpMessage message) {
        mMessage = message;
        mMaxAge = 0;
        mExpireTime = 0;
        mUuid = "";
        mType = "";
        mNts = "";
        mLocation = null;
        mLocalAddress = null;
    }

    public SsdpMessageDelegate(
            @Nonnull final HttpMessage message,
            @Nonnull final InetAddress address) {
        mMessage = message;
        mLocalAddress = address;
        mMaxAge = parseCacheControl(mMessage);
        final String[] result = parseUsn(mMessage);
        mUuid = result[0];
        mType = result[1];
        mLocation = mMessage.getHeader(Http.LOCATION);
        mNts = mMessage.getHeader(Http.NTS);
        mExpireTime = TimeUnit.SECONDS.toMillis(mMaxAge) + System.currentTimeMillis();
    }

    // VisibleForTesting
    void updateLocation() {
        mLocation = mMessage.getHeader(Http.LOCATION);
    }

    // VisibleForTesting
    static int parseCacheControl(@Nonnull final HttpMessage message) {
        final String age = TextUtils.toLowerCase(message.getHeader(Http.CACHE_CONTROL));
        if (TextUtils.isEmpty(age) || !age.startsWith("max-age")) {
            return DEFAULT_MAX_AGE;
        }
        final int pos = age.indexOf('=');
        if (pos < 0 || pos + 1 == age.length()) {
            return DEFAULT_MAX_AGE;
        }
        try {
            return Integer.parseInt(age.substring(pos + 1));
        } catch (final NumberFormatException ignored) {
        }
        return DEFAULT_MAX_AGE;
    }

    // VisibleForTesting
    @Nonnull
    static String[] parseUsn(@Nonnull final HttpMessage message) {
        final String usn = message.getHeader(Http.USN);
        if (TextUtils.isEmpty(usn) || !usn.startsWith("uuid")) {
            return new String[]{"", ""};
        }
        final int pos = usn.indexOf("::");
        if (pos < 0) {
            return new String[]{usn, ""};
        }
        return new String[]{usn.substring(0, pos), usn.substring(pos + 2)};
    }

    @Override
    public boolean isPinned() {
        return false;
    }

    @Override
    public int getScopeId() {
        if (mLocalAddress == null) {
            return 0;
        }
        if (mLocalAddress instanceof Inet6Address) {
            return ((Inet6Address) mLocalAddress).getScopeId();
        }
        return 0;
    }

    @Nullable
    @Override
    public InetAddress getLocalAddress() {
        return mLocalAddress;
    }

    @Nullable
    @Override
    public String getHeader(@Nonnull final String name) {
        return mMessage.getHeader(name);
    }

    @Override
    public void setHeader(
            @Nonnull final String name,
            @Nonnull final String value) {
        mMessage.setHeader(name, value);
    }

    @Nonnull
    @Override
    public String getUuid() {
        return mUuid;
    }

    @Nonnull
    @Override
    public String getType() {
        return mType;
    }

    @Nullable
    @Override
    public String getNts() {
        return mNts;
    }

    @Override
    public int getMaxAge() {
        return mMaxAge;
    }

    @Override
    public long getExpireTime() {
        return mExpireTime;
    }

    @Nullable
    @Override
    public String getLocation() {
        return mLocation;
    }

    @Override
    public void writeData(@Nonnull final OutputStream os) throws IOException {
        mMessage.writeData(os);
    }

    @Nonnull
    @Override
    public String toString() {
        return mMessage.toString();
    }
}
