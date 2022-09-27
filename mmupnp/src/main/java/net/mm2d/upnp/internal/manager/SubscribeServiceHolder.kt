/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import net.mm2d.upnp.ControlPointConfig
import net.mm2d.upnp.Service
import net.mm2d.upnp.internal.impl.launchServer
import java.util.concurrent.TimeUnit

/**
 * Class to manage the Service that became subscribed state.
 *
 * If specified, renew will be executed periodically so that the Subscription will not expire.
 * Also, expired services are deleted.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SubscribeServiceHolder(
    private val config: ControlPointConfig,
) {
    private val mutex = Mutex()
    private val flow: MutableSharedFlow<Unit> = MutableSharedFlow(extraBufferCapacity = 1)
    private val subscriptionMap = mutableMapOf<String, SubscribeService>()
    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = config.launchServer { block() }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    suspend fun add(service: Service, timeout: Long, keepRenew: Boolean): Unit = mutex.withLock {
        val id = service.subscriptionId
        if (id.isNullOrEmpty()) {
            return
        }
        subscriptionMap[id] = SubscribeService(service, timeout, keepRenew)
        flow.tryEmit(Unit)
    }

    suspend fun renew(service: Service, timeout: Long): Unit = mutex.withLock {
        subscriptionMap[service.subscriptionId]?.renew(timeout)
    }

    suspend fun setKeepRenew(service: Service, keep: Boolean): Unit = mutex.withLock {
        subscriptionMap[service.subscriptionId]?.setKeepRenew(keep)
        flow.tryEmit(Unit)
    }

    suspend fun remove(service: Service): Unit = mutex.withLock {
        subscriptionMap.remove(service.subscriptionId)?.let {
            flow.tryEmit(Unit)
        }
    }

    suspend fun getService(subscriptionId: String): Service? = mutex.withLock {
        subscriptionMap[subscriptionId]?.service
    }

    suspend fun clear(): Unit = mutex.withLock {
        subscriptionMap.values.forEach {
            it.service.unsubscribe()
        }
        subscriptionMap.clear()
    }

    private suspend fun CoroutineScope.block() {
        try {
            while (isActive) {
                renewSubscribe(waitEntry())
                removeExpiredService()
                yield()
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
    private suspend fun waitEntry(): Collection<SubscribeService> {
        while (mutex.withLock { subscriptionMap.isEmpty() }) {
            flow.first()
        }
        // 操作をロックしないようにコピーに対して処理を行う。
        return mutex.withLock { ArrayList(subscriptionMap.values) }
    }

    /**
     * Trigger renew on the argument [Service].
     *
     * This process does not exclude the whole because it includes network communication.
     * Therefore, the argument list is a collection that is not accessed by others.
     *
     * @param serviceList [Service] collection
     */
    private suspend fun renewSubscribe(serviceList: Collection<SubscribeService>) {
        serviceList.forEach {
            if (!it.renewSubscribe(System.currentTimeMillis()) && it.isFailed()) {
                remove(it.service)
            }
        }
    }

    /**
     * Remove expired [Service].
     */
    private suspend fun removeExpiredService(): Unit = mutex.withLock {
        val now = System.currentTimeMillis()
        subscriptionMap.values
            .filter { it.isExpired(now) }
            .map { it.service }
            .forEach {
                remove(it)
                it.unsubscribe()
            }
    }

    /**
     * Wait until the latest Renew execution time.
     *
     * @throws InterruptedException An interrupt occurred
     */
    @Throws(InterruptedException::class)
    private suspend fun waitNextRenewTime() {
        mutex.withLock {
            if (subscriptionMap.isEmpty()) {
                return
            }
        }
        val sleep = maxOf(findMostRecentTime() - System.currentTimeMillis(), MIN_INTERVAL)
        // ビジーループを回避するため最小値を設ける
        withTimeout(sleep) {
            flow.first()
        }
    }

    /**
     * Returns the closest one of the times to scan.
     *
     * @return Next time to scan
     */
    private fun findMostRecentTime(): Long =
        subscriptionMap.values.minByOrNull { it.getNextScanTime() }?.getNextScanTime() ?: 0L

    companion object {
        private val MIN_INTERVAL = TimeUnit.SECONDS.toMillis(1)
    }
}
