/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TextUtilsTest {
    @Test
    public void isEmpty_nullもしくは空文字でtrueそれ意外false() {
        assertTrue(TextUtils.isEmpty(null));
        assertTrue(TextUtils.isEmpty(""));
        assertFalse(TextUtils.isEmpty(" "));
    }
}
