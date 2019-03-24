/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread

import net.mm2d.upnp.TaskExecutor

internal class TaskExecutors(
        callback: TaskExecutor? = null,
        io: TaskExecutor? = null
) {
    private val callbackExecutor = callback ?: ExecutorFactory.createCallback()
    private val ioExecutor = io ?: ExecutorFactory.createIo()
    private val managerExecutor = ExecutorFactory.createManager()
    private val serverExecutor = ExecutorFactory.createServer()

    fun callback(task: Runnable): Boolean {
        return callbackExecutor.execute(task)
    }

    fun callback(task: () -> Unit): Boolean = callback(Runnable { task() })

    fun io(task: Runnable): Boolean {
        return ioExecutor.execute(task)
    }

    fun io(task: () -> Unit): Boolean = io(Runnable { task() })

    fun manager(task: Runnable): Boolean {
        return managerExecutor.execute(task)
    }

    fun server(task: Runnable): Boolean {
        return serverExecutor.execute(task)
    }

    fun terminate() {
        callbackExecutor.terminate()
        ioExecutor.terminate()
        managerExecutor.terminate()
        serverExecutor.terminate()
    }
}
