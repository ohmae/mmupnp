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

public class TaskHandlerTest {
    private TaskExecutor mCallback;
    private TaskExecutor mIo;
    private TaskHandler mTaskHandler;

    @Before
    public void setUp() throws Exception {
        mCallback = mock(CallbackExecutor.class);
        mIo = mock(IoExecutor.class);
        mTaskHandler = new TaskHandler(mCallback, mIo);
    }

    @Test
    public void callback() {
        final Runnable task = mock(Runnable.class);
        mTaskHandler.callback(task);

        verify(mCallback, times(1)).execute(task);
    }

    @Test
    public void io() {
        final Runnable task = mock(Runnable.class);
        mTaskHandler.io(task);

        verify(mIo, times(1)).execute(task);
    }

    @Test
    public void terminate() {
        mTaskHandler.terminate();

        verify(mCallback, times(1)).terminate();
        verify(mIo, times(1)).terminate();
    }
}