/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Subscribeの期限が切れないように定期的にrenewを実行するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class SubscribeKeeper implements Runnable {
    private static final String TAG = SubscribeKeeper.class.getSimpleName();
    private static final long MIN_INTERVAL = TimeUnit.SECONDS.toMillis(1);

    private final Map<String, SubscribeService> mServiceMap;

    private Thread mThread;
    private volatile boolean mShutdownRequest = false;

    SubscribeKeeper() {
        mServiceMap = new HashMap<>();
    }

    /**
     * スレッドを開始する。
     */
    void start() {
        mShutdownRequest = false;
        mThread = new Thread(this, TAG);
        mThread.start();
    }

    /**
     * スレッドの停止を要求する。
     */
    void shutdownRequest() {
        mShutdownRequest = true;
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
    }

    /**
     * Subscribeを開始したServiceを登録する。
     *
     * @param service   登録するService
     * @param keepRenew 期限が切れる前にrenewSubscribeを続ける場合true
     */
    synchronized void add(@Nonnull Service service, boolean keepRenew) {
        if (service.getSubscriptionId() == null) {
            return;
        }
        final SubscribeService subscribeService = new SubscribeService(service, keepRenew);
        mServiceMap.put(service.getSubscriptionId(), subscribeService);
        notifyAll();
    }

    /**
     * 指定したサービスを削除する。
     *
     * @param service 削除するサービス
     */
    synchronized void remove(@Nonnull Service service) {
        mServiceMap.remove(service.getSubscriptionId());
        notifyAll();
    }

    /**
     * 保持しているServiceすべてを含むListを返す。
     *
     * @return Serviceリスト
     */
    @Nonnull
    synchronized List<Service> getServiceList() {
        final List<Service> list = new ArrayList<>(mServiceMap.size());
        for (Map.Entry<String, SubscribeService> entry : mServiceMap.entrySet()) {
            list.add(entry.getValue().getService());
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
    Service getService(final @Nonnull String subscriptionId) {
        SubscribeService c = mServiceMap.get(subscriptionId);
        if (c == null) {
            return null;
        }
        return c.getService();
    }

    /**
     * 保持しているServiceをすべて削除する。
     */
    synchronized void clear() {
        mServiceMap.clear();
    }

    @Override
    public void run() {
        try {
            while (!mShutdownRequest) {
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
        while (mServiceMap.size() == 0) {
            wait();
        }
        // 操作をロックしないようにコピーに対して処理を行う。
        return new ArrayList<>(mServiceMap.values());
    }

    /**
     * 引数のService郡に対し、renewのトリガをかける。
     *
     * <p>この処理はネットワーク通信を含むため全体を排他しない。
     * その為引数となるリストは他からアクセスされないコレクションとする。
     *
     * @param serviceList renewのトリガをかけるServiceのコレクション
     */
    private void renewSubscribe(final @Nonnull Collection<SubscribeService> serviceList) {
        for (final SubscribeService c : serviceList) {
            if (!c.renewSubscribe(System.currentTimeMillis()) && c.isFailed()) {
                remove(c.getService());
            }
        }
    }

    /**
     * 期限切れのServiceを削除する。
     */
    private synchronized void removeExpiredService() {
        final long now = System.currentTimeMillis();
        List<SubscribeService> list = new ArrayList<>(mServiceMap.values());
        for (SubscribeService c : list) {
            if (c.isExpired(now)) {
                final Service service = c.getService();
                remove(service);
                service.expired();
            }
        }
    }

    /**
     * 直近のRenew実行時間まで待機する。
     *
     * @throws InterruptedException 割り込みが発生した
     */
    private synchronized void waitNextRenewTime() throws InterruptedException {
        if (mServiceMap.size() == 0) {
            return;
        }
        long recent = Long.MAX_VALUE;
        for (SubscribeService c : mServiceMap.values()) {
            final long wait = c.getNextTime();
            if (recent > wait) {
                recent = wait;
            }
        }
        long sleep = recent - System.currentTimeMillis();
        if (sleep < MIN_INTERVAL) { // ビジーループを回避するためMIN_INTERVALの間はwaitする
            sleep = MIN_INTERVAL;
        }
        wait(sleep);
    }
}
