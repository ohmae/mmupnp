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

/**
 * EmptyObjectを提供する。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public final class ControlPoints {
    @Nonnull
    private static final ControlPoint EMPTY_CONTROL_POINT = new EmptyControlPoint();

    /**
     * ControlPointのEmpty実装を返す。
     *
     * @return ControlPointのEmpty実装
     */
    @Nonnull
    public static ControlPoint emptyControlPoint() {
        return EMPTY_CONTROL_POINT;
    }

    @Nonnull
    private static final Device EMPTY_DEVICE = new EmptyDevice();

    /**
     * DeviceのEmpty実装を返す。
     *
     * @return DeviceのEmpty実装
     */
    @Nonnull
    public static Device emptyDevice() {
        return EMPTY_DEVICE;
    }

    @Nonnull
    private static final Service EMPTY_SERVICE = new EmptyService();

    /**
     * ServiceのEmpty実装を返す。
     *
     * @return ServiceのEmpty実装
     */
    @Nonnull
    public static Service emptyService() {
        return EMPTY_SERVICE;
    }

    @Nonnull
    private static final Action EMPTY_ACTION = new EmptyAction();

    /**
     * ActionのEmpty実装を返す。
     *
     * @return ActionのEmpty実装
     */
    @Nonnull
    public static Action emptyAction() {
        return EMPTY_ACTION;
    }

    @Nonnull
    private static final SsdpMessage EMPTY_SSDP_MESSAGE = new EmptySsdpMessage();

    /**
     * SsdpMessageのEmpty実装を返す。
     *
     * @return SsdpMessageのEmpty実装
     */
    @Nonnull
    public static SsdpMessage emptySsdpMessage() {
        return EMPTY_SSDP_MESSAGE;
    }
}
