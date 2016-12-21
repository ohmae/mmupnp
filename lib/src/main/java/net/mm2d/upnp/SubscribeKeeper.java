/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Subscribeの期限が切れないように定期的にrenewを実行するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class SubscribeKeeper implements Runnable {
    private static final String TAG = SubscribeKeeper.class.getSimpleName();
    private static final long MARGIN_TIME = TimeUnit.SECONDS.toMillis(10);
    private static final long MIN_INTERVAL = TimeUnit.SECONDS.toMillis(1);
    private static final int RETRY_COUNT = 2;
    private final ControlPoint mControlPoint;
    private volatile boolean mShutdownRequest = false;
    private Thread mThread;

    private static class Container {
        private final Service mService;
        private int mFailCount;

        Container(Service service) {
            mService = service;
            mFailCount = 0;
        }

        Service getService() {
            return mService;
        }

        int getFailCount() {
            return mFailCount;
        }

        void resetFailCount() {
            mFailCount = 0;
        }

        void increaseFailCount() {
            mFailCount++;
        }

        @Override
        public int hashCode() {
            return mService.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }
            if (!(object instanceof Container)) {
                return false;
            }
            Container c = (Container) object;
            return mService.equals(c.getService());
        }
    }

    private final List<Container> mServiceList;
    private final Comparator<Container> mComparator = new Comparator<Container>() {
        @Override
        public int compare(Container c1, Container c2) {
            return (int) (calculateRenewTime(c1) - calculateRenewTime(c2));
        }
    };

    /**
     * Renewを実行する時間(UTC[ms])を計算して返す。
     *
     * <p>Subscribeのtimeoutの半分の時間を基準に実行、
     * 実行に失敗した場合はtimeoutの時間を基準に実行し、
     * 1回までの通信失敗は許容する。
     *
     * <p>また、基準時間から一定時間引いた時間に実行することで
     * デバイスごとに時間が多少ずれていても動作できるようにする。
     *
     * @param c 調査するService
     * @return Renewを行う時間
     */
    private long calculateRenewTime(@Nonnull Container c) {
        final Service service = c.getService();
        long interval = service.getSubscriptionTimeout() * (c.getFailCount() + 1) / RETRY_COUNT;
        if (interval > MARGIN_TIME * 2) {
            interval -= MARGIN_TIME;
        } else {
            interval = interval / 2;
        }
        return service.getSubscriptionStart() + interval;
    }

    SubscribeKeeper(@Nonnull ControlPoint controlPoint) {
        mControlPoint = controlPoint;
        mServiceList = new ArrayList<>();
    }

    void start() {
        mShutdownRequest = false;
        mThread = new Thread(this, TAG);
        mThread.start();
    }

    void shutdownRequest() {
        mShutdownRequest = true;
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
    }

    synchronized void update() {
        Collections.sort(mServiceList, mComparator);
    }

    synchronized void add(@Nonnull Service service) {
        final Container container = new Container(service);
        if (!mServiceList.contains(container)) {
            mServiceList.add(container);
        }
        Collections.sort(mServiceList, mComparator);
        notifyAll();
    }

    synchronized void remove(@Nonnull Service service) {
        for (int i = 0; i < mServiceList.size(); i++) {
            if (mServiceList.get(i).getService().equals(service)) {
                mServiceList.remove(i);
                break;
            }
        }
    }

    synchronized void clear() {
        mServiceList.clear();
    }

    @Override
    public void run() {
        try {
            while (!mShutdownRequest) {
                renewSubscribe(waitListEntry());
                waitNextRenewTime();
            }
        } catch (final InterruptedException ignored) {
        }
    }

    /**
     * ServiceListに何らかのエントリーが追加されるまで待機する。
     *
     * @return ServiceListのコピー
     * @throws InterruptedException 割り込みが発生した
     */
    private synchronized List<Container> waitListEntry() throws InterruptedException {
        while (mServiceList.size() == 0) {
            wait();
        }
        // リスト操作をロックしないようにコピーに対して処理を行う。
        return new ArrayList<>(mServiceList);
    }

    /**
     * Renewタイムを超えたサービスについてRenewを実行する。
     *
     * @param serviceList Renew実行までの時間が短い順にソートされたサービスリスト
     */
    private void renewSubscribe(final List<Container> serviceList) {
        final long now = System.currentTimeMillis();
        for (final Container c : serviceList) {
            if (calculateRenewTime(c) > now) {
                break;
            }
            try {
                if (c.getService().renewSubscribe(false)) {
                    c.resetFailCount();
                } else {
                    c.increaseFailCount();
                }
            } catch (final IOException e) {
                Log.w(TAG, e);
                c.increaseFailCount();
            }
            if (c.getFailCount() >= RETRY_COUNT) {
                // 2回renewに失敗した場合はDeviceとの通信に問題ありとしてlost扱いにする
                mControlPoint.lostDevice(c.getService().getDevice());
            }
        }
        // 内部でunregisterされ、このクラスのremoveもコールされる。
        mControlPoint.removeExpiredSubscribeService();
    }

    /**
     * 直近のRenew実行時間まで待機する。
     *
     * @throws InterruptedException 割り込みが発生した
     */
    private synchronized void waitNextRenewTime() throws InterruptedException {
        Collections.sort(mServiceList, mComparator);
        if (mServiceList.size() != 0) {
            final Container c = mServiceList.get(0);
            long sleep = calculateRenewTime(c) - System.currentTimeMillis();
            if (sleep < MIN_INTERVAL) {
                sleep = MIN_INTERVAL; // ビジーループ阻止
            }
            wait(sleep);
        }
    }
}
