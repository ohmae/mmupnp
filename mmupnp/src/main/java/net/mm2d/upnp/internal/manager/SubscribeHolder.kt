/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager

import net.mm2d.upnp.Service
import net.mm2d.upnp.internal.thread.TaskExecutors
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max

/**
 * Subscribe状態となったServiceを管理するクラス。
 *
 * 指定すればSubscribeの期限が切れないように定期的にrenewを実行する。
 * また、期限が切れたサービスは削除される。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SubscribeHolder(
    private val taskExecutors: TaskExecutors
) : Runnable {
    private var futureTask: FutureTask<*>? = null
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val subscriptionMap = mutableMapOf<String, SubscribeService>()

    /**
     * 保持している[Service]すべてを含むListを返す。
     *
     * @return [Service]リスト
     */
    fun getServiceList(): List<Service> {
        lock.withLock {
            return subscriptionMap.values.map { it.getService() }
        }
    }

    /**
     * スレッドを開始する。
     */
    @Synchronized
    fun start() {
        FutureTask(this, null).also {
            futureTask = it
            taskExecutors.manager(it)
        }
    }

    /**
     * スレッドの停止を要求する。
     */
    @Synchronized
    fun stop() {
        futureTask?.cancel(false)
        futureTask = null
    }

    private fun isCanceled(): Boolean {
        return futureTask?.isCancelled ?: true
    }

    /**
     * Subscribeを開始した[Service]を登録する。
     *
     * @param service   登録する[Service]
     * @param keepRenew 期限が切れる前にrenewSubscribeを続ける場合true
     */
    fun add(service: Service, timeout: Long, keepRenew: Boolean) {
        lock.withLock {
            val id = service.subscriptionId
            if (id.isNullOrEmpty()) {
                return
            }
            subscriptionMap[id] = SubscribeService(service, timeout, keepRenew)
            condition.signalAll()
        }
    }

    fun renew(service: Service, timeout: Long) {
        lock.withLock {
            subscriptionMap[service.subscriptionId]
                ?.renew(timeout)
        }
    }

    fun setKeepRenew(service: Service, keep: Boolean) {
        lock.withLock {
            subscriptionMap[service.subscriptionId]
                ?.setKeepRenew(keep)
            condition.signalAll()
        }
    }

    /**
     * 指定したサービスを削除する。
     *
     * @param service 削除するサービス
     */
    fun remove(service: Service) {
        lock.withLock {
            subscriptionMap.remove(service.subscriptionId)?.let {
                condition.signalAll()
            }
        }
    }

    /**
     * Subscription IDに該当するServiceを返す。
     *
     * @param subscriptionId Subscription ID
     * @return 該当するService
     */
    fun getService(subscriptionId: String): Service? {
        lock.withLock {
            return subscriptionMap[subscriptionId]?.getService()
        }
    }

    /**
     * 保持しているServiceをすべて削除する。
     */
    fun clear() {
        lock.withLock {
            subscriptionMap.clear()
        }
    }

    override fun run() {
        Thread.currentThread().let {
            it.name = it.name + "-subscribe-holder"
        }
        try {
            while (!isCanceled()) {
                renewSubscribe(waitEntry())
                removeExpiredService()
                waitNextRenewTime()
            }
        } catch (ignored: InterruptedException) {
        }
    }

    /**
     * ServiceListに何らかのエントリーが追加されるまで待機する。
     *
     * 戻り値としてrenewのトリガをかけるServiceのコレクションを返す。
     * renew処理は排他を行わず実行するため、排他する必要が無いように、他に影響のないコピーを返す。
     *
     * @return renewのトリガをかけるServiceのコレクション
     * @throws InterruptedException 割り込みが発生した
     */
    @Throws(InterruptedException::class)
    private fun waitEntry(): Collection<SubscribeService> {
        lock.withLock {
            while (subscriptionMap.isEmpty()) {
                condition.await()
            }
            // 操作をロックしないようにコピーに対して処理を行う。
            return ArrayList(subscriptionMap.values)
        }
    }

    /**
     * 引数の[Service]郡に対し、renewのトリガをかける。
     *
     * この処理はネットワーク通信を含むため全体を排他しない。
     * その為引数となるリストは他からアクセスされないコレクションとする。
     *
     * @param serviceList renewのトリガをかける[Service]のコレクション
     */
    private fun renewSubscribe(serviceList: Collection<SubscribeService>) {
        serviceList.forEach {
            if (!it.renewSubscribe(System.currentTimeMillis()) && it.isFailed()) {
                remove(it.getService())
            }
        }
    }

    /**
     * 期限切れの[Service]を削除する。
     */
    private fun removeExpiredService() {
        lock.withLock {
            val now = System.currentTimeMillis()
            subscriptionMap.values
                .toList()
                .filter { it.isExpired(now) }
                .map { it.getService() }
                .forEach {
                    remove(it)
                    it.unsubscribeSync()
                }
        }
    }

    /**
     * 直近のRenew実行時間まで待機する。
     *
     * @throws InterruptedException 割り込みが発生した
     */
    @Throws(InterruptedException::class)
    private fun waitNextRenewTime() {
        lock.withLock {
            if (subscriptionMap.isEmpty()) {
                return
            }
            val sleep = max(findMostRecentTime() - System.currentTimeMillis(), MIN_INTERVAL)
            // ビジーループを回避するため最小値を設ける
            condition.await(sleep, TimeUnit.MILLISECONDS)
        }
    }

    /**
     * スキャンすべき時刻の内最も小さな時刻を返す。
     *
     * @return 直近のスキャン時刻
     */
    private fun findMostRecentTime(): Long {
        return subscriptionMap.values.minBy { it.getNextScanTime() }
            ?.getNextScanTime() ?: 0L
    }

    companion object {
        private val MIN_INTERVAL = TimeUnit.SECONDS.toMillis(1)
    }
}
