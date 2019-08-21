/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread

import java.util.concurrent.*

internal object ExecutorFactory {
    private const val PRIORITY_CALLBACK = Thread.NORM_PRIORITY
    private const val PRIORITY_IO = Thread.MIN_PRIORITY
    private const val PRIORITY_MANAGER = Thread.MIN_PRIORITY
    private const val PRIORITY_SERVER = Thread.MIN_PRIORITY + 1
    private const val KEEP_ALIVE_SECOND = 15L

    fun callback(): ExecuteFunction {
        val factory = ExecutorThreadFactory("callback-", PRIORITY_CALLBACK)
        val executor = Executors.newSingleThreadExecutor(factory)
        return DefaultTaskExecutor(executor).toFunction()
    }

    fun io(maxThread: Int = maxPoolSize()): ExecuteFunction {
        val factory = ExecutorThreadFactory("io-", PRIORITY_IO)
        val queue = ThreadWorkQueue()
        val executor = ThreadPoolExecutor(0, maxThread, KEEP_ALIVE_SECOND, TimeUnit.SECONDS, queue, factory, queue)
        return DefaultTaskExecutor(executor, true).toFunction()
    }

    private fun maxPoolSize(): Int = maxOf(2, Runtime.getRuntime().availableProcessors()) * 2

    fun manager(): ExecuteFunction =
        DefaultTaskExecutor(serviceExecutor("mg-", PRIORITY_MANAGER)).toFunction()

    fun server(): ExecuteFunction =
        DefaultTaskExecutor(serviceExecutor("sv-", PRIORITY_SERVER), true).toFunction()

    private fun serviceExecutor(prefix: String, priority: Int): ExecutorService =
        ThreadPoolExecutor(
            0, Integer.MAX_VALUE,
            0L, TimeUnit.NANOSECONDS,
            SynchronousQueue(),
            ExecutorThreadFactory(prefix, priority)
        )
}
