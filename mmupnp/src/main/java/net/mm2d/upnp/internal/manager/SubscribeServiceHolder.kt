/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager

import net.mm2d.upnp.Service
import net.mm2d.upnp.internal.thread.TaskExecutors
import net.mm2d.upnp.internal.thread.ThreadCondition
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Class to manage the Service that became subscribed state.
 *
 * If specified, renew will be executed periodically so that the Subscription will not expire.
 * Also, expired services are deleted.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SubscribeServiceHolder(
    taskExecutors: TaskExecutors
) : Runnable {
    private val threadCondition = ThreadCondition(taskExecutors.manager)
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val subscriptionMap = mutableMapOf<String, SubscribeService>()

    fun start(): Unit = threadCondition.start(this)
    fun stop(): Unit = threadCondition.stop()

    fun add(service: Service, timeout: Long, keepRenew: Boolean): Unit = lock.withLock {
        val id = service.subscriptionId
        if (id.isNullOrEmpty()) {
            return
        }
        subscriptionMap[id] = SubscribeService(service, timeout, keepRenew)
        condition.signalAll()
    }

    fun renew(service: Service, timeout: Long): Unit = lock.withLock {
        subscriptionMap[service.subscriptionId]?.renew(timeout)
    }

    fun setKeepRenew(service: Service, keep: Boolean): Unit = lock.withLock {
        subscriptionMap[service.subscriptionId]?.setKeepRenew(keep)
        condition.signalAll()
    }

    fun remove(service: Service): Unit = lock.withLock {
        subscriptionMap.remove(service.subscriptionId)?.let {
            condition.signalAll()
        }
    }

    fun getService(subscriptionId: String): Service? = lock.withLock {
        subscriptionMap[subscriptionId]?.service
    }

    fun clear(): Unit = lock.withLock {
        subscriptionMap.values.forEach {
            it.service.unsubscribe()
        }
        subscriptionMap.clear()
    }

    override fun run() {
        Thread.currentThread().let {
            it.name = it.name + "-subscribe-holder"
        }
        try {
            while (!threadCondition.isCanceled()) {
                renewSubscribe(waitEntry())
                removeExpiredService()
                waitNextRenewTime()
            }
        } catch (ignored: InterruptedException) {
        }
    }

    /**
     * Wait until some entry is added to ServiceList.
     *
     * Returns a collection of Service that triggers renew as return value.
     * Since the renew process is executed without exclusion,
     * it returns a copy which has no other effect so that there is no need for exclusion.
     *
     * @return collection of Service that triggers renew
     * @throws InterruptedException An interrupt occurred
     */
    @Throws(InterruptedException::class)
    private fun waitEntry(): Collection<SubscribeService> = lock.withLock {
        while (subscriptionMap.isEmpty()) {
            condition.await()
        }
        // 操作をロックしないようにコピーに対して処理を行う。
        ArrayList(subscriptionMap.values)
    }

    /**
     * Trigger renew on the argument [Service].
     *
     * This process does not exclude the whole because it includes network communication.
     * Therefore, the argument list is a collection that is not accessed by others.
     *
     * @param serviceList [Service] collection
     */
    private fun renewSubscribe(serviceList: Collection<SubscribeService>) {
        serviceList.forEach {
            if (!it.renewSubscribe(System.currentTimeMillis()) && it.isFailed()) {
                remove(it.service)
            }
        }
    }

    /**
     * Remove expired [Service].
     */
    private fun removeExpiredService(): Unit = lock.withLock {
        val now = System.currentTimeMillis()
        subscriptionMap.values
            .filter { it.isExpired(now) }
            .map { it.service }
            .forEach {
                remove(it)
                it.unsubscribeSync()
            }
    }

    /**
     * Wait until the latest Renew execution time.
     *
     * @throws InterruptedException An interrupt occurred
     */
    @Throws(InterruptedException::class)
    private fun waitNextRenewTime(): Unit = lock.withLock {
        if (subscriptionMap.isEmpty()) {
            return
        }
        val sleep = maxOf(findMostRecentTime() - System.currentTimeMillis(), MIN_INTERVAL)
        // ビジーループを回避するため最小値を設ける
        condition.await(sleep, TimeUnit.MILLISECONDS)
    }

    /**
     * Returns the closest one of the times to scan.
     *
     * @return Next time to scan
     */
    private fun findMostRecentTime(): Long =
        subscriptionMap.values.minBy { it.getNextScanTime() }?.getNextScanTime() ?: 0L

    companion object {
        private val MIN_INTERVAL = TimeUnit.SECONDS.toMillis(1)
    }
}
