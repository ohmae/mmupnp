/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.ControlPoint.NotifyEventListener;
import net.mm2d.upnp.DeviceHolder.ExpireListener;
import net.mm2d.upnp.EventReceiver.EventMessageListener;
import net.mm2d.upnp.SsdpNotifyReceiver.NotifyListener;
import net.mm2d.upnp.SsdpSearchServer.ResponseListener;

import java.net.NetworkInterface;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * ControlPointのテストを容易にするためのDependency injection
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
class ControlPointDiFactory {
    @Nonnull
    private final Protocol mProtocol;

    ControlPointDiFactory() {
        this(Protocol.DEFAULT);
    }

    ControlPointDiFactory(@Nonnull final Protocol protocol) {
        mProtocol = protocol;
    }

    @Nonnull
    Map<String, DeviceImpl.Builder> createLoadingDeviceMap() {
        return new HashMap<>();
    }

    @Nonnull
    DeviceHolder createDeviceHolder(@Nonnull final ExpireListener listener) {
        return new DeviceHolder(listener);
    }

    @Nonnull
    SsdpSearchServerList createSsdpSearchServerList(
            @Nonnull final Collection<NetworkInterface> interfaces,
            @Nonnull final ResponseListener listener) {
        return new SsdpSearchServerList().init(mProtocol, interfaces, listener);
    }

    @Nonnull
    SsdpNotifyReceiverList createSsdpNotifyReceiverList(
            @Nonnull final Collection<NetworkInterface> interfaces,
            @Nonnull final NotifyListener listener) {
        return new SsdpNotifyReceiverList().init(mProtocol, interfaces, listener);
    }

    @Nonnull
    SubscribeManager createSubscribeManager(
            @Nonnull final ThreadPool threadPool,
            @Nonnull final NotifyEventListener listener) {
        return new SubscribeManager(threadPool, listener, this);
    }

    @Nonnull
    SubscribeHolder createSubscribeHolder() {
        return new SubscribeHolder();
    }

    @Nonnull
    EventReceiver createEventReceiver(@Nonnull final EventMessageListener listener) {
        return new EventReceiver(listener);
    }
}
