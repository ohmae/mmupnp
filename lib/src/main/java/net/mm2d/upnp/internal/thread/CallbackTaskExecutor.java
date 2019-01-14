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
import java.util.concurrent.RejectedExecutionException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CallbackTaskExecutor implements TaskExecutor {
    @Nullable
    private ExecutorService mExecutor;

    CallbackTaskExecutor() {
        this(createExecutor());
    }

    CallbackTaskExecutor(@Nonnull final ExecutorService executor) {
        mExecutor = executor;
    }

    @Nonnull
    private static ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor();
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
        executor.shutdownNow();
        mExecutor = null;
    }
}
