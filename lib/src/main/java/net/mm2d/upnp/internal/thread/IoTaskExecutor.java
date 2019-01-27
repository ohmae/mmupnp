/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread;

import net.mm2d.log.Logger;
import net.mm2d.upnp.TaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class IoTaskExecutor implements TaskExecutor {
    @Nullable
    private ExecutorService mExecutor;

    IoTaskExecutor() {
        this(calculateMaximumPoolSize());
    }

    IoTaskExecutor(final int maxThread) {
        this(createExecutor(maxThread));
    }

    // VisibleForTesting
    IoTaskExecutor(@Nonnull final ExecutorService executor) {
        mExecutor = executor;
    }

    @Nonnull
    private static ExecutorService createExecutor(final int maxThread) {
        final ThreadWorkQueue queue = new ThreadWorkQueue();
        return new ThreadPoolExecutor(0, maxThread,1L, TimeUnit.MINUTES, queue, queue);
    }

    private static int calculateMaximumPoolSize() {
        return Math.max(2, Runtime.getRuntime().availableProcessors()) * 2;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public boolean execute(@Nonnull final Runnable task) {
        final ExecutorService executor = mExecutor;
        if (executor == null || executor.isShutdown()) {
            return false;
        }
        try {
            executor.execute(task);
        } catch (final RejectedExecutionException ignored) {
            return false;
        }
        return true;
    }

    @Override
    public void terminate() {
        final ExecutorService executor = mExecutor;
        if (executor == null || executor.isShutdown()) {
            return;
        }
        try {
            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (final InterruptedException e) {
            executor.shutdownNow();
            Logger.w(e);
        } finally {
            mExecutor = null;
        }
    }
}
