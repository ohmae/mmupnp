/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread

import net.mm2d.log.Logger
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * WorkQueue to pass to ThreadPoolExecutor.
 *
 * Create a thread pool with the following features for parallel execution
 *
 * - Create up to the maximum number of threads when needed
 * - Reduce the number of threads if idle for a certain time
 * - Prioritize idle threads before creating worker threads
 *
 * For a worker thread between corePoolSize and maximumPoolSize,
 * ThreadPoolExecutor calls workQueue#offer at execute time, and creates a thread if false.
 * At this time, if the number of worker threads exceeds maximumPoolSize,
 * it is rejected and RejectedExecutionHandler is called.
 *
 * Therefore, the following Queue is required to create the thread pool with the above features
 *
 * - If an idle thread (a thread in wait state in take or poll) exists when offer() is called, it will be queued and return true.
 * - Return false otherwise
 * - Implement RejectedExecutionHandler and queue if it is not shutdown state when rejectedExecution is called.
 */
internal class ThreadWorkQueue(
    private val delegate: BlockingQueue<Runnable> = LinkedBlockingQueue()
) : BlockingQueue<Runnable> by delegate, RejectedExecutionHandler {
    private val idleThreads = AtomicInteger(0)

    override fun offer(runnable: Runnable): Boolean =
        if (idleThreads.get() == 0) {
            false
        } else delegate.offer(runnable)

    @Throws(InterruptedException::class)
    override fun take(): Runnable {
        idleThreads.incrementAndGet()
        try {
            return delegate.take()
        } finally {
            idleThreads.decrementAndGet()
        }
    }

    @Throws(InterruptedException::class)
    override fun poll(timeout: Long, unit: TimeUnit): Runnable? {
        idleThreads.incrementAndGet()
        try {
            return delegate.poll(timeout, unit)
        } finally {
            idleThreads.decrementAndGet()
        }
    }

    override fun rejectedExecution(r: Runnable, executor: ThreadPoolExecutor) {
        if (executor.isShutdown) {
            Logger.e("already shutdown: task $r is rejected from $executor")
        } else if (!delegate.offer(r)) {
            Logger.e("Unexpected problem: task $r is rejected from $executor")
        }
    }
}
