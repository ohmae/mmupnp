/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread;

import net.mm2d.log.Logger;
import net.mm2d.upnp.Property;
import net.mm2d.upnp.TaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class DefaultTaskExecutor implements TaskExecutor {
    private static final long AWAIT_TIMEOUT = Property.DEFAULT_TIMEOUT;
    @Nullable
    private ExecutorService mExecutor;

    private final boolean mAwaitTermination;

    DefaultTaskExecutor(@Nonnull final ExecutorService executor) {
        this(executor, false);
    }

    DefaultTaskExecutor(
            @Nonnull final ExecutorService executor,
            final boolean awaitTermination) {
        mExecutor = executor;
        mAwaitTermination = awaitTermination;
    }

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
        if (!mAwaitTermination) {
            executor.shutdownNow();
            mExecutor = null;
            return;
        }
        try {
            executor.shutdown();
            if (!executor.awaitTermination(AWAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
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
