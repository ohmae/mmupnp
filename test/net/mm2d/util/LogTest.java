/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.util;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import net.mm2d.util.Log.Print;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LogTest {
    @Test
    public void v_VERBOSEレベルでコールされること() {
        final String tag = "TAG";
        final String message = "MESSAGE";
        final Throwable throwable = new Throwable();

        Print print = spy(Print.class);
        Log.setPrint(print);
        Log.v(tag, message);
        verify(print).println(Log.VERBOSE, tag, message);

        print = mock(Print.class);
        Log.setPrint(print);
        Log.v(tag, message, throwable);
        verify(print).println(eq(Log.VERBOSE), eq(tag), contains(message));
    }

    @Test
    public void d_DEBUGレベルでコールされること() {
        final String tag = "TAG";
        final String message = "MESSAGE";
        final Throwable throwable = new Throwable();

        Print print = mock(Print.class);
        Log.setPrint(print);
        Log.d(tag, message);
        verify(print).println(Log.DEBUG, tag, message);

        print = mock(Print.class);
        Log.setPrint(print);
        Log.d(tag, message, throwable);
        verify(print).println(eq(Log.DEBUG), eq(tag), contains(message));
    }

    @Test
    public void i_INFOレベルでコールされること() {
        final String tag = "TAG";
        final String message = "MESSAGE";
        final Throwable throwable = new Throwable();

        Print print = mock(Print.class);
        Log.setPrint(print);
        Log.i(tag, message);
        verify(print).println(Log.INFO, tag, message);

        print = mock(Print.class);
        Log.setPrint(print);
        Log.i(tag, message, throwable);
        verify(print).println(eq(Log.INFO), eq(tag), contains(message));
    }

    @Test
    public void w_WARNレベルでコールされること() {
        final String tag = "TAG";
        final String message = "MESSAGE";
        final Throwable throwable = new Throwable();

        Print print = mock(Print.class);
        Log.setPrint(print);
        Log.w(tag, message);
        verify(print).println(Log.WARN, tag, message);

        print = mock(Print.class);
        Log.setPrint(print);
        Log.w(tag, message, throwable);
        verify(print).println(eq(Log.WARN), eq(tag), contains(message));

        print = mock(Print.class);
        Log.setPrint(print);
        Log.w(tag, throwable);
        verify(print).println(eq(Log.WARN), eq(tag), anyString());
    }

    @Test
    public void e_ERRORレベルでコールされること() {
        final String tag = "TAG";
        final String message = "MESSAGE";
        final Throwable throwable = new Throwable();

        Print print = mock(Print.class);
        Log.setPrint(print);
        Log.e(tag, message);
        verify(print).println(Log.ERROR, tag, message);

        print = mock(Print.class);
        Log.setPrint(print);
        Log.e(tag, message, throwable);
        verify(print).println(eq(Log.ERROR), eq(tag), contains(message));
    }
}
