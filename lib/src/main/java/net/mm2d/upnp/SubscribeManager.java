/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.log.Log;
import net.mm2d.upnp.ControlPoint.NotifyEventListener;
import net.mm2d.upnp.EventReceiver.EventMessageListener;
import net.mm2d.util.StringPair;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class SubscribeManager implements EventMessageListener {
    @Nonnull
    private final ThreadPool mThreadPool;
    @Nonnull
    private final NotifyEventListener mNotifyEventListener;
    @Nonnull
    private final SubscribeHolder mSubscribeHolder;
    @Nonnull
    private final EventReceiver mEventReceiver;

    SubscribeManager(
            @Nonnull final ThreadPool threadPool,
            @Nonnull final NotifyEventListener listener,
            @Nonnull final DiFactory factory) {
        mThreadPool = threadPool;
        mNotifyEventListener = listener;
        mSubscribeHolder = factory.createSubscribeHolder();
        mEventReceiver = factory.createEventReceiver(this);
    }

    @Override
    public boolean onEventReceived(
            @Nonnull final String sid,
            final long seq,
            @Nonnull final List<StringPair> properties) {
        final Service service = mSubscribeHolder.getService(sid);
        return service != null && mThreadPool.executeInSequential(() -> {
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
            Log.w("illegal notify argument:" + name + " " + value);
            return;
        }
        mNotifyEventListener.onNotifyEvent(service, seq, variable.getName(), value);
    }

    void initialize() {
        mSubscribeHolder.start();
    }

    void start() {
        try {
            mEventReceiver.open();
        } catch (final IOException e) {
            Log.w(e);
        }
    }

    void stop() {
        final List<Service> serviceList = mSubscribeHolder.getServiceList();
        for (final Service service : serviceList) {
            mThreadPool.executeInParallel(() -> {
                try {
                    service.unsubscribe();
                } catch (final IOException e) {
                    Log.w(e);
                }
            });
        }
        mSubscribeHolder.clear();
        mEventReceiver.close();
    }

    void terminate() {
        mSubscribeHolder.shutdownRequest();
    }

    /**
     * イベント通知を受け取るポートを返す。
     *
     * @return イベント通知受信用ポート番号
     * @see EventReceiver
     */
    int getEventPort() {
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
    Service getSubscribeService(@Nonnull final String subscriptionId) {
        return mSubscribeHolder.getService(subscriptionId);
    }

    /**
     * SubscriptionIDが確定したServiceを購読リストに登録する
     *
     * <p>Serviceのsubscribeが実行された後にServiceからコールされる。
     *
     * @param service 登録するService
     * @see Service
     * @see Service#subscribe()
     */
    void registerSubscribeService(
            @Nonnull final Service service,
            final boolean keep) {
        mSubscribeHolder.add(service, keep);
    }

    /**
     * 指定SubscriptionIDのサービスを購読リストから削除する。
     *
     * @param service 削除するService
     * @see Service
     * @see Service#unsubscribe()
     */
    void unregisterSubscribeService(@Nonnull final Service service) {
        mSubscribeHolder.remove(service);
    }
}
