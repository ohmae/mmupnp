/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread;

import net.mm2d.upnp.TaskExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TaskExecutors {
    @Nonnull
    private final TaskExecutor mCallbackExecutor;
    @Nonnull
    private final TaskExecutor mIoExecutor;
    @Nonnull
    private final TaskExecutor mManagerExecutor;
    @Nonnull
    private final TaskExecutor mServerExecutor;

    public TaskExecutors() {
        this(null);
    }

    public TaskExecutors(
            @Nullable final TaskExecutor callback) {
        this(callback, null);
    }

    // VisibleForTesting
    TaskExecutors(
            @Nullable final TaskExecutor callback,
            @Nullable final TaskExecutor io) {
        mCallbackExecutor = callback != null ? callback : ExecutorFactory.createCallback();
        mIoExecutor = io != null ? io : ExecutorFactory.createIo();
        mManagerExecutor = ExecutorFactory.createManager();
        mServerExecutor = ExecutorFactory.createServer();
    }

    public boolean callback(@Nonnull final Runnable task) {
        return mCallbackExecutor.execute(task);
    }

    public boolean io(@Nonnull final Runnable task) {
        return mIoExecutor.execute(task);
    }

    public boolean manager(@Nonnull final Runnable task) {
        return mManagerExecutor.execute(task);
    }

    public boolean server(@Nonnull final Runnable task) {
        return mServerExecutor.execute(task);
    }

    public void terminate() {
        mCallbackExecutor.terminate();
        mIoExecutor.terminate();
        mManagerExecutor.terminate();
        mServerExecutor.terminate();
    }
}
