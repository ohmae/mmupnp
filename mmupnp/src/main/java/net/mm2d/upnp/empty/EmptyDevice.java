/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty;

import net.mm2d.upnp.Action;
import net.mm2d.upnp.ControlPoint;
import net.mm2d.upnp.ControlPoints;
import net.mm2d.upnp.Device;
import net.mm2d.upnp.HttpClient;
import net.mm2d.upnp.Icon;
import net.mm2d.upnp.IconFilter;
import net.mm2d.upnp.Service;
import net.mm2d.upnp.SsdpMessage;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EmptyDevice implements Device {
    @Override
    public void loadIconBinary(
            @Nonnull final HttpClient client,
            @Nonnull final IconFilter filter) {
    }

    @Nonnull
    @Override
    public ControlPoint getControlPoint() {
        return ControlPoints.emptyControlPoint();
    }

    @Override
    public void updateSsdpMessage(@Nonnull final SsdpMessage message) {
    }

    @Nonnull
    @Override
    public SsdpMessage getSsdpMessage() {
        return ControlPoints.emptySsdpMessage();
    }

    @Override
    public long getExpireTime() {
        return 0L;
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public int getScopeId() {
        return 0;
    }

    @Nullable
    @Override
    public String getValue(@Nonnull final String name) {
        return null;
    }

    @Nullable
    @Override
    public String getValueWithNamespace(
            @Nonnull final String namespace,
            @Nonnull final String name) {
        return null;
    }

    @Nonnull
    @Override
    public String getLocation() {
        return "";
    }

    @Nonnull
    @Override
    public String getBaseUrl() {
        return "";
    }

    @Nonnull
    @Override
    public String getIpAddress() {
        return "";
    }

    @Nonnull
    @Override
    public String getUdn() {
        return "";
    }

    @Nullable
    @Override
    public String getUpc() {
        return null;
    }

    @Nonnull
    @Override
    public String getDeviceType() {
        return "";
    }

    @Nonnull
    @Override
    public String getFriendlyName() {
        return "";
    }

    @Nullable
    @Override
    public String getManufacture() {
        return null;
    }

    @Nullable
    @Override
    public String getManufactureUrl() {
        return null;
    }

    @Nonnull
    @Override
    public String getModelName() {
        return "";
    }

    @Nullable
    @Override
    public String getModelUrl() {
        return null;
    }

    @Nullable
    @Override
    public String getModelDescription() {
        return null;
    }

    @Nullable
    @Override
    public String getModelNumber() {
        return null;
    }

    @Nullable
    @Override
    public String getSerialNumber() {
        return null;
    }

    @Nullable
    @Override
    public String getPresentationUrl() {
        return null;
    }

    @Nonnull
    @Override
    public List<Icon> getIconList() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<Service> getServiceList() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Service findServiceById(@Nonnull final String id) {
        return null;
    }

    @Nullable
    @Override
    public Service findServiceByType(@Nonnull final String type) {
        return null;
    }

    @Nullable
    @Override
    public Action findAction(@Nonnull final String name) {
        return null;
    }

    @Override
    public boolean isEmbeddedDevice() {
        return false;
    }

    @Nullable
    @Override
    public Device getParent() {
        return null;
    }

    @Nonnull
    @Override
    public List<Device> getDeviceList() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Device findDeviceByType(@Nonnull final String deviceType) {
        return null;
    }

    @Nullable
    @Override
    public Device findDeviceByTypeRecursively(@Nonnull final String deviceType) {
        return null;
    }

    @Override
    public boolean isPinned() {
        return false;
    }
}
