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

public class TaskHandler {
    private final TaskExecutor mCallbackExecutor;
    private final TaskExecutor mIoExecutor;

    public TaskHandler() {
        this(null);
    }

    public TaskHandler(
            @Nullable final TaskExecutor callback) {
        this(callback, null);
    }

    // VisibleForTesting
    TaskHandler(
            @Nullable final TaskExecutor callback,
            @Nullable final TaskExecutor io) {
        mCallbackExecutor = callback != null ? callback : new CallbackExecutor();
        mIoExecutor = io != null ? io : new IoExecutor();
    }

    public boolean callback(@Nonnull final Runnable task) {
        return mCallbackExecutor.execute(task);
    }

    public boolean io(@Nonnull final Runnable task) {
        return mIoExecutor.execute(task);
    }

    public void terminate() {
        mCallbackExecutor.terminate();
        mIoExecutor.terminate();
    }
}
