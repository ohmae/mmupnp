/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TextUtilsTest {
    @Test
    public void isEmpty_nullもしくは空文字でtrueそれ意外false() {
        assertThat(TextUtils.isEmpty(null), is(true));
        assertThat(TextUtils.isEmpty(""), is(true));
        assertThat(TextUtils.isEmpty(" "), is(false));
    }
}
