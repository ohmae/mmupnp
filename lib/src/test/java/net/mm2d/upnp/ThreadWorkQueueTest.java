/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class ThreadWorkQueueTest {
    private static final int NUMBER_OF_PROCESSORS = 4;

    @Test(timeout = 500)
    public void executeInParallel_プロセッサ数まで並列化が可能() throws Exception {
        final int processorsCount = NUMBER_OF_PROCESSORS;
        final ThreadWorkQueue queue = new ThreadWorkQueue();
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, processorsCount,
                5L, TimeUnit.SECONDS, queue, queue);
        final CountDownLatch latch = new CountDownLatch(processorsCount);
        for (int i = 0; i < processorsCount; i++) {
            executor.execute(() -> {
                latch.countDown();
                try {
                    Thread.sleep(10000);
                } catch (final InterruptedException ignored) {
                }
            });
        }
        latch.await();
        assertThat(executor.getPoolSize(), is(processorsCount));
        assertThat(queue, hasSize(0));
        final List<Runnable> remain = executor.shutdownNow();
        assertThat(remain, hasSize(0));
    }

    @Test(timeout = 500)
    public void executeInParallel_プロセッサ数を超えるタスクを積むとあまりがQueueに積まれる() throws Exception {
        final int processorsCount = NUMBER_OF_PROCESSORS;
        final ThreadWorkQueue queue = new ThreadWorkQueue();
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, processorsCount,
                5L, TimeUnit.SECONDS, queue, queue);
        final CountDownLatch latch = new CountDownLatch(processorsCount);
        for (int i = 0; i < processorsCount + 1; i++) {
            executor.execute(() -> {
                latch.countDown();
                try {
                    Thread.sleep(10000);
                } catch (final InterruptedException ignored) {
                }
            });
        }
        latch.await();
        Thread.sleep(100);
        assertThat(executor.getPoolSize(), is(processorsCount));
        assertThat(queue, hasSize(1));
        final List<Runnable> remain = executor.shutdownNow();
        assertThat(remain, hasSize(1));
    }

    @Test(timeout = 500)
    public void executeInParallel_プロセッサ数未満のスレッドでアイドルスレッドが使用されスレッド数が増えないこと() throws Exception {
        final ThreadWorkQueue queue = new ThreadWorkQueue();
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, NUMBER_OF_PROCESSORS,
                5L, TimeUnit.SECONDS, queue, queue);
        final CountDownLatch latch = new CountDownLatch(1);
        executor.execute(latch::countDown);
        latch.await();
        assertThat(executor.getPoolSize(), is(1));

        Thread.sleep(100);
        final CountDownLatch latch2 = new CountDownLatch(1);
        executor.execute(latch2::countDown);
        latch2.await();
        assertThat(executor.getPoolSize(), is(1));
        executor.shutdownNow();
    }

    @Test(timeout = 500)
    public void executeInParallel_プロセッサ数未満のスレッドでアイドルスレッドが使用されスレッド数が増えないこと2() throws Exception {
        final ThreadWorkQueue queue = new ThreadWorkQueue();
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, NUMBER_OF_PROCESSORS,
                5L, TimeUnit.SECONDS, queue, queue);
        final int count = 2;
        final CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            executor.execute(latch::countDown);
        }
        latch.await();
        assertThat(executor.getPoolSize(), is(count));

        Thread.sleep(100);
        final CountDownLatch latch2 = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            executor.execute(latch2::countDown);
        }
        latch2.await();
        assertThat(executor.getPoolSize(), is(count));
        executor.shutdownNow();
    }

    @Test(timeout = 5000)
    public void executeInParallel_スレッド増減の確認() throws Exception {
        final ThreadWorkQueue queue = new ThreadWorkQueue();
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, NUMBER_OF_PROCESSORS,
                1L, TimeUnit.SECONDS, queue, queue);
        final int count = 2;
        final CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            executor.execute(latch::countDown);
        }
        latch.await();
        assertThat(executor.getPoolSize(), is(count));
        Thread.sleep(1100);
        assertThat(executor.getPoolSize(), is(0));
        final CountDownLatch latch2 = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            executor.execute(latch2::countDown);
        }
        latch2.await();
        assertThat(executor.getPoolSize(), is(count));
        executor.shutdownNow();
    }

    @Test(timeout = 500)
    public void executeInParallel_プロセッサ数を大幅に超えるタスクを積んでも破綻しない() throws Exception {
        final int processorsCount = NUMBER_OF_PROCESSORS;
        final ThreadWorkQueue queue = new ThreadWorkQueue();
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, processorsCount,
                5L, TimeUnit.SECONDS, queue, queue);
        final CountDownLatch latch = new CountDownLatch(processorsCount);
        for (int i = 0; i < processorsCount * 100; i++) {
            executor.execute(() -> {
                latch.countDown();
                try {
                    Thread.sleep(10000);
                } catch (final InterruptedException ignored) {
                }
            });
        }
        latch.await();
        Thread.sleep(100);
        assertThat(executor.getPoolSize(), is(processorsCount));
        executor.shutdownNow();
    }

    @Test(timeout = 500, expected = RejectedExecutionException.class)
    public void executeInParallel_shutdownNow後にexecuteでRejectedExecutionException() throws Exception {
        final ThreadWorkQueue queue = new ThreadWorkQueue();
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, NUMBER_OF_PROCESSORS,
                5L, TimeUnit.SECONDS, queue, queue);
        executor.shutdownNow();
        executor.execute(mock(Runnable.class));
    }

    @Test(timeout = 500, expected = RejectedExecutionException.class)
    public void executeInParallel_shutdown後にexecuteでRejectedExecutionException() throws Exception {
        final ThreadWorkQueue queue = new ThreadWorkQueue();
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, NUMBER_OF_PROCESSORS,
                5L, TimeUnit.SECONDS, queue, queue);
        executor.shutdown();
        executor.execute(() -> {
        });
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "unchecked"})
    @Test
    public void delegate() throws Exception {
        final BlockingQueue<Runnable> delegate = mock(BlockingQueue.class);
        final ThreadWorkQueue queue = new ThreadWorkQueue(delegate);
        final Runnable task = mock(Runnable.class);
        final Collection<Runnable> collection = Collections.singleton(task);

        queue.add(task);
        verify(delegate).add(task);

        queue.remove();
        verify(delegate).remove();

        queue.poll();
        verify(delegate).poll();

        queue.element();
        verify(delegate).element();

        queue.peek();
        verify(delegate).peek();

        queue.put(task);
        verify(delegate).put(task);

        queue.offer(task, 1, TimeUnit.SECONDS);
        verify(delegate).offer(task, 1, TimeUnit.SECONDS);

        queue.remainingCapacity();
        verify(delegate).remainingCapacity();

        queue.remove(task);
        verify(delegate).remove(task);

        queue.containsAll(collection);
        verify(delegate).containsAll(collection);

        queue.addAll(collection);
        verify(delegate).addAll(collection);

        queue.removeAll(collection);
        verify(delegate).removeAll(collection);

        queue.retainAll(collection);
        verify(delegate).retainAll(collection);

        queue.clear();
        verify(delegate).clear();

        queue.size();
        verify(delegate).size();

        queue.isEmpty();
        verify(delegate).isEmpty();

        queue.contains(task);
        verify(delegate).contains(task);

        queue.iterator();
        verify(delegate).iterator();

        queue.toArray();
        verify(delegate).toArray();

        final Runnable[] array = new Runnable[1];
        queue.toArray(array);
        verify(delegate).toArray(array);

        queue.drainTo(collection);
        verify(delegate).drainTo(collection);

        queue.drainTo(collection, 1);
        verify(delegate).drainTo(collection, 1);
    }
}
