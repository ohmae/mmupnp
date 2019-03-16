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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class ExecutorFactory {
    static TaskExecutor createCallback() {
        final ThreadFactory factory = new ExecutorThreadFactory("callback-", Thread.NORM_PRIORITY);
        final ExecutorService executor = Executors.newSingleThreadExecutor(factory);
        return new DefaultTaskExecutor(executor);
    }

    static TaskExecutor createIo() {
        return createIo(calculateMaximumPoolSize());
    }

    private static int calculateMaximumPoolSize() {
        return Math.max(2, Runtime.getRuntime().availableProcessors()) * 2;
    }

    static TaskExecutor createIo(final int maxThread) {
        final ThreadFactory factory = new ExecutorThreadFactory("io-", Thread.MIN_PRIORITY);
        final ThreadWorkQueue queue = new ThreadWorkQueue();
        final ExecutorService executor = new ThreadPoolExecutor(0, maxThread, 1L, TimeUnit.MINUTES, queue, factory, queue);
        return new DefaultTaskExecutor(executor, true);
    }
}
