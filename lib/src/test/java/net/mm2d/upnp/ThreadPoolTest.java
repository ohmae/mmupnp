/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ThreadPoolTest {
    @Test
    public void executeInParallel_executeが実行される() throws Exception {
        final ExecutorService sequential = mock(ExecutorService.class);
        final ExecutorService parallel = mock(ExecutorService.class);
        final ThreadPool threadPool = new ThreadPool(sequential, parallel);
        final Runnable command = mock(Runnable.class);

        assertThat(threadPool.executeInParallel(command), is(true));

        verify(parallel, times(1)).execute(command);
    }

    @Test
    public void executeInParallel_shutdown済みなら何もしないでfalse() throws Exception {
        final ExecutorService sequential = mock(ExecutorService.class);
        final ExecutorService parallel = mock(ExecutorService.class);
        final ThreadPool threadPool = new ThreadPool(sequential, parallel);
        doReturn(true).when(parallel).isShutdown();
        final Runnable command = mock(Runnable.class);

        assertThat(threadPool.executeInParallel(command), is(false));

        verify(parallel, never()).execute(command);
    }

    @Test
    public void executeInParallel_exceptionが発生すればfalse() throws Exception {
        final ExecutorService sequential = mock(ExecutorService.class);
        final ExecutorService parallel = mock(ExecutorService.class);
        final ThreadPool threadPool = new ThreadPool(sequential, parallel);
        final Runnable command = mock(Runnable.class);
        doThrow(new RejectedExecutionException()).when(parallel).execute(command);

        assertThat(threadPool.executeInParallel(command), is(false));
    }

    @Test
    public void executeInSequential_executeが実行される() throws Exception {
        final ExecutorService sequential = mock(ExecutorService.class);
        final ExecutorService parallel = mock(ExecutorService.class);
        final ThreadPool threadPool = new ThreadPool(sequential, parallel);
        final Runnable command = mock(Runnable.class);

        assertThat(threadPool.executeInSequential(command), is(true));

        verify(sequential, times(1)).execute(command);
    }

    @Test
    public void executeInSequential_shutdown済みなら何もしないでfalse() throws Exception {
        final ExecutorService sequential = mock(ExecutorService.class);
        final ExecutorService parallel = mock(ExecutorService.class);
        final ThreadPool threadPool = new ThreadPool(sequential, parallel);
        doReturn(true).when(sequential).isShutdown();
        final Runnable command = mock(Runnable.class);

        assertThat(threadPool.executeInSequential(command), is(false));

        verify(sequential, never()).execute(command);
    }

    @Test
    public void executeInSequential_exceptionが発生すればfalse() throws Exception {
        final ExecutorService sequential = mock(ExecutorService.class);
        final ExecutorService parallel = mock(ExecutorService.class);
        final ThreadPool threadPool = new ThreadPool(sequential, parallel);
        final Runnable command = mock(Runnable.class);
        doThrow(new RejectedExecutionException()).when(sequential).execute(command);

        assertThat(threadPool.executeInSequential(command), is(false));
    }

    @Test(timeout = 2000L)
    public void terminate_timeover() throws Exception {
        final ExecutorService sequential = mock(ExecutorService.class);
        final ExecutorService parallel = mock(ExecutorService.class);
        final ThreadPool threadPool = new ThreadPool(sequential, parallel);
        doReturn(false).when(parallel).awaitTermination(anyLong(), ArgumentMatchers.any(TimeUnit.class));

        threadPool.terminate();
        verify(parallel, times(1)).shutdownNow();
    }

    @Test(timeout = 2000L)
    public void terminate_exception() throws Exception {
        final ExecutorService sequential = mock(ExecutorService.class);
        final ExecutorService parallel = mock(ExecutorService.class);
        final ThreadPool threadPool = new ThreadPool(sequential, parallel);
        doThrow(new InterruptedException()).when(parallel).awaitTermination(anyLong(), ArgumentMatchers.any(TimeUnit.class));

        threadPool.terminate();
    }
}
