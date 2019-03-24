/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread

import io.mockk.mockk
import io.mockk.verify
import net.mm2d.upnp.TaskExecutor
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TaskExecutorsTest {
    @Test
    fun callback() {
        val callbackTaskExecutor: TaskExecutor = mockk(relaxed = true)
        val ioTaskExecutor: TaskExecutor = mockk(relaxed = true)
        val taskExecutors = TaskExecutors(callbackTaskExecutor, ioTaskExecutor)
        val task: Runnable = mockk()
        taskExecutors.callback(task)

        verify(exactly = 1) { callbackTaskExecutor.execute(task) }
        taskExecutors.terminate()
    }

    @Test
    fun io() {
        val callbackTaskExecutor: TaskExecutor = mockk(relaxed = true)
        val ioTaskExecutor: TaskExecutor = mockk(relaxed = true)
        val taskExecutors = TaskExecutors(callbackTaskExecutor, ioTaskExecutor)
        val task: Runnable = mockk()
        taskExecutors.io(task)

        verify(exactly = 1) { ioTaskExecutor.execute(task) }
        taskExecutors.terminate()
    }

    @Test
    fun terminate() {
        val callbackTaskExecutor: TaskExecutor = mockk(relaxed = true)
        val ioTaskExecutor: TaskExecutor = mockk(relaxed = true)
        val taskExecutors = TaskExecutors(callbackTaskExecutor, ioTaskExecutor)
        taskExecutors.terminate()

        verify(exactly = 1) { callbackTaskExecutor.terminate() }
        verify(exactly = 1) { ioTaskExecutor.terminate() }
    }
}