/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.util;

import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.Closeable;
import java.io.IOException;

@RunWith(JUnit4.class)
public class IOUtilsTest {
    @Test
    public void closeQuietly_closeがコールされる() throws Exception {
        final Closeable closeable = mock(Closeable.class);
        IOUtils.closeQuietly(closeable);
        verify(closeable).close();
    }

    @Test
    public void closeQuietly_nullを渡してもExceptionが発生しない() {
        IOUtils.closeQuietly(null);
    }

    @Test
    public void closeQuietly_closeでIOExceptionが発生しても外に伝搬しない() throws Exception {
        final Closeable closeable = mock(Closeable.class);
        doThrow(new IOException()).when(closeable).close();
        IOUtils.closeQuietly(closeable);
    }
}
