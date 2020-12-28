/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.manager

import net.mm2d.upnp.common.internal.thread.TaskExecutors
import net.mm2d.upnp.common.internal.thread.ThreadCondition
import net.mm2d.upnp.cp.Device
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A class holding [Device] found in [net.mm2d.upnp.cp.ControlPoint].
 *
 * Check the expiration date of Device, and notify the expired Device as Lost.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 *
 * @constructor initialize
 * @param expireListener Listener to receive expired notifications
 */
internal class DeviceHolder(
    taskExecutors: TaskExecutors,
    private val expireListener: (Device) -> Unit
) : Runnable {
    private val threadCondition = ThreadCondition(taskExecutors.manager)
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val deviceMap = mutableMapOf<String, Device>()

    val deviceList: List<Device>
        get() = lock.withLock {
            deviceMap.values.toList()
        }

    val size: Int
        get() = lock.withLock {
            deviceMap.size
        }

    fun start() {
        threadCondition.start(this)
    }

    fun stop() {
        threadCondition.stop()
    }

    fun add(device: Device) {
        lock.withLock {
            deviceMap[device.udn] = device
            condition.signalAll()
        }
    }

    operator fun get(udn: String): Device? = lock.withLock {
        deviceMap[udn]
    }

    fun remove(device: Device): Device? = lock.withLock {
        deviceMap.remove(device.udn)
    }

    fun remove(udn: String): Device? = lock.withLock {
        deviceMap.remove(udn)
    }

    fun clear(): Unit = lock.withLock {
        deviceMap.clear()
    }

    override fun run() {
        ThreadCondition.setThreadNameSuffix("-device-holder")
        lock.withLock {
            try {
                while (!threadCondition.isCanceled()) {
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
        val mostRecentExpireTime = deviceMap.values.map { it.expireTime }.minOrNull() ?: 0L
        val duration = mostRecentExpireTime - System.currentTimeMillis() + MARGIN_TIME
        val sleep = maxOf(duration, MARGIN_TIME) // avoid negative value
        condition.await(sleep, TimeUnit.MILLISECONDS)
    }

    companion object {
        private val MARGIN_TIME = TimeUnit.SECONDS.toMillis(10)
    }
}
