/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl;

import net.mm2d.upnp.ControlPoint.NotifyEventListener;
import net.mm2d.upnp.Protocol;
import net.mm2d.upnp.TaskExecutor;
import net.mm2d.upnp.internal.manager.DeviceHolder;
import net.mm2d.upnp.internal.manager.DeviceHolder.ExpireListener;
import net.mm2d.upnp.internal.manager.SubscribeHolder;
import net.mm2d.upnp.internal.manager.SubscribeManager;
import net.mm2d.upnp.internal.server.EventReceiver;
import net.mm2d.upnp.internal.server.EventReceiver.EventMessageListener;
import net.mm2d.upnp.internal.server.SsdpNotifyReceiver.NotifyListener;
import net.mm2d.upnp.internal.server.SsdpNotifyReceiverList;
import net.mm2d.upnp.internal.server.SsdpSearchServer.ResponseListener;
import net.mm2d.upnp.internal.server.SsdpSearchServerList;
import net.mm2d.upnp.internal.thread.TaskExecutors;

import java.net.NetworkInterface;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ControlPointのテストを容易にするためのDependency injection
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class DiFactory {
    @Nonnull
    private final Protocol mProtocol;
    @Nullable
    private final TaskExecutor mCallbackExecutor;

    public DiFactory() {
        this(Protocol.DEFAULT, null);
    }

    DiFactory(@Nonnull final Protocol protocol) {
        this(protocol, null);
    }

    public DiFactory(
            @Nonnull final Protocol protocol,
            @Nullable final TaskExecutor callback) {
        mProtocol = protocol;
        mCallbackExecutor = callback;
    }

    @Nonnull
    public Map<String, DeviceImpl.Builder> createLoadingDeviceMap() {
        return new HashMap<>();
    }

    @Nonnull
    public DeviceHolder createDeviceHolder(
            @Nonnull final TaskExecutors executors,
            @Nonnull final ExpireListener listener) {
        return new DeviceHolder(executors, listener);
    }

    @Nonnull
    public SsdpSearchServerList createSsdpSearchServerList(
            @Nonnull final TaskExecutors executors,
            @Nonnull final Iterable<NetworkInterface> interfaces,
            @Nonnull final ResponseListener listener) {
        return new SsdpSearchServerList().init(executors, mProtocol, interfaces, listener);
    }

    @Nonnull
    public SsdpNotifyReceiverList createSsdpNotifyReceiverList(
            @Nonnull final TaskExecutors executors,
            @Nonnull final Iterable<NetworkInterface> interfaces,
            @Nonnull final NotifyListener listener) {
        return new SsdpNotifyReceiverList().init(executors, mProtocol, interfaces, listener);
    }

    @Nonnull
    public SubscribeManager createSubscribeManager(
            @Nonnull final TaskExecutors taskExecutors,
            @Nonnull final NotifyEventListener listener) {
        return new SubscribeManager(taskExecutors, listener, this);
    }

    @Nonnull
    public SubscribeHolder createSubscribeHolder(
            @Nonnull final TaskExecutors executors) {
        return new SubscribeHolder(executors);
    }

    @Nonnull
    public EventReceiver createEventReceiver(
            @Nonnull final TaskExecutors taskExecutors,
            @Nonnull final EventMessageListener listener) {
        return new EventReceiver(taskExecutors, listener);
    }

    @Nonnull
    public TaskExecutors createTaskExecutors() {
        return new TaskExecutors(mCallbackExecutor);
    }
}
