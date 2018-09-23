/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty;

import net.mm2d.upnp.ControlPoint;
import net.mm2d.upnp.Device;
import net.mm2d.upnp.IconFilter;
import net.mm2d.upnp.SsdpMessageFilter;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EmptyControlPoint implements ControlPoint {
    @Override
    public void initialize() {
    }

    @Override
    public void terminate() {
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void clearDeviceList() {
    }

    @Override
    public void search() {
    }

    @Override
    public void search(@Nullable final String st) {
    }

    @Override
    public void setSsdpMessageFilter(@Nullable final SsdpMessageFilter filter) {
    }

    @Override
    public void setIconFilter(@Nullable final IconFilter filter) {
    }

    @Override
    public void addDiscoveryListener(@Nonnull final DiscoveryListener listener) {
    }

    @Override
    public void removeDiscoveryListener(@Nonnull final DiscoveryListener listener) {
    }

    @Override
    public void addNotifyEventListener(@Nonnull final NotifyEventListener listener) {
    }

    @Override
    public void removeNotifyEventListener(@Nonnull final NotifyEventListener listener) {
    }

    @Override
    public int getDeviceListSize() {
        return 0;
    }

    @Nonnull
    @Override
    public List<Device> getDeviceList() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Device getDevice(@Nonnull final String udn) {
        return null;
    }

    @Override
    public void addPinnedDevice(@Nonnull final String location) {
    }

    @Override
    public void removePinnedDevice(@Nonnull final String location) {
    }
}
