/*
 * Copyright(C) 2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.HttpHeader.Entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class HttpHeaderTest {
    @Test
    public void testEntry_getName() {
        final String name = "name";
        final String value = "value";
        final HttpHeader.Entry entry = new HttpHeader.Entry(name, value);

        assertThat(entry.getName(), is(name));
    }

    @Test
    public void testEntry_getValue() {
        final String name = "name";
        final String value = "value";
        final HttpHeader.Entry entry = new HttpHeader.Entry(name, value);

        assertThat(entry.getValue(), is(value));
    }

    @Test
    public void testEntry_copy() {
        final String name = "name";
        final String value = "value";
        final HttpHeader.Entry entry1 = new HttpHeader.Entry(name, value);
        final HttpHeader.Entry entry2 = new HttpHeader.Entry(entry1);

        assertThat(entry1, is(entry2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEntry_異なるnameで更新() {
        final String name = "name";
        final String value = "value";
        final HttpHeader.Entry entry = new HttpHeader.Entry(name, value);

        entry.setName(value);
    }

    @Test
    public void testEntry_hashCode() {
        final String name = "name";
        final String value = "value";
        final HttpHeader.Entry entry1 = new HttpHeader.Entry(name, value);
        final HttpHeader.Entry entry2 = new HttpHeader.Entry(entry1);

        assertThat(entry1.hashCode(), is(entry2.hashCode()));
    }

    @Test
    public void testEntry_toString() {
        final String name = "name";
        final String value = "value";
        final HttpHeader.Entry entry = new HttpHeader.Entry(name, value);

        assertThat(entry.toString().contains(name), is(true));
        assertThat(entry.toString().contains(value), is(true));
    }

    @Test
    public void testEntry_equals() {
        final String name = "name";
        final String value = "value";
        final HttpHeader.Entry entry1 = new HttpHeader.Entry(name, value);
        final HttpHeader.Entry entry2 = new HttpHeader.Entry(entry1);
        final HttpHeader.Entry entry3 = new HttpHeader.Entry(name + "1", value);
        final HttpHeader.Entry entry4 = new HttpHeader.Entry(name, value + "1");

        assertThat(entry1.equals(entry1), is(true));
        assertThat(entry1.equals(entry2), is(true));
        assertThat(entry1.equals(entry3), is(false));
        assertThat(entry1.equals(entry4), is(false));
        assertThat(entry1.equals(null), is(false));
        assertThat(entry1.equals(name), is(false));
    }

    @Test
    public void size_個数が反映される() {
        final HttpHeader header = new HttpHeader();

        assertThat(header.size(), is(0));
        assertThat(header.isEmpty(), is(true));
        header.put("name", "value");
        assertThat(header.size(), is(1));
        assertThat(header.isEmpty(), is(false));
        header.put("name1", "value");
        assertThat(header.size(), is(2));
        assertThat(header.isEmpty(), is(false));
        header.put("NAME", "value");
        assertThat(header.size(), is(2));
        assertThat(header.isEmpty(), is(false));
    }

    @Test
    public void get_大文字小文字に関係なく値が取得できる() {
        final String name1 = "name1";
        final String value1 = "value1";
        final HttpHeader header = new HttpHeader();
        header.put(name1, value1);

        assertThat(header.get(name1), is(value1));
        assertThat(header.get(name1.toUpperCase()), is(value1));
    }

    @Test
    public void get_大文字小文字に関係なく上書きできる() {
        final String name1 = "name1";
        final String value1 = "value1";
        final String value2 = "value2";
        final HttpHeader header = new HttpHeader();
        header.put(name1, value1);
        header.put(name1.toUpperCase(), value2);

        assertThat(header.get(name1), is(value2));
    }

    @Test
    public void remove_大文字小文字関係なく削除できる() {
        final String name1 = "name1";
        final String name2 = "name2";
        final String value1 = "value1";
        final String value2 = "value2";
        final HttpHeader header = new HttpHeader();
        header.put(name1, value1);
        header.put(name2, value2);

        assertThat(header.get(name2), is(value2));
        assertThat(header.remove(name2.toUpperCase()), is(value2));
        assertThat(header.get(name2), is(nullValue()));

        assertThat(header.get(name1), is(value1));
        assertThat(header.remove(name1), is(value1));
        assertThat(header.get(name1), is(nullValue()));

        header.put(name1.toUpperCase(), value1);
        header.put(name2.toUpperCase(), value2);

        assertThat(header.get(name2), is(value2));
        assertThat(header.remove(name2.toUpperCase()), is(value2));
        assertThat(header.get(name2), is(nullValue()));

        assertThat(header.get(name1), is(value1));
        assertThat(header.remove(name1), is(value1));
        assertThat(header.get(name1), is(nullValue()));
    }

    @Test
    public void remove_存在しない値をremoveしてもクラッシュしない() {
        final String name1 = "name1";
        final String name2 = "name2";
        final String value1 = "value1";
        final String value2 = "value2";
        final HttpHeader header = new HttpHeader();
        header.put(name1, value1);
        header.put(name2, value2);

        assertThat(header.remove(name1), is(value1));
        assertThat(header.remove(name1), is(nullValue()));
    }

    @Test
    public void containsValue_値が含まれる() {
        final String name1 = "name1";
        final String name2 = "name2";
        final String value1 = "value1";
        final String value2 = "value2";
        final HttpHeader header = new HttpHeader();
        header.put(name1, value1);
        header.put(name2, value2);

        assertThat(header.containsValue(name1, "value"), is(true));
        assertThat(header.containsValue(name1.toUpperCase(), "value"), is(true));

        assertThat(header.containsValue(name2, value1), is(false));
        assertThat(header.containsValue(name2.toUpperCase(), value1), is(false));
    }

    @Test
    public void toString_値が含まれる() {
        final String name1 = "name1";
        final String name2 = "name2";
        final String value1 = "value1";
        final String value2 = "value2";
        final HttpHeader header = new HttpHeader();
        header.put(name1, value1);
        header.put(name2, value2);

        final String string = header.toString();
        assertThat(string.contains(name1), is(true));
        assertThat(string.contains(name2), is(true));
        assertThat(string.contains(value1), is(true));
        assertThat(string.contains(value2), is(true));
    }

    @Test
    public void clear_すべてクリア() {
        final String name1 = "name1";
        final String name2 = "name2";
        final String value1 = "value1";
        final String value2 = "value2";
        final HttpHeader header = new HttpHeader();
        header.put(name1, value1);
        header.put(name2, value2);

        assertThat(header.get(name1), is(value1));
        assertThat(header.get(name2), is(value2));

        header.clear();

        assertThat(header.get(name1), is(nullValue()));
        assertThat(header.get(name2), is(nullValue()));
    }

    @Test
    public void values_Setインターフェース() {
        final String name1 = "name1";
        final String name2 = "name2";
        final String value1 = "value1";
        final String value2 = "value2";
        final HttpHeader header = new HttpHeader();
        header.put(name1, value1);
        header.put(name2, value2);

        final Collection<Entry> entrySet = header.values();
        assertThat(entrySet, hasItem(new HttpHeader.Entry(name1, value1)));
        assertThat(entrySet, hasItem(new HttpHeader.Entry(name2, value2)));
    }

    @Test
    public void copy_ディープコピーできている() {
        final String name1 = "name1";
        final String name2 = "name2";
        final String value1 = "value1";
        final String value2 = "value2";
        final HttpHeader header1 = new HttpHeader();
        header1.put(name1, value1);
        header1.put(name2, value2);
        final HttpHeader header2 = new HttpHeader(header1);
        final ArrayList<HttpHeader.Entry> list1 = new ArrayList<>(header1.values());
        final ArrayList<HttpHeader.Entry> list2 = new ArrayList<>(header2.values());

        assertThat(list1.equals(list2), is(true));

        header2.put(name2, value1);
        assertThat(header1.get(name2), is(value2));
        assertThat(header2.get(name2), is(value1));
    }
}
