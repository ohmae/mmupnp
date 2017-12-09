/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class PairTest {
    @Test
    public void constructor_nullでも可() throws Exception {
        new Pair<String, String>(null, null);
        new Pair<Integer, Integer>(null, null);
    }

    @Test
    public void getKey_Keyが取り出せる() throws Exception {
        final String key = "key";
        final String value = "value";
        final Pair<String, String> pair = new Pair<>(key, value);

        assertThat(pair.getKey(), is(key));
    }

    @Test
    public void getValue_Valueが取り出せる() throws Exception {
        final String key = "key";
        final String value = "value";
        final Pair<String, String> pair = new Pair<>(key, value);

        assertThat(pair.getValue(), is(value));
    }

    @Test
    public void equals_同一インスタンス() throws Exception {
        final Pair<String, String> pair1 = new Pair<>("1", "2");

        assertThat(pair1.equals(pair1), is(true));
    }

    @Test
    public void equals_フィールドが同一() throws Exception {
        final Pair<String, String> pair1 = new Pair<>("1", "2");
        final Pair<String, String> pair2 = new Pair<>("1", "2");

        assertThat(pair1.equals(pair2), is(true));
    }

    @Test
    public void equals_フィールドが異なる() throws Exception {
        final Pair<String, String> pair1 = new Pair<>("1", "2");
        final Pair<String, String> pair2 = new Pair<>("1", "3");

        assertThat(pair1.equals(pair2), is(false));
    }

    @Test
    public void equals_nullとの比較() throws Exception {
        final Pair<String, String> pair1 = new Pair<>("1", "2");

        assertThat(pair1.equals(null), is(false));
    }

    @Test
    public void hashCode_equalsが真なら同一() throws Exception {
        final Pair<String, String> pair1 = new Pair<>("1", "2");
        final Pair<String, String> pair2 = new Pair<>("1", "2");

        assertThat(pair1.hashCode(), is(pair2.hashCode()));
    }

    @Test
    public void toString_両方の値が含まれる() throws Exception {
        final Pair<String, String> pair1 = new Pair<>("1", "2");

        assertThat(pair1.toString(), containsString("1"));
        assertThat(pair1.toString(), containsString("2"));
    }

    @Test
    public void nullalbe() throws Exception {
        final Pair<String, String> pair1 = new Pair<>(null, "1");
        final Pair<String, String> pair2 = new Pair<>("1", null);
        final Pair<String, String> pair3 = new Pair<>(null, null);
        final Pair<String, String> pair4 = new Pair<>(null, null);

        pair1.hashCode();
        pair2.hashCode();
        pair3.hashCode();
        pair1.toString();
        pair2.toString();
        pair3.toString();
        assertThat(pair1.equals(null), is(false));
        assertThat(pair1.equals(""), is(false));
        assertThat(pair1.equals(pair2), is(false));
        assertThat(pair1.equals(pair3), is(false));
        assertThat(pair2.equals(pair3), is(false));
        assertThat(pair3.equals(pair4), is(true));
    }
}
