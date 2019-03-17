/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager;

import net.mm2d.log.Logger;
import net.mm2d.upnp.ControlPoint.NotifyEventListener;
import net.mm2d.upnp.Service;
import net.mm2d.upnp.StateVariable;
import net.mm2d.upnp.internal.impl.DiFactory;
import net.mm2d.upnp.internal.server.EventReceiver;
import net.mm2d.upnp.internal.server.EventReceiver.EventMessageListener;
import net.mm2d.upnp.internal.thread.TaskExecutors;
import net.mm2d.upnp.util.StringPair;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SubscribeManager implements EventMessageListener {
    @Nonnull
    private final TaskExecutors mTaskExecutors;
    @Nonnull
    private final NotifyEventListener mNotifyEventListener;
    @Nonnull
    private final SubscribeHolder mSubscribeHolder;
    @Nonnull
    private final EventReceiver mEventReceiver;

    public SubscribeManager(
            @Nonnull final TaskExecutors taskExecutors,
            @Nonnull final NotifyEventListener listener,
            @Nonnull final DiFactory factory) {
        mTaskExecutors = taskExecutors;
        mNotifyEventListener = listener;
        mSubscribeHolder = factory.createSubscribeHolder(taskExecutors);
        mEventReceiver = factory.createEventReceiver(taskExecutors, this);
    }

    @Override
    public boolean onEventReceived(
            @Nonnull final String sid,
            final long seq,
            @Nonnull final List<StringPair> properties) {
        Logger.d(() -> sid + " " + seq + " " + properties);
        final Service service = mSubscribeHolder.getService(sid);
        if (service == null) {
            Logger.e("service is null");
        }
        return service != null && mTaskExecutors.callback(() -> {
            for (final StringPair pair : properties) {
                notifyEvent(service, seq, pair.getKey(), pair.getValue());
            }
        });
    }

    private void notifyEvent(
            @Nonnull final Service service,
            final long seq,
            @Nullable final String name,
            @Nullable final String value) {
        final StateVariable variable = service.findStateVariable(name);
        if (variable == null || !variable.isSendEvents() || value == null) {
            Logger.w(() -> "illegal notify argument: " + name + " " + value);
            return;
        }
        mNotifyEventListener.onNotifyEvent(service, seq, variable.getName(), value);
    }

    public void initialize() {
        mSubscribeHolder.start();
    }

    public void start() {
        try {
            mEventReceiver.open();
        } catch (final IOException e) {
            Logger.w(e);
        }
    }

    public void stop() {
        final List<Service> serviceList = mSubscribeHolder.getServiceList();
        for (final Service service : serviceList) {
            mTaskExecutors.io(service::unsubscribeSync);
        }
        mSubscribeHolder.clear();
        mEventReceiver.close();
    }

    public void terminate() {
        mSubscribeHolder.shutdownRequest();
    }

    /**
     * イベント通知を受け取るポートを返す。
     *
     * @return イベント通知受信用ポート番号
     * @see EventReceiver
     */
    public int getEventPort() {
        return mEventReceiver.getLocalPort();
    }

    /**
     * SubscriptionIDに合致するServiceを返す。
     *
     * <p>合致するServiceがない場合null
     *
     * @param subscriptionId SubscriptionID
     * @return 該当Service
     * @see Service
     */
    @Nullable
    public Service getSubscribeService(@Nonnull final String subscriptionId) {
        return mSubscribeHolder.getService(subscriptionId);
    }

    /**
     * SubscriptionIDが確定したServiceを購読リストに登録する
     *
     * <p>Serviceのsubscribeが実行された後にServiceからコールされる。
     *
     * @param service 登録するService
     * @param timeout タイムアウトするまでの時間
     * @param keep    keep-aliveを行う場合true
     * @see Service
     * @see Service#subscribeSync()
     */
    public void register(
            @Nonnull final Service service,
            final long timeout,
            final boolean keep) {
        mSubscribeHolder.add(service, timeout, keep);
    }

    public void renew(
            @Nonnull final Service service,
            final long timeout) {
        mSubscribeHolder.renew(service, timeout);
    }

    public void setKeepRenew(
            @Nonnull final Service service,
            final boolean keep) {
        mSubscribeHolder.setKeepRenew(service, keep);
    }

    /**
     * 指定SubscriptionIDのサービスを購読リストから削除する。
     *
     * @param service 削除するService
     * @see Service
     * @see Service#unsubscribeSync()
     */
    public void unregister(@Nonnull final Service service) {
        mSubscribeHolder.remove(service);
    }
}
