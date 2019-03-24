/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager

import net.mm2d.upnp.Service
import java.util.concurrent.TimeUnit

/**
 * [Service]のSubscribe状態を管理するクラス。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 * @param service             [Service]
 * @param subscriptionTimeout タイムアウトするまでの時間
 * @param keepRenew           定期的にrenewを実行する場合true
 */
internal class SubscribeService(
    private val service: Service,
    private var subscriptionTimeout: Long,
    private var keepRenew: Boolean
) {
    private var failCount: Int = 0
    private var subscriptionStart: Long = System.currentTimeMillis()
    private var subscriptionExpiryTime: Long = subscriptionStart + subscriptionTimeout

    fun getService(): Service {
        return service
    }

    /**
     * リトライ回数の上限を超えて失敗した状態かを返す。
     *
     * @return リトライ回数の上限をこえている場合true
     */
    fun isFailed(): Boolean {
        return failCount >= RETRY_COUNT
    }

    /**
     * 次にスキャンする時刻を返す。
     *
     * keep指定している場合は次にRenewを実行する時刻、
     * そうでない場合は、Expireする時刻
     *
     * @return スキャンする時刻
     */
    fun getNextScanTime(): Long {
        return if (!keepRenew) subscriptionExpiryTime else calculateRenewTime()
    }

    fun renew(timeout: Long) {
        subscriptionStart = System.currentTimeMillis()
        subscriptionTimeout = timeout
        subscriptionExpiryTime = subscriptionStart + subscriptionTimeout
    }

    fun setKeepRenew(keep: Boolean) {
        keepRenew = keep
    }

    /**
     * Renewを実行する時間(UTC(ms))を計算して返す。
     *
     * Subscribeのtimeoutの半分の時間を基準に実行、
     * 実行に失敗した場合はtimeoutの時間を基準に実行し、
     * 1回までの通信失敗は許容する。
     *
     * また、基準時間から一定時間引いた時間に実行することで
     * デバイスごとに時間が多少ずれていても動作できるようにする。
     *
     * @return Renewを行う時間
     */
    fun calculateRenewTime(): Long {
        var interval = subscriptionTimeout * (failCount + 1) / RETRY_COUNT
        if (interval > MARGIN_TIME * 2) {
            interval -= MARGIN_TIME
        } else {
            interval /= 2
        }
        return subscriptionStart + interval
    }

    /**
     * 有効期限が切れているかを確認する
     *
     * @param now 現在時刻
     * @return 有効期限切れの場合true
     */
    fun isExpired(now: Long): Boolean {
        return subscriptionExpiryTime < now
    }

    /**
     * 条件であればSubscribeを実行する。
     *
     * @param now 現在時刻
     * @return 実行に失敗した場合false
     */
    fun renewSubscribe(now: Long): Boolean {
        if (!keepRenew || calculateRenewTime() > now) {
            return true
        }
        if (service.renewSubscribeSync()) {
            resetFailCount()
            return true
        } else {
            increaseFailCount()
        }
        return false
    }

    private fun resetFailCount() {
        failCount = 0
    }

    private fun increaseFailCount() {
        failCount++
    }

    override fun hashCode(): Int {
        return service.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is SubscribeService) return false
        return service == other.service
    }

    companion object {
        private val MARGIN_TIME = TimeUnit.SECONDS.toMillis(10)
        private const val RETRY_COUNT = 2
    }
}
