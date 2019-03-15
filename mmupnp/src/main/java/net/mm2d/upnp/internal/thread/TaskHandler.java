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
    private final TaskExecutor mCallbackTaskExecutor;
    private final TaskExecutor mIoTaskExecutor;

    public TaskHandler() {
        this(null, null);
    }

    public TaskHandler(
            @Nullable final TaskExecutor callback,
            @Nullable final TaskExecutor io) {
        mCallbackTaskExecutor = callback != null ? callback : new CallbackTaskExecutor();
        mIoTaskExecutor = io != null ? io : new IoTaskExecutor();
    }

    public boolean callback(@Nonnull final Runnable task) {
        return mCallbackTaskExecutor.execute(task);
    }

    public boolean io(@Nonnull final Runnable task) {
        return mIoTaskExecutor.execute(task);
    }

    public void terminate() {
        mCallbackTaskExecutor.terminate();
        mIoTaskExecutor.terminate();
    }
}
