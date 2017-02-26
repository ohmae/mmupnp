/*
 * Copyright(C) 2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Set;

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

    @Test
    public void size_個数が反映される() {
        final HttpHeader header = new HttpHeader();

        assertThat(header.size(), is(0));
        header.put("name", "value");
        assertThat(header.size(), is(1));
        header.put("name1", "value");
        assertThat(header.size(), is(2));
        header.put("NAME", "value");
        assertThat(header.size(), is(2));
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

        assertThat(header.get(name1), is(value1));
        header.remove(name1);
        assertThat(header.get(name1), is(nullValue()));

        assertThat(header.get(name2), is(value2));
        header.remove(name2.toUpperCase());
        assertThat(header.get(name2), is(nullValue()));
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
    public void entrySet_Setインターフェース() {
        final String name1 = "name1";
        final String name2 = "name2";
        final String value1 = "value1";
        final String value2 = "value2";
        final HttpHeader header = new HttpHeader();
        header.put(name1, value1);
        header.put(name2, value2);

        final Set<HttpHeader.Entry> entrySet = header.entrySet();
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
        final ArrayList<HttpHeader.Entry> list1 = new ArrayList<>(header1.entrySet());
        final ArrayList<HttpHeader.Entry> list2 = new ArrayList<>(header2.entrySet());

        assertThat(list1.equals(list2), is(true));

        header2.put(name2, value1);
        assertThat(header1.get(name2), is(value2));
        assertThat(header2.get(name2), is(value1));
    }
}
