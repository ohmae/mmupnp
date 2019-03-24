/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import com.google.common.truth.Truth.assertThat
import net.mm2d.upnp.util.TestUtils
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class HttpResponseTest {
    @Test
    fun readData_読み出しができること() {
        val response = HttpResponse.create()
        response.readData(TestUtils.getResourceAsStream("cds-length.bin"))

        assertThat(response.startLine).isEqualTo("HTTP/1.1 200 OK")
        assertThat(response.status).isEqualTo(Http.Status.HTTP_OK)
        assertThat(Http.parseDate(response.getHeader(Http.DATE))).isEqualTo(DATE)
        assertThat(response.body).isEqualTo(TestUtils.getResourceAsString("cds.xml"))
    }

    @Test
    fun HttpRequest_ディープコピーができる() {
        val response1 = HttpResponse.create()
        response1.readData(TestUtils.getResourceAsStream("cds-length.bin"))

        val response2 = HttpResponse.copy(response1)
        assertThat(response1.startLine).isEqualTo(response2.startLine)
        assertThat(response1.status).isEqualTo(response2.status)
        assertThat(response1.getHeader(Http.DATE)).isEqualTo(response2.getHeader(Http.DATE))
        assertThat(response1.body).isEqualTo(response2.body)
        assertThat(response1.bodyBinary).isEqualTo(response2.bodyBinary)

        response1.bodyBinary!![0] = 0
        assertThat(response1.bodyBinary).isNotEqualTo(response2.bodyBinary)
    }

    @Test
    fun readData_Chunk読み出しができること() {
        val response = HttpResponse.create()
        response.readData(TestUtils.getResourceAsStream("cds-chunked.bin"))

        assertThat(response.startLine).isEqualTo("HTTP/1.1 200 OK")
        assertThat(response.status).isEqualTo(Http.Status.HTTP_OK)
        assertThat(Http.parseDate(response.getHeader(Http.DATE))).isEqualTo(DATE)
        assertThat(response.body).isEqualTo(TestUtils.getResourceAsString("cds.xml"))
    }

    @Test
    fun readData_Chunk読み出しができること2() {
        val response = HttpResponse.create()
        response.readData(TestUtils.getResourceAsStream("cds-chunked-large.bin"))

        assertThat(response.startLine).isEqualTo("HTTP/1.1 200 OK")
        assertThat(response.status).isEqualTo(Http.Status.HTTP_OK)
        assertThat(Http.parseDate(response.getHeader(Http.DATE))).isEqualTo(DATE)
        assertThat(response.body).isEqualTo(TestUtils.getResourceAsString("cds.xml"))
    }

    @Test(expected = IOException::class)
    fun readData_読み出せない場合IOException() {
        val data = "\n"
        HttpResponse.create().readData(ByteArrayInputStream(data.toByteArray()))
    }

    @Test(expected = IOException::class)
    fun readData_status_line異常の場合IOException() {
        val data = "HTTP/1.1 200"
        HttpResponse.create().readData(ByteArrayInputStream(data.toByteArray()))
    }

    @Test(expected = IOException::class)
    fun readData_size異常の場合IOException() {
        val data = "HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\n  "
        HttpResponse.create().readData(ByteArrayInputStream(data.toByteArray()))
    }

    @Test(expected = IOException::class)
    fun readData_chunk_sizeなしの場合IOException() {
        val data = "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n\r\n"
        HttpResponse.create().readData(ByteArrayInputStream(data.toByteArray()))
    }

    @Test(expected = IOException::class)
    fun readData_chunk_sizeが16進数でない場合IOException() {
        val data = "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\ngg\r\n"
        HttpResponse.create().readData(ByteArrayInputStream(data.toByteArray()))
    }

    @Test(expected = IOException::class)
    fun readData_chunk_sizeよりデータが少ない場合IOException() {
        val data = "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n10\r\n  \r\n"
        HttpResponse.create().readData(ByteArrayInputStream(data.toByteArray()))
    }

    @Test(expected = IOException::class)
    fun readData_最後が0で終わっていない場合IOException2() {
        val data = "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n2\r\n  \r\n"
        HttpResponse.create().readData(ByteArrayInputStream(data.toByteArray()))
    }

    @Test
    fun writeData_書き出しができること() {
        val data = TestUtils.getResourceAsString("cds.xml")
        val response = HttpResponse.create().apply {
            setStatus(Http.Status.HTTP_OK)
            setHeader(Http.SERVER, Property.SERVER_VALUE)
            setHeader(Http.DATE, Http.formatDate(System.currentTimeMillis()))
            setHeader(Http.CONNECTION, Http.CLOSE)
            setBody(data, true)
        }

        val baos = ByteArrayOutputStream()
        response.writeData(baos)

        val bais = ByteArrayInputStream(baos.toByteArray())
        val readResponse = HttpResponse.create()
        readResponse.readData(bais)

        assertThat(readResponse.startLine).isEqualTo(response.startLine)
        assertThat(readResponse.body).isEqualTo(response.body)
    }

    @Test
    fun writeData_Chunk書き出しができること() {
        val data = TestUtils.getResourceAsString("cds.xml")
        val response = HttpResponse.create().apply {
            setStatus(Http.Status.HTTP_OK)
            setHeader(Http.SERVER, Property.SERVER_VALUE)
            setHeader(Http.DATE, Http.formatDate(System.currentTimeMillis()))
            setHeader(Http.CONNECTION, Http.CLOSE)
            setHeader(Http.TRANSFER_ENCODING, Http.CHUNKED)
            setBody(data, false)
        }
        val baos = ByteArrayOutputStream()
        response.writeData(baos)

        val bais = ByteArrayInputStream(baos.toByteArray())
        val readResponse = HttpResponse.create()
        readResponse.readData(bais)

        assertThat(readResponse.startLine).isEqualTo(response.startLine)
        assertThat(readResponse.body).isEqualTo(response.body)
    }

    @Test
    fun setStatusLine_version_status_phraseに反映される() {
        val response = HttpResponse.create()
        response.setStartLine("HTTP/1.1 200 OK")

        assertThat(response.version).isEqualTo(Http.HTTP_1_1)
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.reasonPhrase).isEqualTo("OK")
        assertThat(response.status).isEqualTo(Http.Status.HTTP_OK)
    }

    @Test
    fun setStatusLine_version_status_phraseに反映される2() {
        val response = HttpResponse.create()
        response.setStartLine("HTTP/1.1 404 Not Found")

        assertThat(response.version).isEqualTo(Http.HTTP_1_1)
        assertThat(response.statusCode).isEqualTo(404)
        assertThat(response.reasonPhrase).isEqualTo("Not Found")
        assertThat(response.status).isEqualTo(Http.Status.HTTP_NOT_FOUND)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setStatusLine_不足がある場合Exception() {
        val response = HttpResponse.create()
        response.setStartLine("HTTP/1.1 404")
    }

    @Test
    fun setStatusCode_正常系() {
        val response = HttpResponse.create()
        response.setStatusCode(200)

        assertThat(response.status).isEqualTo(Http.Status.HTTP_OK)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setStatusCode_不正なステータスコードはException() {
        val response = HttpResponse.create()
        response.setStatusCode(0)
    }

    companion object {
        private val DATE: Date = Calendar.getInstance().also {
            it.timeZone = TimeZone.getTimeZone("GMT")
            it.set(2017, Calendar.JANUARY, 1, 0, 0, 0)
            it.set(Calendar.MILLISECOND, 0)
        }.time
    }
}
