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
    private final TaskExecutor mCallbackExecutor;
    private final TaskExecutor mIoExecutor;
    private final TaskExecutor mManagerExecutor = ExecutorFactory.createManager();
    private final TaskExecutor mServerExecutor = ExecutorFactory.createServer();

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
    }
}
