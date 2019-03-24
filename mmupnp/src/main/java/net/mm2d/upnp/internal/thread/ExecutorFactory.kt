/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread

import net.mm2d.upnp.TaskExecutor
import java.util.concurrent.*

internal object ExecutorFactory {
    private const val PRIORITY_CALLBACK = Thread.NORM_PRIORITY
    private const val PRIORITY_IO = Thread.MIN_PRIORITY
    private const val PRIORITY_MANAGER = Thread.MIN_PRIORITY
    private const val PRIORITY_SERVER = Thread.MIN_PRIORITY + 1

    fun createCallback(): TaskExecutor {
        val factory = ExecutorThreadFactory("callback-", PRIORITY_CALLBACK)
        val executor = Executors.newSingleThreadExecutor(factory)
        return DefaultTaskExecutor(executor)
    }

    fun createIo(maxThread: Int = calculateMaximumPoolSize()): TaskExecutor {
        val factory = ExecutorThreadFactory("io-", PRIORITY_IO)
        val queue = ThreadWorkQueue()
        val executor = ThreadPoolExecutor(0, maxThread, 15L, TimeUnit.SECONDS, queue, factory, queue)
        return DefaultTaskExecutor(executor, true)
    }

    private fun calculateMaximumPoolSize(): Int {
        return Math.max(2, Runtime.getRuntime().availableProcessors()) * 2
    }

    fun createManager(): TaskExecutor {
        val executor = createServiceExecutor("mg-", PRIORITY_MANAGER)
        return DefaultTaskExecutor(executor)
    }

    fun createServer(): TaskExecutor {
        val executor = createServiceExecutor("sv-", PRIORITY_SERVER)
        return DefaultTaskExecutor(executor, true)
    }

    private fun createServiceExecutor(prefix: String, priority: Int): ExecutorService {
        val factory = ExecutorThreadFactory(prefix, priority)
        return ThreadPoolExecutor(
            0, Integer.MAX_VALUE,
            0L, TimeUnit.NANOSECONDS,
            SynchronousQueue(), factory
        )
    }
}
