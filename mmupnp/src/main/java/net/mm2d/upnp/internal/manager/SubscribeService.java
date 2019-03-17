/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager;

import net.mm2d.upnp.Service;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * ServiceのSubscribe状態を管理するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
class SubscribeService {
    private static final long MARGIN_TIME = TimeUnit.SECONDS.toMillis(10);
    private static final int RETRY_COUNT = 2;
    @Nonnull
    private final Service mService;
    private boolean mKeepRenew;
    private int mFailCount;
    private long mSubscriptionStart;
    private long mSubscriptionTimeout;
    private long mSubscriptionExpiryTime;

    /**
     * Serviceを指定し初期化する。
     *
     * @param service   Service
     * @param timeout   タイムアウトするまでの時間
     * @param keepRenew 定期的にrenewを実行する場合true
     */
    SubscribeService(
            @Nonnull final Service service,
            final long timeout,
            final boolean keepRenew) {
        mService = service;
        mSubscriptionStart = System.currentTimeMillis();
        mSubscriptionTimeout = timeout;
        mSubscriptionExpiryTime = mSubscriptionStart + mSubscriptionTimeout;
        mKeepRenew = keepRenew;
        mFailCount = 0;
    }

    void renew(final long timeout) {
        mSubscriptionStart = System.currentTimeMillis();
        mSubscriptionTimeout = timeout;
        mSubscriptionExpiryTime = mSubscriptionStart + mSubscriptionTimeout;
    }

    void setKeepRenew(final boolean keep) {
        mKeepRenew = keep;
    }

    /**
     * Serviceを返す。
     *
     * @return Service
     */
    @Nonnull
    Service getService() {
        return mService;
    }

    /**
     * リトライ回数の上限を超えて失敗した状態かを返す。
     *
     * @return リトライ回数の上限をこえている場合true
     */
    boolean isFailed() {
        return mFailCount >= RETRY_COUNT;
    }

    /**
     * 次にスキャンする時刻を返す。
     *
     * <p>keep指定している場合は次にRenewを実行する時刻、
     * そうでない場合は、Expireする時刻
     *
     * @return スキャンする時刻
     */
    long getNextScanTime() {
        if (!mKeepRenew) {
            return mSubscriptionExpiryTime;
        }
        return calculateRenewTime();
    }


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
     * @return Renewを行う時間
     */
    // VisibleForTesting
    long calculateRenewTime() {
        long interval = mSubscriptionTimeout * (mFailCount + 1) / RETRY_COUNT;
        if (interval > MARGIN_TIME * 2) {
            interval -= MARGIN_TIME;
        } else {
            interval = interval / 2;
        }
        return mSubscriptionStart + interval;
    }

    /**
     * 有効期限が切れているかを確認する
     *
     * @param now 現在時刻
     * @return 有効期限切れの場合true
     */
    boolean isExpired(final long now) {
        return mSubscriptionExpiryTime < now;
    }

    /**
     * 条件であればSubscribeを実行する。
     *
     * @param now 現在時刻
     * @return 実行に失敗した場合false
     */
    boolean renewSubscribe(final long now) {
        if (!mKeepRenew || calculateRenewTime() > now) {
            return true;
        }
        if (mService.renewSubscribeSync()) {
            resetFailCount();
            return true;
        } else {
            increaseFailCount();
        }
        return false;
    }

    private void resetFailCount() {
        mFailCount = 0;
    }

    private void increaseFailCount() {
        mFailCount++;
    }

    @Override
    public int hashCode() {
        return mService.hashCode();
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof SubscribeService)) {
            return false;
        }
        final SubscribeService s = (SubscribeService) object;
        return mService.equals(s.getService());
    }
}
