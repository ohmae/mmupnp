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
class SubscribeKeeper extends Thread {
    private static final String TAG = SubscribeKeeper.class.getSimpleName();
    private static final long MARGIN_TIME = TimeUnit.SECONDS.toMillis(10);
    private static final long MIN_INTERVAL = TimeUnit.SECONDS.toMillis(1);
    private static final int RETRY_COUNT = 2;
    private final ControlPoint mControlPoint;
    private volatile boolean mShutdownRequest = false;

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

    public SubscribeKeeper(@Nonnull ControlPoint controlPoint) {
        super(TAG);
        mControlPoint = controlPoint;
        mServiceList = new ArrayList<>();
    }

    public void shutdownRequest() {
        mShutdownRequest = true;
        interrupt();
    }

    public synchronized void update() {
        Collections.sort(mServiceList, mComparator);
    }

    public synchronized void add(@Nonnull Service service) {
        mServiceList.add(new Container(service));
        Collections.sort(mServiceList, mComparator);
        notifyAll();
    }

    public synchronized void remove(@Nonnull Service service) {
        for (int i = 0; i < mServiceList.size(); i++) {
            if (mServiceList.get(i).getService().equals(service)) {
                mServiceList.remove(i);
                break;
            }
        }
    }

    public synchronized void clear() {
        mServiceList.clear();
    }

    @Override
    public void run() {
        try {
            while (!mShutdownRequest) {
                final List<Container> work;
                synchronized (this) {
                    while (mServiceList.size() == 0) {
                        wait();
                    }
                    // リスト操作をロックしないようにコピーに対して処理を行う。
                    work = new ArrayList<>(mServiceList);
                }
                final long now = System.currentTimeMillis();
                for (final Container c : work) {
                    if (calculateRenewTime(c) < now) {
                        try {
                            c.getService().renewSubscribe(false);
                            c.resetFailCount();
                        } catch (final IOException e) {
                            Log.w(TAG, e);
                            c.increaseFailCount();
                            if (c.getFailCount() >= RETRY_COUNT) {
                                // 2回renewに失敗した場合はDeviceとの通信に問題ありとしてlost扱いにする
                                mControlPoint.lostDevice(c.getService().getDevice());
                            }
                        }
                    } else {
                        break;
                    }
                }
                // 内部でunregisterされ、このクラスのremoveもコールされる。
                mControlPoint.removeExpiredSubscribeService();
                synchronized (this) {
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
        } catch (final InterruptedException ignored) {
        }
    }
}
