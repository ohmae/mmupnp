/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message

import com.google.common.truth.Truth.assertThat
import net.mm2d.upnp.internal.message.SingleHttpHeaders.Entry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.*

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class SingleHttpHeadersTest {
    @Test
    fun testEntry_getName() {
        val name = "name"
        val value = "value"
        val entry = Entry(name, value)

        assertThat(entry.name).isEqualTo(name)
    }

    @Test
    fun testEntry_getValue() {
        val name = "name"
        val value = "value"
        val entry = Entry(name, value)

        assertThat(entry.value).isEqualTo(value)
    }

    @Test
    fun testEntry_copy() {
        val name = "name"
        val value = "value"
        val entry1 = Entry(name, value)
        val entry2 = Entry(entry1)

        assertThat(entry1).isEqualTo(entry2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testEntry_異なるnameで更新() {
        val name = "name"
        val value = "value"
        val entry = Entry(name, value)

        entry.name = value
    }

    @Test
    fun testEntry_hashCode() {
        val name = "name"
        val value = "value"
        val entry1 = Entry(name, value)
        val entry2 = Entry(entry1)

        assertThat(entry1.hashCode()).isEqualTo(entry2.hashCode())
    }

    @Test
    fun testEntry_toString() {
        val name = "name"
        val value = "value"
        val entry = Entry(name, value)

        assertThat(entry.toString().contains(name)).isTrue()
        assertThat(entry.toString().contains(value)).isTrue()
    }

    @Test
    fun testEntry_equals() {
        val name = "name"
        val value = "value"
        val entry1 = Entry(name, value)
        val entry2 = Entry(entry1)
        val entry3 = Entry(name + "1", value)
        val entry4 = Entry(name, value + "1")

        assertThat(entry1 == entry1).isTrue()
        assertThat(entry1 == entry2).isTrue()
        assertThat(entry1 == entry3).isFalse()
        assertThat(entry1 == entry4).isFalse()
        assertThat(entry1 == null).isFalse()
    }

    @Test
    fun size_個数が反映される() {
        val header = SingleHttpHeaders()

        assertThat(header.size()).isEqualTo(0)
        assertThat(header.isEmpty).isTrue()
        header.put("name", "value")
        assertThat(header.size()).isEqualTo(1)
        assertThat(header.isEmpty).isFalse()
        header.put("name1", "value")
        assertThat(header.size()).isEqualTo(2)
        assertThat(header.isEmpty).isFalse()
        header.put("NAME", "value")
        assertThat(header.size()).isEqualTo(2)
        assertThat(header.isEmpty).isFalse()
    }

    @Test
    fun get_大文字小文字に関係なく値が取得できる() {
        val name1 = "name1"
        val value1 = "value1"
        val header = SingleHttpHeaders()
        header.put(name1, value1)

        assertThat(header[name1]).isEqualTo(value1)
        assertThat(header[name1.uppercase(Locale.US)]).isEqualTo(value1)
    }

    @Test
    fun get_大文字小文字に関係なく上書きできる() {
        val name1 = "name1"
        val value1 = "value1"
        val value2 = "value2"
        val header = SingleHttpHeaders()
        header.put(name1, value1)
        header.put(name1.uppercase(Locale.US), value2)

        assertThat(header[name1]).isEqualTo(value2)
    }

    @Test
    fun remove_大文字小文字関係なく削除できる() {
        val name1 = "name1"
        val name2 = "name2"
        val value1 = "value1"
        val value2 = "value2"
        val header = SingleHttpHeaders()
        header.put(name1, value1)
        header.put(name2, value2)

        assertThat(header[name2]).isEqualTo(value2)
        assertThat(header.remove(name2.uppercase(Locale.US))).isEqualTo(value2)
        assertThat(header[name2]).isNull()

        assertThat(header[name1]).isEqualTo(value1)
        assertThat(header.remove(name1)).isEqualTo(value1)
        assertThat(header[name1]).isNull()

        header.put(name1.uppercase(Locale.US), value1)
        header.put(name2.uppercase(Locale.US), value2)

        assertThat(header[name2]).isEqualTo(value2)
        assertThat(header.remove(name2.uppercase(Locale.US))).isEqualTo(value2)
        assertThat(header[name2]).isNull()

        assertThat(header[name1]).isEqualTo(value1)
        assertThat(header.remove(name1)).isEqualTo(value1)
        assertThat(header[name1]).isNull()
    }

    @Test
    fun remove_存在しない値をremoveしてもクラッシュしない() {
        val name1 = "name1"
        val name2 = "name2"
        val value1 = "value1"
        val value2 = "value2"
        val header = SingleHttpHeaders()
        header.put(name1, value1)
        header.put(name2, value2)

        assertThat(header.remove(name1)).isEqualTo(value1)
        assertThat(header.remove(name1)).isNull()
    }

    @Test
    fun containsValue_値が含まれる() {
        val name1 = "name1"
        val name2 = "name2"
        val value1 = "value1"
        val value2 = "value2"
        val header = SingleHttpHeaders()
        header.put(name1, value1)
        header.put(name2, value2)

        assertThat(header.containsValue(name1, "value")).isTrue()
        assertThat(header.containsValue(name1.uppercase(Locale.US), "value")).isTrue()

        assertThat(header.containsValue(name2, value1)).isFalse()
        assertThat(header.containsValue(name2.uppercase(Locale.US), value1)).isFalse()
    }

    @Test
    fun toString_値が含まれる() {
        val name1 = "name1"
        val name2 = "name2"
        val value1 = "value1"
        val value2 = "value2"
        val header = SingleHttpHeaders()
        header.put(name1, value1)
        header.put(name2, value2)

        val string = header.toString()
        assertThat(string.contains(name1)).isTrue()
        assertThat(string.contains(name2)).isTrue()
        assertThat(string.contains(value1)).isTrue()
        assertThat(string.contains(value2)).isTrue()
    }

    @Test
    fun clear_すべてクリア() {
        val name1 = "name1"
        val name2 = "name2"
        val value1 = "value1"
        val value2 = "value2"
        val header = SingleHttpHeaders()
        header.put(name1, value1)
        header.put(name2, value2)

        assertThat(header[name1]).isEqualTo(value1)
        assertThat(header[name2]).isEqualTo(value2)

        header.clear()

        assertThat(header[name1]).isNull()
        assertThat(header[name2]).isNull()
    }

    @Test
    fun values_Setインターフェース() {
        val name1 = "name1"
        val name2 = "name2"
        val value1 = "value1"
        val value2 = "value2"
        val header = SingleHttpHeaders()
        header.put(name1, value1)
        header.put(name2, value2)

        val entrySet = header.values()
        assertThat(entrySet).contains(Entry(name1, value1))
        assertThat(entrySet).contains(Entry(name2, value2))
    }

    @Test
    fun copy_ディープコピーできている() {
        val name1 = "name1"
        val name2 = "name2"
        val value1 = "value1"
        val value2 = "value2"
        val header1 = SingleHttpHeaders()
        header1.put(name1, value1)
        header1.put(name2, value2)
        val header2 = SingleHttpHeaders(header1)
        val list1 = ArrayList(header1.values())
        val list2 = ArrayList(header2.values())

        assertThat(list1 == list2).isTrue()

        header2.put(name2, value1)
        assertThat(header1[name2]).isEqualTo(value2)
        assertThat(header2[name2]).isEqualTo(value1)
    }
}
