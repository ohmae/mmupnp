/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TestParseUtilsTest {
    @Test(expected = InvocationTargetException.class)
    public void constructor() throws Exception {
        final Constructor<TextParseUtils> constructor = TextParseUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void parseIntSafely_10進数異常値指定() {
        assertThat(TextParseUtils.parseIntSafely(null, -1), is(-1));
        assertThat(TextParseUtils.parseIntSafely(null, -1), is(-1));
        assertThat(TextParseUtils.parseIntSafely("abcdef", -1), is(-1));
        assertThat(TextParseUtils.parseIntSafely("123abc", -1), is(-1));
    }

    @Test
    public void parseIntSafely_10進数正常にパースできる() {
        assertThat(TextParseUtils.parseIntSafely("100", -1), is(100));
        assertThat(TextParseUtils.parseIntSafely("-100", -1), is(-100));
        assertThat(TextParseUtils.parseIntSafely("2147483647", -1), is(2147483647));
        assertThat(TextParseUtils.parseIntSafely("-2147483648", -1), is(-2147483648));
    }

    @Test
    public void parseIntSafely_2進数正常にパースできる() {
        assertThat(TextParseUtils.parseIntSafely("100", 2, -1), is(4));
        assertThat(TextParseUtils.parseIntSafely("0100", 2, -1), is(4));
        assertThat(TextParseUtils.parseIntSafely("-100", 2, -1), is(-4));
    }

    @Test
    public void parseIntSafely_8進数正常にパースできる() {
        assertThat(TextParseUtils.parseIntSafely("777", 8, -1), is(0777));
        assertThat(TextParseUtils.parseIntSafely("0777", 8, -1), is(0777));
        assertThat(TextParseUtils.parseIntSafely("-777", 8, -1), is(-0777));
    }

    @Test
    public void parseIntSafely_16進数正常にパースできる() {
        assertThat(TextParseUtils.parseIntSafely("abc", 16, -1), is(0xabc));
        assertThat(TextParseUtils.parseIntSafely("-abc", 16, -1), is(-0xabc));
        assertThat(TextParseUtils.parseIntSafely("7fffffff", 16, -1), is(0x7fffffff));
    }

    @Test
    public void parseLongSafely_異常値指定() {
        assertThat(TextParseUtils.parseLongSafely(null, -1), is(-1L));
        assertThat(TextParseUtils.parseLongSafely(null, -1), is(-1L));
        assertThat(TextParseUtils.parseLongSafely("abcdef", -1), is(-1L));
        assertThat(TextParseUtils.parseLongSafely("123abc", -1), is(-1L));
    }

    @Test
    public void parseLongSafely_10進数正常にパースできる() {
        assertThat(TextParseUtils.parseLongSafely("100", -1), is(100L));
        assertThat(TextParseUtils.parseLongSafely("-100", -1), is(-100L));
        assertThat(TextParseUtils.parseLongSafely("9223372036854775807", -1), is(9223372036854775807L));
        assertThat(TextParseUtils.parseLongSafely("-9223372036854775808", -1), is(-9223372036854775808L));
    }

    @Test
    public void parseLongSafely_2進数正常にパースできる() {
        assertThat(TextParseUtils.parseLongSafely("100", 2, -1), is(4L));
        assertThat(TextParseUtils.parseLongSafely("0100", 2, -1), is(4L));
        assertThat(TextParseUtils.parseLongSafely("-100", 2, -1), is(-4L));
    }

    @Test
    public void parseLongSafely_8進数正常にパースできる() {
        assertThat(TextParseUtils.parseLongSafely("777", 8, -1), is(0777L));
        assertThat(TextParseUtils.parseLongSafely("0777", 8, -1), is(0777L));
        assertThat(TextParseUtils.parseLongSafely("-777", 8, -1), is(-0777L));
    }

    @Test
    public void parseLongSafely_16進数正常にパースできる() {
        assertThat(TextParseUtils.parseLongSafely("abc", 16, -1), is(0xabcL));
        assertThat(TextParseUtils.parseLongSafely("-abc", 16, -1), is(-0xabcL));
        assertThat(TextParseUtils.parseLongSafely("7fffffffffffffff", 16, -1), is(0x7fffffffffffffffL));
    }
}
