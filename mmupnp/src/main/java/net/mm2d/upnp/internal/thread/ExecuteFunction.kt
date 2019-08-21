/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread

import net.mm2d.upnp.TaskExecutor

class ExecuteFunction(
    private val executor: TaskExecutor
) : TaskExecutor by executor {
    operator fun invoke(task: Runnable) = executor.execute(task)
    operator fun invoke(task: () -> Unit) = executor.execute(task.toRunnable())
}

private fun (() -> Unit).toRunnable() = Runnable { this() }
internal fun TaskExecutor.toFunction() = ExecuteFunction(this)
