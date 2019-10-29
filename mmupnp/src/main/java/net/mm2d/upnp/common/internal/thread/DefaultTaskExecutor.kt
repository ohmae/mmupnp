/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.thread

import net.mm2d.log.Logger
import net.mm2d.upnp.common.Property
import net.mm2d.upnp.common.TaskExecutor
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal class DefaultTaskExecutor(
    executor: ExecutorService,
    private val awaitTermination: Boolean = false
) : TaskExecutor {
    private val executorReference = AtomicReference<ExecutorService?>(executor)

    override fun execute(task: Runnable): Boolean {
        val executor = executorReference.get() ?: return false
        try {
            executor.execute(task)
        } catch (ignored: RejectedExecutionException) {
            return false
        }
        return true
    }

    override fun terminate() {
        val executor = executorReference.getAndSet(null) ?: return
        if (!awaitTermination) {
            executor.shutdownNow()
            return
        }
        try {
            executor.shutdown()
            if (!executor.awaitTermination(AWAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Logger.w(e)
        }
    }

    companion object {
        private val AWAIT_TIMEOUT = Property.DEFAULT_TIMEOUT.toLong()
    }
}
