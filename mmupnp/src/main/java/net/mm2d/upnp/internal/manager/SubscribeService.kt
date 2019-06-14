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
 * Class that manages Subscribe status of [Service].
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 *
 * @constructor initialize
 * @param service [Service]
 * @param subscriptionTimeout Time to time out
 * @param keepRenew true: periodically execute renew
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
     * Returns whether the status has exceeded the upper limit of the number of retries.
     *
     * @return true: failed, false: otherwise
     */
    fun isFailed(): Boolean {
        return failCount >= RETRY_COUNT
    }

    /**
     * Returns the next scan time.
     *
     * The time to execute Renew next if keep is specified,
     * otherwise the time to expire
     *
     * @return The next scan time
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
     * Calculate and return the time to execute Renew (UTC(ms)).
     *
     * Execute on the basis of Halftime of Subscribe's timeout.
     * If execution is unsuccessful, execute on the basis of Timeout's time.
     * Allow up to one communication failure.
     *
     * Also, by executing slightly before the reference time,
     * it is possible to operate even if the time is slightly offset for each device.
     *
     * @return Time to do Renew
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
     * Return whether it is expired.
     *
     * @param now Current time
     * @return true: expired, false: otherwise
     */
    fun isExpired(now: Long): Boolean {
        return subscriptionExpiryTime < now
    }

    /**
     * Execute renewSubscribe if there is a condition.
     *
     * @param now Current time
     * @return false: failed, true: otherwise
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

    override fun hashCode(): Int = service.hashCode()

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
