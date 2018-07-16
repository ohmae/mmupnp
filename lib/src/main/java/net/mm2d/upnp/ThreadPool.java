/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.log.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

class ThreadPool {
    @Nonnull
    private final ExecutorService mSequentialExecutor;
    @Nonnull
    private final ExecutorService mParallelExecutor;

    ThreadPool() {
        this(Executors.newSingleThreadExecutor(), Executors.newCachedThreadPool());
    }

    // VisibleForTesting
    @SuppressWarnings("WeakerAccess")
    ThreadPool(
            @Nonnull final ExecutorService sequential,
            @Nonnull final ExecutorService parallel) {
        mSequentialExecutor = sequential;
        mParallelExecutor = parallel;
    }

    boolean executeInSequential(@Nonnull final Runnable command) {
        if (mSequentialExecutor.isShutdown()) {
            return false;
        }
        try {
            mSequentialExecutor.execute(command);
        } catch (final RejectedExecutionException ignored) {
            return false;
        }
        return true;
    }

    boolean executeInParallel(@Nonnull final Runnable command) {
        if (mParallelExecutor.isShutdown()) {
            return false;
        }
        try {
            mParallelExecutor.execute(command);
        } catch (final RejectedExecutionException ignored) {
            return false;
        }
        return true;
    }

    void terminate() {
        mSequentialExecutor.shutdownNow();
        mParallelExecutor.shutdown();
        try {
            if (!mParallelExecutor.awaitTermination(
                    Property.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                mParallelExecutor.shutdownNow();
            }
        } catch (final InterruptedException e) {
            Log.w(e);
        }
    }
}
