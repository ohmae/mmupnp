/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread;

import net.mm2d.upnp.TaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

class ExecutorFactory {
    private static final int PRIORITY_CALLBACK = Thread.NORM_PRIORITY;
    private static final int PRIORITY_IO = Thread.MIN_PRIORITY;
    private static final int PRIORITY_MANAGER = Thread.MIN_PRIORITY;
    private static final int PRIORITY_SERVER = Thread.MIN_PRIORITY + 1;

    @Nonnull
    static TaskExecutor createCallback() {
        final ThreadFactory factory = new ExecutorThreadFactory("cb-", PRIORITY_CALLBACK);
        final ExecutorService executor = Executors.newSingleThreadExecutor(factory);
        return new DefaultTaskExecutor(executor);
    }

    @Nonnull
    static TaskExecutor createIo() {
        return createIo(calculateMaximumPoolSize());
    }

    private static int calculateMaximumPoolSize() {
        return Math.max(2, Runtime.getRuntime().availableProcessors()) * 2;
    }

    @Nonnull
    static TaskExecutor createIo(final int maxThread) {
        final ThreadFactory factory = new ExecutorThreadFactory("io-", PRIORITY_IO);
        final ThreadWorkQueue queue = new ThreadWorkQueue();
        final ExecutorService executor = new ThreadPoolExecutor(0, maxThread, 15L, TimeUnit.SECONDS, queue, factory, queue);
        return new DefaultTaskExecutor(executor, true);
    }

    @Nonnull
    static TaskExecutor createManager() {
        final ExecutorService executor = createServiceExecutor("mg-", PRIORITY_MANAGER);
        return new DefaultTaskExecutor(executor);
    }

    @Nonnull
    static TaskExecutor createServer() {
        final ExecutorService executor = createServiceExecutor("sv-", PRIORITY_SERVER);
        return new DefaultTaskExecutor(executor, true);
    }

    @Nonnull
    private static ExecutorService createServiceExecutor(
            @Nonnull final String prefix,
            final int priority) {
        final ThreadFactory factory = new ExecutorThreadFactory(prefix, priority);
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                0L, TimeUnit.NANOSECONDS,
                new SynchronousQueue<>(), factory);
    }
}
