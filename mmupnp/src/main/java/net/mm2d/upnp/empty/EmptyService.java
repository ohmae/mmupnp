/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty;

import net.mm2d.upnp.Action;
import net.mm2d.upnp.ControlPoints;
import net.mm2d.upnp.Device;
import net.mm2d.upnp.Service;
import net.mm2d.upnp.StateVariable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EmptyService implements Service {
    @Nonnull
    @Override
    public Device getDevice() {
        return ControlPoints.emptyDevice();
    }

    @Nonnull
    @Override
    public String getServiceType() {
        return "";
    }

    @Nonnull
    @Override
    public String getServiceId() {
        return "";
    }

    @Nonnull
    @Override
    public String getScpdUrl() {
        return "";
    }

    @Nonnull
    @Override
    public String getControlUrl() {
        return "";
    }

    @Nonnull
    @Override
    public String getEventSubUrl() {
        return "";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "";
    }

    @Nonnull
    @Override
    public List<Action> getActionList() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Action findAction(@Nonnull final String name) {
        return null;
    }

    @Nonnull
    @Override
    public List<StateVariable> getStateVariableList() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public StateVariable findStateVariable(@Nullable final String name) {
        return null;
    }

    @Override
    public boolean subscribeSync() {
        return false;
    }

    @Override
    public boolean subscribeSync(final boolean keepRenew) {
        return false;
    }

    @Override
    public boolean renewSubscribeSync() {
        return false;
    }

    @Override
    public boolean unsubscribeSync() {
        return false;
    }

    @Nullable
    @Override
    public String getSubscriptionId() {
        return null;
    }
}
