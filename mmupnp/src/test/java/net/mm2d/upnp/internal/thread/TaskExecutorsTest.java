/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.thread;

import net.mm2d.upnp.TaskExecutor;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class TaskExecutorsTest {
    private TaskExecutor mCallback;
    private TaskExecutor mIo;
    private TaskExecutors mTaskExecutors;

    @Before
    public void setUp() throws Exception {
        mCallback = mock(CallbackExecutor.class);
        mIo = mock(IoExecutor.class);
        mTaskExecutors = new TaskExecutors(mCallback, mIo);
    }

    @Test
    public void callback() {
        final Runnable task = mock(Runnable.class);
        mTaskExecutors.callback(task);

        verify(mCallback, times(1)).execute(task);
    }

    @Test
    public void io() {
        final Runnable task = mock(Runnable.class);
        mTaskExecutors.io(task);

        verify(mIo, times(1)).execute(task);
    }

    @Test
    public void terminate() {
        mTaskExecutors.terminate();

        verify(mCallback, times(1)).terminate();
        verify(mIo, times(1)).terminate();
    }
}