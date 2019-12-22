/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.thread

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class ExecutorFactoryTest {
    @Test
    fun execute_executeが実行される() {
        val executorService: ExecutorService = mockk(relaxed = true)
        val taskExecutor = DefaultTaskExecutor(executorService)
        val command: Runnable = mockk()

        assertThat(taskExecutor.execute(command)).isTrue()

        verify(exactly = 1) { executorService.execute(any()) }
    }

    @Test
    fun execute_exceptionが発生すればfalse() {
        val executorService: ExecutorService = mockk(relaxed = true)
        val taskExecutor = DefaultTaskExecutor(executorService)
        val command: Runnable = mockk()
        every { executorService.execute(any()) } throws RejectedExecutionException()

        assertThat(taskExecutor.execute(command)).isFalse()
    }

    @Test
    fun execute_terminate後はfalse() {
        val executorService: ExecutorService = mockk(relaxed = true)
        val taskExecutor = DefaultTaskExecutor(executorService)
        val command: Runnable = mockk()

        taskExecutor.terminate()
        assertThat(taskExecutor.execute(command)).isFalse()
    }

    @Test
    fun terminate_shutdownNowが実行される() {
        val executorService: ExecutorService = mockk(relaxed = true)
        val taskExecutor = DefaultTaskExecutor(executorService)

        taskExecutor.terminate()

        verify(exactly = 1) { executorService.shutdownNow() }
    }

    @Test
    fun terminate_2回コールできる() {
        val executorService: ExecutorService = mockk(relaxed = true)
        val taskExecutor = DefaultTaskExecutor(executorService)

        taskExecutor.terminate()
        taskExecutor.terminate()

        verify(exactly = 1) { executorService.shutdownNow() }
    }

    @Test
    fun execute_io_executeが実行される() {
        val executorService: ExecutorService = mockk(relaxed = true)
        val taskExecutor = DefaultTaskExecutor(executorService, true)
        val command: Runnable = mockk()

        assertThat(taskExecutor.execute(command)).isTrue()

        verify(exactly = 1) { executorService.execute(any()) }
    }

    @Test
    fun execute_io_exceptionが発生すればfalse() {
        val executorService: ExecutorService = mockk(relaxed = true)
        val taskExecutor = DefaultTaskExecutor(executorService, true)
        val command: Runnable = mockk()
        every { executorService.execute(any()) } throws RejectedExecutionException()

        assertThat(taskExecutor.execute(command)).isFalse()
    }

    @Test
    fun execute_io_terminate後はfalse() {
        val executorService: ExecutorService = mockk(relaxed = true)
        val taskExecutor = DefaultTaskExecutor(executorService, true)
        val command: Runnable = mockk()

        taskExecutor.terminate()
        assertThat(taskExecutor.execute(command)).isFalse()
    }

    @Test
    fun terminate_io_shutdownNowが実行される() {
        val executorService: ExecutorService = mockk(relaxed = true)
        val taskExecutor = DefaultTaskExecutor(executorService, true)
        every { executorService.awaitTermination(any(), any()) } returns true

        taskExecutor.terminate()

        verify(exactly = 1) { executorService.shutdown() }
    }

    @Test
    fun terminate_io_2回コールできる() {
        val executorService: ExecutorService = mockk(relaxed = true)
        val taskExecutor = DefaultTaskExecutor(executorService, true)
        every { executorService.awaitTermination(any(), any()) } returns true

        taskExecutor.terminate()
        taskExecutor.terminate()

        verify(exactly = 1) { executorService.shutdown() }
    }

    @Test
    fun terminate_io_awaitTerminationが割り込まれてもshutdownNowがコールされる() {
        val executorService: ExecutorService = mockk(relaxed = true)
        val taskExecutor = DefaultTaskExecutor(executorService, true)
        every { executorService.awaitTermination(any(), any()) } throws InterruptedException()

        taskExecutor.terminate()

        verify(exactly = 1) { executorService.shutdownNow() }
    }

    @Test
    fun terminate_io_タイムアウトしたらshutdownNow() {
        val executorService: ExecutorService = mockk(relaxed = true)
        val taskExecutor = DefaultTaskExecutor(executorService, true)
        every { executorService.awaitTermination(any(), any()) } returns false

        taskExecutor.terminate()

        verify(exactly = 1) { executorService.shutdown() }
        verify(exactly = 1) { executorService.shutdownNow() }
    }
}
