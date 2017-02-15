/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import net.mm2d.util.Log.Print;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class LogTest {
    @Before
    public void before() {
        Log.setLogLevel(Log.VERBOSE);
    }

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

    @Test
    public void setLogLevel_指定したレベル以上のログのみ出力される() {
        final String tag = "TAG";
        final String message = "MESSAGE";
        final Throwable throwable = new Throwable();
        Print print;

        Log.setLogLevel(Log.VERBOSE);
        print = mock(Print.class);
        Log.setPrint(print);
        Log.v(tag, message);
        Log.v(tag, message, throwable);
        Log.d(tag, message);
        Log.d(tag, message, throwable);
        Log.i(tag, message);
        Log.i(tag, message, throwable);
        Log.w(tag, message);
        Log.w(tag, message, throwable);
        Log.w(tag, throwable);
        Log.e(tag, message);
        Log.e(tag, message, throwable);
        verify(print, times(11)).println(anyInt(), anyString(), anyString());

        Log.setLogLevel(Log.DEBUG);
        print = mock(Print.class);
        Log.setPrint(print);
        Log.v(tag, message);
        Log.v(tag, message, throwable);
        verify(print, never()).println(anyInt(), anyString(), anyString());
        Log.d(tag, message);
        Log.d(tag, message, throwable);
        Log.i(tag, message);
        Log.i(tag, message, throwable);
        Log.w(tag, message);
        Log.w(tag, message, throwable);
        Log.w(tag, throwable);
        Log.e(tag, message);
        Log.e(tag, message, throwable);
        verify(print, times(9)).println(anyInt(), anyString(), anyString());

        Log.setLogLevel(Log.INFO);
        print = mock(Print.class);
        Log.setPrint(print);
        Log.v(tag, message);
        Log.v(tag, message, throwable);
        Log.d(tag, message);
        Log.d(tag, message, throwable);
        verify(print, never()).println(anyInt(), anyString(), anyString());

        Log.i(tag, message);
        Log.i(tag, message, throwable);
        Log.w(tag, message);
        Log.w(tag, message, throwable);
        Log.w(tag, throwable);
        Log.e(tag, message);
        Log.e(tag, message, throwable);
        verify(print, times(7)).println(anyInt(), anyString(), anyString());

        Log.setLogLevel(Log.WARN);
        print = mock(Print.class);
        Log.setPrint(print);
        Log.v(tag, message);
        Log.v(tag, message, throwable);
        Log.d(tag, message);
        Log.d(tag, message, throwable);
        Log.i(tag, message);
        Log.i(tag, message, throwable);
        verify(print, never()).println(anyInt(), anyString(), anyString());

        Log.w(tag, message);
        Log.w(tag, message, throwable);
        Log.w(tag, throwable);
        Log.e(tag, message);
        Log.e(tag, message, throwable);
        verify(print, times(5)).println(anyInt(), anyString(), anyString());

        Log.setLogLevel(Log.ERROR);
        print = mock(Print.class);
        Log.setPrint(print);
        Log.v(tag, message);
        Log.v(tag, message, throwable);
        Log.d(tag, message);
        Log.d(tag, message, throwable);
        Log.i(tag, message);
        Log.i(tag, message, throwable);
        Log.w(tag, message);
        Log.w(tag, message, throwable);
        Log.w(tag, throwable);
        verify(print, never()).println(anyInt(), anyString(), anyString());

        Log.e(tag, message);
        Log.e(tag, message, throwable);
        verify(print, times(2)).println(anyInt(), anyString(), anyString());

        Log.setLogLevel(Log.ASSERT);
        print = mock(Print.class);
        Log.setPrint(print);
        Log.v(tag, message);
        Log.v(tag, message, throwable);
        Log.d(tag, message);
        Log.d(tag, message, throwable);
        Log.i(tag, message);
        Log.i(tag, message, throwable);
        Log.w(tag, message);
        Log.w(tag, message, throwable);
        Log.w(tag, throwable);
        Log.e(tag, message);
        Log.e(tag, message, throwable);
        verify(print, never()).println(anyInt(), anyString(), anyString());
    }

    @Test
    public void v_Tagがnullでも問題ない() {
        Log.e(null, "");
    }
}
