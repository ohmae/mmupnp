/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.Closeable;
import java.io.IOException;

@RunWith(JUnit4.class)
public class IoUtilsTest {
    @Test
    public void closeQuietly_closeがコールされる() throws IOException {
        final Closeable closeable = mock(Closeable.class);
        IoUtils.closeQuietly(closeable);
        verify(closeable).close();
    }

    @Test
    public void closeQuietly_nullを渡してもExceptionが発生しない() {
        IoUtils.closeQuietly(null);
    }

    @Test
    public void closeQuietly_closeでIOExceptionが発生しても外に伝搬しない() {
        try {
            final Closeable closeable = mock(Closeable.class);
            doThrow(new IOException()).when(closeable).close();
            IoUtils.closeQuietly(closeable);
        } catch (final IOException e) {
            fail();
        }
    }
}
