/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.thread

import net.mm2d.upnp.common.TaskExecutor

internal class TaskExecutors(
    callback: TaskExecutor? = null,
    io: TaskExecutor? = null
) {
    val callback = callback?.toFunction() ?: ExecutorFactory.callback()
    val io = io?.toFunction() ?: ExecutorFactory.io()
    val manager = ExecutorFactory.manager()
    val server = ExecutorFactory.server()

    fun terminate() {
        callback.terminate()
        io.terminate()
        manager.terminate()
        server.terminate()
    }
}
