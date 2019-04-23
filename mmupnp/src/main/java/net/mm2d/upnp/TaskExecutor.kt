/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

/**
 * Executor interface that executes tasks in a specified thread.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface TaskExecutor {
    /**
     * Execute a task in a specific thread.
     *
     * @param task task to execute
     * @return true if execution could be done or if it could be queued up
     */
    fun execute(task: Runnable): Boolean

    /**
     * Called during ControlPoint exit processing.
     *
     * If need to stop threads etc., implement it here.
     */
    fun terminate() = Unit
}
