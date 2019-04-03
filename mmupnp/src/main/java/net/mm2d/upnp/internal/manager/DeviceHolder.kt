/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager

import net.mm2d.upnp.Device
import net.mm2d.upnp.internal.thread.TaskExecutors
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max

/**
 * [net.mm2d.upnp.ControlPoint]で発見した[Device]を保持するクラス。
 *
 * Deviceの有効期限を確認し、有効期限が切れたDeviceをLostとして通知する。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 *
 * @param expireListener 期限切れの通知を受け取るリスナー
 */
internal class DeviceHolder(
    private val taskExecutors: TaskExecutors,
    private val expireListener: (Device) -> Unit
) : Runnable {
    private var futureTask: FutureTask<*>? = null
    private val threadLock = ReentrantLock()
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val deviceMap = mutableMapOf<String, Device>()

    /**
     * 現在保持している[Device]の順序を保持したリストを作成して返す。
     *
     * @return [Device]のリスト
     */
    val deviceList: List<Device>
        get() = lock.withLock {
            return deviceMap.values.toList()
        }
    /**
     * 保持している[Device]の数を返す。
     *
     * @return [Device]の数
     */
    val size: Int
        get() = lock.withLock {
            return deviceMap.size
        }

    /**
     * スレッドを開始する。
     */
    fun start() {
        threadLock.withLock {
            FutureTask(this, null).also {
                futureTask = it
                taskExecutors.manager(it)
            }
        }
    }

    /**
     * スレッドに割り込みをかけ終了させる。
     */
    fun stop() {
        threadLock.withLock {
            futureTask?.cancel(false)
            futureTask = null
        }
    }

    private fun isCanceled(): Boolean {
        return futureTask?.isCancelled ?: true
    }

    /**
     * [Device]追加。
     *
     * @param device 追加される[Device]
     */
    fun add(device: Device) {
        lock.withLock {
            deviceMap[device.udn] = device
            condition.signalAll()
        }
    }

    operator fun get(udn: String): Device? {
        lock.withLock {
            return deviceMap[udn]
        }
    }

    /**
     * [Device]削除。
     *
     * @param device 削除される[Device]
     * @return 削除された[Device]
     */
    fun remove(device: Device): Device? {
        lock.withLock {
            return deviceMap.remove(device.udn)
        }
    }

    /**
     * [Device]削除。
     *
     * @param udn 削除される[Device]のudn。
     * @return 削除された[Device]
     */
    fun remove(udn: String): Device? {
        lock.withLock {
            return deviceMap.remove(udn)
        }
    }

    /**
     * 登録された[Device]をクリア。
     */
    fun clear() {
        lock.withLock {
            deviceMap.clear()
        }
    }

    override fun run() {
        Thread.currentThread().let {
            it.name = it.name + "-device-holder"
        }
        lock.withLock {
            try {
                while (!isCanceled()) {
                    while (deviceMap.isEmpty()) {
                        condition.await()
                    }
                    expireDevice()
                    waitNextExpireTime()
                }
            } catch (ignored: InterruptedException) {
            }
        }
    }

    private fun expireDevice() {
        val now = System.currentTimeMillis()
        deviceMap.values
            .toList()
            .filter { it.expireTime < now }
            .forEach {
                deviceMap.remove(it.udn)
                expireListener.invoke(it)
            }
    }

    @Throws(InterruptedException::class)
    private fun waitNextExpireTime() {
        if (deviceMap.isEmpty()) {
            return
        }
        val sleep = max(
            findMostRecentExpireTime() - System.currentTimeMillis() + MARGIN_TIME,
            MARGIN_TIME
        ) // 負の値となる可能性を排除
        condition.await(sleep, TimeUnit.MILLISECONDS)
    }

    private fun findMostRecentExpireTime(): Long {
        return deviceMap.values.minBy { it.expireTime }
            ?.expireTime ?: 0L
    }

    companion object {
        private val MARGIN_TIME = TimeUnit.SECONDS.toMillis(10)
    }
}
