/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import com.google.common.truth.Truth.assertThat
import net.mm2d.upnp.Http.Status
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class HttpTest {
    @Test
    fun status_valueOf_変換できる() {
        assertThat(Status.valueOf(200)).isEqualTo(Status.HTTP_OK)
    }

    @Test
    fun status_valueOf_想定外の値はINVALID() {
        assertThat(Status.valueOf(1)).isEqualTo(Status.HTTP_INVALID)
    }

    @Test
    fun parseDate_RFC_1123_GMT() {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
                .parse("2018-1-28 13:45:55 GMT")
        assertThat(Http.parseDate("Sun, 28 Jan 2018 13:45:55 GMT")).isEqualTo(date)
    }

    @Test
    fun parseDate_RFC_1123_JST() {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
                .parse("2018-1-28 13:45:55 JST")
        assertThat(Http.parseDate("Sun, 28 Jan 2018 13:45:55 JST")).isEqualTo(date)
    }

    @Test
    fun parseDate_RFC_1036_GMT() {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
                .parse("2018-1-28 13:45:55 GMT")
        assertThat(Http.parseDate("Sunday, 28-Jan-18 13:45:55 GMT")).isEqualTo(date)
    }

    @Test
    fun parseDate_RFC_1036_JST() {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
                .parse("2018-1-28 13:45:55 JST")
        assertThat(Http.parseDate("Sunday, 28-Jan-18 13:45:55 JST")).isEqualTo(date)
    }

    @Test
    fun parseDate_ASC_TIME() {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
                .parse("2018-1-28 13:45:55 GMT")
        assertThat(Http.parseDate("Sun Jan 28 13:45:55 2018")).isEqualTo(date)
    }

    @Test
    fun parseDate_Error() {
        assertThat(Http.parseDate("2018-1-28 13:45:55")).isNull()
    }

    @Test
    fun parseDate_nullを渡してもException発生しない() {
        assertThat(Http.parseDate(null)).isNull()
    }

    @Test
    fun getCurrentDate() {
        assertThat(Http.parseDate(Http.currentDate)).isNotNull()
    }

    @Test
    fun isHttpUrl() {
        assertThat(Http.isHttpUrl(null)).isFalse()
        assertThat(Http.isHttpUrl("")).isFalse()
        assertThat(Http.isHttpUrl("https://example.com/")).isFalse()
        assertThat(Http.isHttpUrl("http://example.com/")).isTrue()
    }

    @Test
    fun makeUrlWithScopeId_0指定は何もしない() {
        assertThat(Http.makeUrlWithScopeId("http://[fe80::1234]:8888/device.xml", 0).toString())
                .isEqualTo("http://[fe80::1234]:8888/device.xml")
        assertThat(Http.makeUrlWithScopeId("http://192.0.2.3:8888/device.xml", 0).toString())
                .isEqualTo("http://192.0.2.3:8888/device.xml")
        assertThat(Http.makeUrlWithScopeId("http://[fe80::1234]/device.xml", 0).toString())
                .isEqualTo("http://[fe80::1234]/device.xml")
        assertThat(Http.makeUrlWithScopeId("http://192.0.2.3/device.xml", 0).toString())
                .isEqualTo("http://192.0.2.3/device.xml")
    }

    @Test
    fun makeUrlWithScopeId_scope_idが追加できる() {
        assertThat(Http.makeUrlWithScopeId("http://[fe80::1234]:8888/device.xml", 1).toString())
                .isEqualTo("http://[fe80::1234%1]:8888/device.xml")
        assertThat(Http.makeUrlWithScopeId("http://[fe80::1234]/device.xml", 1).toString())
                .isEqualTo("http://[fe80::1234%1]/device.xml")
        assertThat(Http.makeUrlWithScopeId("http://[fe80::1234]:80/device.xml", 1).toString())
                .isEqualTo("http://[fe80::1234%1]/device.xml")
        assertThat(Http.makeUrlWithScopeId("http://[fe80::1234]:8888/device.xml&bitrate=1200", 1).toString())
                .isEqualTo("http://[fe80::1234%1]:8888/device.xml&bitrate=1200")
    }

    @Test
    fun makeUrlWithScopeId_指定済みの場合は置換する() {
        assertThat(Http.makeUrlWithScopeId("http://[fe80::1234%22]:8888/device.xml", 1).toString())
                .isEqualTo("http://[fe80::1234%1]:8888/device.xml")
    }

    @Test
    fun makeUrlWithScopeId_IPv6リテラル以外は追加しない() {
        assertThat(Http.makeUrlWithScopeId("http://192.0.2.1:8888/device.xml", 1).toString())
                .isEqualTo("http://192.0.2.1:8888/device.xml")
        assertThat(Http.makeUrlWithScopeId("http://192.0.2.1/device.xml", 1).toString())
                .isEqualTo("http://192.0.2.1/device.xml")
        assertThat(Http.makeUrlWithScopeId("http://www.example.com:8888/device.xml", 1).toString())
                .isEqualTo("http://www.example.com:8888/device.xml")
        assertThat(Http.makeUrlWithScopeId("http://www.example.com/device.xml", 1).toString())
                .isEqualTo("http://www.example.com/device.xml")
    }

    @Test
    fun getAbsoluteUrl_locationがホスト名のみ() {
        val baseUrl = "http://10.0.0.1:1000/"
        val url1 = "http://10.0.0.1:1000/hoge/fuga"
        val url2 = "/hoge/fuga"
        val url3 = "fuga"

        assertThat(Http.makeAbsoluteUrl(baseUrl, url1, 0))
                .isEqualTo(URL("http://10.0.0.1:1000/hoge/fuga"))
        assertThat(Http.makeAbsoluteUrl(baseUrl, url2, 0))
                .isEqualTo(URL("http://10.0.0.1:1000/hoge/fuga"))
        assertThat(Http.makeAbsoluteUrl(baseUrl, url3, 0))
                .isEqualTo(URL("http://10.0.0.1:1000/fuga"))
    }

    @Test
    fun getAbsoluteUrl_locationがホスト名のみで末尾のスラッシュなし() {
        val baseUrl = "http://10.0.0.1:1000"
        val url1 = "http://10.0.0.1:1000/hoge/fuga"
        val url2 = "/hoge/fuga"
        val url3 = "fuga"

        assertThat(Http.makeAbsoluteUrl(baseUrl, url1, 0))
                .isEqualTo(URL("http://10.0.0.1:1000/hoge/fuga"))
        assertThat(Http.makeAbsoluteUrl(baseUrl, url2, 0))
                .isEqualTo(URL("http://10.0.0.1:1000/hoge/fuga"))
        assertThat(Http.makeAbsoluteUrl(baseUrl, url3, 0))
                .isEqualTo(URL("http://10.0.0.1:1000/fuga"))
    }

    @Test
    fun getAbsoluteUrl_locationがファイル名で終わる() {
        val baseUrl = "http://10.0.0.1:1000/hoge/fuga"
        val url1 = "http://10.0.0.1:1000/hoge/fuga"
        val url2 = "/hoge/fuga"
        val url3 = "fuga"

        assertThat(Http.makeAbsoluteUrl(baseUrl, url1, 0))
                .isEqualTo(URL("http://10.0.0.1:1000/hoge/fuga"))
        assertThat(Http.makeAbsoluteUrl(baseUrl, url2, 0))
                .isEqualTo(URL("http://10.0.0.1:1000/hoge/fuga"))
        assertThat(Http.makeAbsoluteUrl(baseUrl, url3, 0))
                .isEqualTo(URL("http://10.0.0.1:1000/hoge/fuga"))
    }

    @Test
    fun getAbsoluteUrl_locationがディレクトリ名で終わる() {
        val baseUrl = "http://10.0.0.1:1000/hoge/fuga/"
        val url1 = "http://10.0.0.1:1000/hoge/fuga"
        val url2 = "/hoge/fuga"
        val url3 = "fuga"

        assertThat(Http.makeAbsoluteUrl(baseUrl, url1, 0))
                .isEqualTo(URL("http://10.0.0.1:1000/hoge/fuga"))
        assertThat(Http.makeAbsoluteUrl(baseUrl, url2, 0))
                .isEqualTo(URL("http://10.0.0.1:1000/hoge/fuga"))
        assertThat(Http.makeAbsoluteUrl(baseUrl, url3, 0))
                .isEqualTo(URL("http://10.0.0.1:1000/hoge/fuga/fuga"))
    }

    @Test
    fun getAbsoluteUrl_locationにクエリーがついている() {
        val baseUrl = "http://10.0.0.1:1000/hoge/fuga?a=foo&b=bar"
        val url1 = "http://10.0.0.1:1000/hoge/fuga"
        val url2 = "/hoge/fuga"
        val url3 = "fuga"

        assertThat(Http.makeAbsoluteUrl(baseUrl, url1, 0))
                .isEqualTo(URL("http://10.0.0.1:1000/hoge/fuga"))
        assertThat(Http.makeAbsoluteUrl(baseUrl, url2, 0))
                .isEqualTo(URL("http://10.0.0.1:1000/hoge/fuga"))
        assertThat(Http.makeAbsoluteUrl(baseUrl, url3, 0))
                .isEqualTo(URL("http://10.0.0.1:1000/hoge/fuga"))
    }
}
