/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager;

import net.mm2d.upnp.Service;
import net.mm2d.upnp.internal.thread.TaskExecutors;
import net.mm2d.upnp.util.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Subscribe状態となったServiceを管理するクラス。
 *
 * <p>指定すればSubscribeの期限が切れないように定期的にrenewを実行する。
 * また、期限が切れたサービスは削除される。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class SubscribeHolder implements Runnable {
    private static final long MIN_INTERVAL = TimeUnit.SECONDS.toMillis(1);

    @Nonnull
    private final TaskExecutors mTaskExecutors;
    @Nullable
    private FutureTask<?> mFutureTask;
    @Nonnull
    private final Map<String, SubscribeService> mSubscriptionMap = new HashMap<>();

    public SubscribeHolder(@Nonnull final TaskExecutors executors) {
        mTaskExecutors = executors;
    }

    /**
     * スレッドを開始する。
     */
    void start() {
        mFutureTask = new FutureTask<>(this, null);
        mTaskExecutors.manager(mFutureTask);
    }

    /**
     * スレッドの停止を要求する。
     */
    void shutdownRequest() {
        if (mFutureTask != null) {
            mFutureTask.cancel(false);
            mFutureTask = null;
        }
    }

    /**
     * Subscribeを開始したServiceを登録する。
     *
     * @param service   登録するService
     * @param keepRenew 期限が切れる前にrenewSubscribeを続ける場合true
     */
    synchronized void add(
            @Nonnull final Service service,
            final long timeout,
            final boolean keepRenew) {
        final String id = service.getSubscriptionId();
        if (TextUtils.isEmpty(id)) {
            return;
        }
        final SubscribeService subscribeService = new SubscribeService(service, timeout, keepRenew);
        mSubscriptionMap.put(id, subscribeService);
        notifyAll();
    }

    synchronized void renew(
            @Nonnull final Service service,
            final long timeout) {
        final SubscribeService subscribing = mSubscriptionMap.get(service.getSubscriptionId());
        if (subscribing != null) {
            subscribing.renew(timeout);
        }
    }

    synchronized void setKeepRenew(
            @Nonnull final Service service,
            final boolean keep) {
        final SubscribeService subscribing = mSubscriptionMap.get(service.getSubscriptionId());
        if (subscribing != null) {
            subscribing.setKeepRenew(keep);
        }
        notifyAll();
    }

    /**
     * 指定したサービスを削除する。
     *
     * @param service 削除するサービス
     */
    synchronized void remove(@Nonnull final Service service) {
        if (mSubscriptionMap.remove(service.getSubscriptionId()) != null) {
            notifyAll();
        }
    }

    /**
     * 保持しているServiceすべてを含むListを返す。
     *
     * @return Serviceリスト
     */
    @Nonnull
    synchronized List<Service> getServiceList() {
        if (mSubscriptionMap.isEmpty()) {
            return Collections.emptyList();
        }
        final List<Service> list = new ArrayList<>();
        for (final SubscribeService subscribeService : mSubscriptionMap.values()) {
            list.add(subscribeService.getService());
        }
        return list;
    }

    /**
     * Subscription IDに該当するServiceを返す。
     *
     * @param subscriptionId Subscription ID
     * @return 該当するService
     */
    @Nullable
    synchronized Service getService(@Nonnull final String subscriptionId) {
        final SubscribeService c = mSubscriptionMap.get(subscriptionId);
        if (c == null) {
            return null;
        }
        return c.getService();
    }

    /**
     * 保持しているServiceをすべて削除する。
     */
    synchronized void clear() {
        mSubscriptionMap.clear();
    }

    @Override
    public void run() {
        final Thread thread = Thread.currentThread();
        thread.setName(thread.getName() + "-subscribe-holder");
        try {
            while (mFutureTask != null && !mFutureTask.isCancelled()) {
                renewSubscribe(waitEntry());
                removeExpiredService();
                waitNextRenewTime();
            }
        } catch (final InterruptedException ignored) {
        }
    }

    /**
     * ServiceListに何らかのエントリーが追加されるまで待機する。
     *
     * <p>戻り値としてrenewのトリガをかけるServiceのコレクションを返す。
     * renew処理は排他を行わず実行するため、排他する必要が無いように、他に影響のないコピーを返す。
     *
     * @return renewのトリガをかけるServiceのコレクション
     * @throws InterruptedException 割り込みが発生した
     */
    @Nonnull
    private synchronized Collection<SubscribeService> waitEntry() throws InterruptedException {
        while (mSubscriptionMap.size() == 0) {
            wait();
        }
        // 操作をロックしないようにコピーに対して処理を行う。
        return new ArrayList<>(mSubscriptionMap.values());
    }

    /**
     * 引数のService郡に対し、renewのトリガをかける。
     *
     * <p>この処理はネットワーク通信を含むため全体を排他しない。
     * その為引数となるリストは他からアクセスされないコレクションとする。
     *
     * @param serviceList renewのトリガをかけるServiceのコレクション
     */
    private void renewSubscribe(@Nonnull final Collection<SubscribeService> serviceList) {
        for (final SubscribeService s : serviceList) {
            if (!s.renewSubscribe(System.currentTimeMillis()) && s.isFailed()) {
                remove(s.getService());
            }
        }
    }

    /**
     * 期限切れのServiceを削除する。
     */
    private synchronized void removeExpiredService() {
        final long now = System.currentTimeMillis();
        final List<SubscribeService> list = new ArrayList<>(mSubscriptionMap.values());
        for (final SubscribeService s : list) {
            if (s.isExpired(now)) {
                final Service service = s.getService();
                remove(service);
                service.unsubscribeSync();
            }
        }
    }

    /**
     * 直近のRenew実行時間まで待機する。
     *
     * @throws InterruptedException 割り込みが発生した
     */
    private synchronized void waitNextRenewTime() throws InterruptedException {
        if (mSubscriptionMap.size() == 0) {
            return;
        }
        final long sleep = findMostRecentTime() - System.currentTimeMillis();
        wait(Math.max(sleep, MIN_INTERVAL));// ビジーループを回避するため最小値を設ける
    }

    /**
     * スキャンすべき時刻の内最も小さな時刻を返す。
     *
     * @return 直近のスキャン時刻
     */
    private long findMostRecentTime() {
        long recent = Long.MAX_VALUE;
        for (final SubscribeService s : mSubscriptionMap.values()) {
            final long wait = s.getNextScanTime();
            if (recent > wait) {
                recent = wait;
            }
        }
        return recent;
    }
}
