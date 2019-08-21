/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread

import net.mm2d.upnp.TaskExecutor
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class ThreadCondition(
    private val executor: TaskExecutor
) {
    private var futureTask: FutureTask<*>? = null
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var ready = false

    fun start(runnable: Runnable): Unit = lock.withLock {
        ready = false
        FutureTask(runnable, null).also {
            futureTask = it
            executor.execute(it)
        }
    }

    fun stop(): Unit = lock.withLock {
        futureTask?.cancel(false)
        futureTask = null
    }

    fun waitReady(): Boolean = lock.withLock {
        val task = futureTask ?: return false
        if (task.isDone) return false
        if (!ready) {
            try {
                condition.awaitNanos(PREPARE_TIMEOUT_NANOS)
            } catch (ignored: InterruptedException) {
            }
        }
        ready
    }

    fun notifyReady(): Unit = lock.withLock {
        ready = true
        condition.signalAll()
    }

    fun isCanceled(): Boolean = futureTask?.isCancelled ?: true

    companion object {
        private val PREPARE_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(1)
    }
}
