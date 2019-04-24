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
 * A class holding [Device] found in [net.mm2d.upnp.ControlPoint].
 *
 * Check the expiration date of Device, and notify the expired Device as Lost.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 *
 * @constructor initialize
 * @param expireListener Listener to receive expired notifications
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

    val deviceList: List<Device>
        get() = lock.withLock {
            return deviceMap.values.toList()
        }
    val size: Int
        get() = lock.withLock {
            return deviceMap.size
        }

    fun start() {
        threadLock.withLock {
            FutureTask(this, null).also {
                futureTask = it
                taskExecutors.manager(it)
            }
        }
    }

    fun stop() {
        threadLock.withLock {
            futureTask?.cancel(false)
            futureTask = null
        }
    }

    private fun isCanceled(): Boolean {
        return futureTask?.isCancelled ?: true
    }

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

    fun remove(device: Device): Device? {
        lock.withLock {
            return deviceMap.remove(device.udn)
        }
    }

    fun remove(udn: String): Device? {
        lock.withLock {
            return deviceMap.remove(udn)
        }
    }

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
        ) // avoid negative value
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
