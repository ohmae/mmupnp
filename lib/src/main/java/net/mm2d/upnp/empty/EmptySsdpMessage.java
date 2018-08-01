/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty;

import net.mm2d.upnp.SsdpMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EmptySsdpMessage implements SsdpMessage {
    @Override
    public int getScopeId() {
        return 0;
    }

    @Nullable
    @Override
    public InetAddress getLocalAddress() {
        return null;
    }

    @Nullable
    @Override
    public String getHeader(@Nonnull final String name) {
        return null;
    }

    @Override
    public void setHeader(
            @Nonnull final String name,
            @Nonnull final String value) {
    }

    @Nonnull
    @Override
    public String getUuid() {
        return "";
    }

    @Nonnull
    @Override
    public String getType() {
        return "";
    }

    @Nullable
    @Override
    public String getNts() {
        return null;
    }

    @Override
    public int getMaxAge() {
        return 0;
    }

    @Override
    public long getExpireTime() {
        return 0;
    }

    @Nullable
    @Override
    public String getLocation() {
        return null;
    }

    @Override
    public void writeData(@Nonnull final OutputStream os) throws IOException {
        throw new IOException("empty object");
    }
}
