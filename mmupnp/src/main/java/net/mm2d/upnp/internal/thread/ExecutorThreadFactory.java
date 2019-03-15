/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class ExecutorThreadFactory implements ThreadFactory {
    private final ThreadGroup mThreadGroup;
    private final AtomicInteger mThreadNumber = new AtomicInteger(1);
    private final String mNamePrefix;
    private final int mPriority;

    ExecutorThreadFactory(
            @Nonnull final String namePrefix,
            final int priority) {
        mNamePrefix = "mmupnp-" + namePrefix;
        mPriority = priority;
        mThreadGroup = getThreadGroup();
    }

    private static ThreadGroup getThreadGroup() {
        SecurityManager s = System.getSecurityManager();
        return (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
    }

    @Nullable
    @Override
    public Thread newThread(@Nonnull final Runnable runnable) {
        final Thread thread = new Thread(mThreadGroup, runnable,
                mNamePrefix + mThreadNumber.getAndIncrement(),
                0);
        if (thread.getPriority() != mPriority) {
            thread.setPriority(mPriority);
        }
        return thread;
    }
}
