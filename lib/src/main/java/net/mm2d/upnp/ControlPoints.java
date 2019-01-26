/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.empty.EmptyAction;
import net.mm2d.upnp.empty.EmptyControlPoint;
import net.mm2d.upnp.empty.EmptyDevice;
import net.mm2d.upnp.empty.EmptyService;
import net.mm2d.upnp.empty.EmptySsdpMessage;

import javax.annotation.Nonnull;

public final class ControlPoints {
    private ControlPoints() {
    }

    @Nonnull
    private static final ControlPoint EMPTY_CONTROL_POINT = new EmptyControlPoint();

    @Nonnull
    public static ControlPoint emptyControlPoint() {
        return EMPTY_CONTROL_POINT;
    }

    @Nonnull
    private static final Device EMPTY_DEVICE = new EmptyDevice();

    @Nonnull
    public static Device emptyDevice() {
        return EMPTY_DEVICE;
    }

    @Nonnull
    private static final Service EMPTY_SERVICE = new EmptyService();

    @Nonnull
    public static Service emptyService() {
        return EMPTY_SERVICE;
    }

    @Nonnull
    private static final Action EMPTY_ACTION = new EmptyAction();

    @Nonnull
    public static Action emptyAction() {
        return EMPTY_ACTION;
    }

    @Nonnull
    private static final SsdpMessage EMPTY_SSDP_MESSAGE = new EmptySsdpMessage();

    @Nonnull
    public static SsdpMessage emptySsdpMessage() {
        return EMPTY_SSDP_MESSAGE;
    }
}
