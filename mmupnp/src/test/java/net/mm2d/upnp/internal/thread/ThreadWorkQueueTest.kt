/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class ThreadWorkQueueTest {
    @Test(timeout = 20000L)
    fun executeInParallel_プロセッサ数まで並列化が可能() {
        val processorsCount = NUMBER_OF_PROCESSORS
        val queue = ThreadWorkQueue()
        val executor = ThreadPoolExecutor(
            0, processorsCount,
            5L, TimeUnit.SECONDS, queue, queue
        )
        val latch = CountDownLatch(processorsCount)
        for (i in 0 until processorsCount) {
            executor.execute {
                latch.countDown()
                try {
                    Thread.sleep(10000)
                } catch (ignored: InterruptedException) {
                }
            }
        }
        latch.await()
        assertThat(executor.poolSize).isEqualTo(processorsCount)
        assertThat(queue).hasSize(0)
        val remain = executor.shutdownNow()
        assertThat(remain).hasSize(0)
    }

    @Test(timeout = 20000L)
    fun executeInParallel_プロセッサ数を超えるタスクを積むとあまりがQueueに積まれる() {
        val processorsCount = NUMBER_OF_PROCESSORS
        val queue = ThreadWorkQueue()
        val executor = ThreadPoolExecutor(
            0, processorsCount,
            5L, TimeUnit.SECONDS, queue, queue
        )
        val latch = CountDownLatch(processorsCount)
        for (i in 0 until processorsCount + 1) {
            executor.execute {
                latch.countDown()
                try {
                    Thread.sleep(10000)
                } catch (ignored: InterruptedException) {
                }
            }
        }
        latch.await()
        Thread.sleep(100)
        assertThat(executor.poolSize).isEqualTo(processorsCount)
        assertThat(queue).hasSize(1)
        val remain = executor.shutdownNow()
        assertThat(remain).hasSize(1)
    }

    @Test(timeout = 20000L)
    fun executeInParallel_プロセッサ数未満のスレッドでアイドルスレッドが使用されスレッド数が増えないこと() {
        val queue = ThreadWorkQueue()
        val executor = ThreadPoolExecutor(
            0, NUMBER_OF_PROCESSORS,
            5L, TimeUnit.SECONDS, queue, queue
        )
        val latch = CountDownLatch(1)
        executor.execute { latch.countDown() }
        latch.await()
        assertThat(executor.poolSize).isEqualTo(1)

        Thread.sleep(100)
        val latch2 = CountDownLatch(1)
        executor.execute { latch2.countDown() }
        latch2.await()
        assertThat(executor.poolSize).isEqualTo(1)
        executor.shutdownNow()
    }

    @Test(timeout = 20000L)
    fun executeInParallel_プロセッサ数未満のスレッドでアイドルスレッドが使用されスレッド数が増えないこと2() {
        val queue = ThreadWorkQueue()
        val executor = ThreadPoolExecutor(
            0, NUMBER_OF_PROCESSORS,
            5L, TimeUnit.SECONDS, queue, queue
        )
        val count = 2
        val latch = CountDownLatch(count)
        for (i in 0 until count) {
            executor.execute { latch.countDown() }
        }
        latch.await()
        assertThat(executor.poolSize).isEqualTo(count)

        Thread.sleep(100)
        val latch2 = CountDownLatch(count)
        for (i in 0 until count) {
            executor.execute { latch2.countDown() }
        }
        latch2.await()
        assertThat(executor.poolSize).isEqualTo(count)
        executor.shutdownNow()
    }

    @Test(timeout = 20000L)
    fun executeInParallel_スレッド増減の確認() {
        val queue = ThreadWorkQueue()
        val executor = ThreadPoolExecutor(
            0, NUMBER_OF_PROCESSORS,
            1L, TimeUnit.SECONDS, queue, queue
        )
        val count = 2
        val latch = CountDownLatch(count)
        for (i in 0 until count) {
            executor.execute { latch.countDown() }
        }
        latch.await()
        assertThat(executor.poolSize).isEqualTo(count)
        Thread.sleep(1100)
        assertThat(executor.poolSize).isEqualTo(0)
        val latch2 = CountDownLatch(count)
        for (i in 0 until count) {
            executor.execute { latch2.countDown() }
        }
        latch2.await()
        assertThat(executor.poolSize).isEqualTo(count)
        executor.shutdownNow()
    }

    @Test(timeout = 20000L)
    fun executeInParallel_プロセッサ数を大幅に超えるタスクを積んでも破綻しない() {
        val processorsCount = NUMBER_OF_PROCESSORS
        val queue = ThreadWorkQueue()
        val executor = ThreadPoolExecutor(
            0, processorsCount,
            5L, TimeUnit.SECONDS, queue, queue
        )
        val latch = CountDownLatch(processorsCount)
        for (i in 0 until processorsCount * 100) {
            executor.execute {
                latch.countDown()
                try {
                    Thread.sleep(10000)
                } catch (ignored: InterruptedException) {
                }
            }
        }
        latch.await()
        Thread.sleep(100)
        assertThat(executor.poolSize).isEqualTo(processorsCount)
        executor.shutdownNow()
    }

    @Test(timeout = 20000L)
    fun executeInParallel_shutdownNow後にexecuteしてもRejectedExecutionExceptionは発生しない() {
        val queue = ThreadWorkQueue()
        val executor = ThreadPoolExecutor(
            0, NUMBER_OF_PROCESSORS,
            5L, TimeUnit.SECONDS, queue, queue
        )
        executor.shutdownNow()
        val runnable: Runnable = mockk()
        executor.execute(runnable)
        Thread.sleep(500)
        verify(inverse = true) { runnable.run() }
    }

    @Test(timeout = 20000L)
    fun executeInParallel_shutdown後にexecuteしてもRejectedExecutionExceptionは発生しない() {
        val queue = ThreadWorkQueue()
        val executor = ThreadPoolExecutor(
            0, NUMBER_OF_PROCESSORS,
            5L, TimeUnit.SECONDS, queue, queue
        )
        executor.shutdown()
        val runnable: Runnable = mockk()
        executor.execute(runnable)
        Thread.sleep(500)
        verify(inverse = true) { runnable.run() }
    }

    @Test(timeout = 20000L)
    fun fixedThreadPool() {
        val processorsCount = NUMBER_OF_PROCESSORS
        val queue = ThreadWorkQueue()
        val executor = ThreadPoolExecutor(
            processorsCount, processorsCount,
            0L, TimeUnit.MILLISECONDS, queue, queue
        )
        val latch = CountDownLatch(processorsCount)
        for (i in 0 until processorsCount + 1) {
            executor.execute { latch.countDown() }
        }
        latch.await()
        assertThat(executor.poolSize).isEqualTo(processorsCount)
        executor.shutdownNow()
    }

    @Test
    fun delegate() {
        val delegate: BlockingQueue<Runnable> = mockk(relaxed = true)
        every { delegate.remove() } returns null
        every { delegate.poll() } returns null
        every { delegate.element() } returns null
        every { delegate.peek() } returns null
        val queue = ThreadWorkQueue(delegate)
        val task: Runnable = mockk()
        val collection = mutableSetOf(task)

        queue.add(task)
        verify(exactly = 1) { delegate.add(task) }

        queue.remove()
        verify(exactly = 1) { delegate.remove() }

        queue.poll()
        verify(exactly = 1) { delegate.poll() }

        queue.element()
        verify(exactly = 1) { delegate.element() }

        queue.peek()
        verify(exactly = 1) { delegate.peek() }

        queue.put(task)
        verify(exactly = 1) { delegate.put(task) }

        queue.offer(task, 1, TimeUnit.SECONDS)
        verify(exactly = 1) { delegate.offer(task, 1, TimeUnit.SECONDS) }

        queue.remainingCapacity()
        verify(exactly = 1) { delegate.remainingCapacity() }

        queue.remove(task)
        verify(exactly = 1) { delegate.remove(task) }

        queue.containsAll(collection)
        verify(exactly = 1) { delegate.containsAll(collection) }

        queue.addAll(collection)
        verify(exactly = 1) { delegate.addAll(collection) }

        queue.removeAll(collection)
        verify(exactly = 1) { delegate.removeAll(collection) }

        queue.retainAll(collection)
        verify(exactly = 1) { delegate.retainAll(collection) }

        queue.clear()
        verify(exactly = 1) { delegate.clear() }

        queue.size
        verify(exactly = 1) { delegate.size }

        queue.isEmpty()
        verify(exactly = 1) { delegate.isEmpty() }

        queue.contains(task)
        verify(exactly = 1) { delegate.contains(task) }

        queue.iterator()
        verify(exactly = 1) { delegate.iterator() }

        queue.drainTo(collection)
        verify(exactly = 1) { delegate.drainTo(collection) }

        queue.drainTo(collection, 1)
        verify(exactly = 1) { delegate.drainTo(collection, 1) }
    }

    companion object {
        private const val NUMBER_OF_PROCESSORS = 4
    }
}
