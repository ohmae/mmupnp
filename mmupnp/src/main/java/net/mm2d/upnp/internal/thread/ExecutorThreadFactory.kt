/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

internal class ExecutorThreadFactory(
        namePrefix: String,
        private val priority: Int
) : ThreadFactory {
    private val namePrefix = "mmupnp-$namePrefix"
    private val threadNumber = AtomicInteger(1)
    private val threadGroup = System.getSecurityManager()?.threadGroup
            ?: Thread.currentThread().threadGroup

    override fun newThread(runnable: Runnable): Thread {
        val threadName = namePrefix + threadNumber.getAndIncrement()
        return Thread(threadGroup, runnable, threadName).also {
            if (it.priority != priority) {
                it.priority = priority
            }
        }
    }
}
