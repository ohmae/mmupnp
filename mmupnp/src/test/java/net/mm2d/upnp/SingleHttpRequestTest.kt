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
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URL

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class SingleHttpRequestTest {
    @Test
    fun setMethod_getMethodに反映される() {
        val request = SingleHttpRequest.create()
        request.setMethod(Http.GET)
        assertThat(request.getMethod()).isEqualTo(Http.GET)
    }

    @Test
    fun setUrl_getAddress_getPort_getUriに反映される() {
        val request = SingleHttpRequest.create()
        request.setUrl(URL("http://192.0.2.2:12345/cds/control"), true)
        val port = 12345
        val address = InetAddress.getByName("192.0.2.2")
        val socketAddress = InetSocketAddress(address, port)
        assertThat(request.getAddressString()).isEqualTo("192.0.2.2:12345")
        assertThat(request.getHeader(Http.HOST)).isEqualTo("192.0.2.2:12345")
        assertThat(request.address).isEqualTo(address)
        assertThat(request.port).isEqualTo(port)
        assertThat(request.getSocketAddress()).isEqualTo(socketAddress)
        assertThat(request.getUri()).isEqualTo("/cds/control")
    }

    @Test
    fun setUrl_getAddress_getPort_getUriに反映される1() {
        val request = SingleHttpRequest.create()
        request.setUrl(URL("http://[2001:db8::1]:12345/cds/control"), true)
        val port = 12345
        val address = InetAddress.getByName("2001:db8::1")
        val socketAddress = InetSocketAddress(address, port)
        assertThat(request.getAddressString()).isEqualTo("[2001:db8::1]:12345")
        assertThat(request.getHeader(Http.HOST)).isEqualTo("[2001:db8::1]:12345")
        assertThat(request.address).isEqualTo(address)
        assertThat(request.port).isEqualTo(port)
        assertThat(request.getSocketAddress()).isEqualTo(socketAddress)
        assertThat(request.getUri()).isEqualTo("/cds/control")
    }

    @Test(expected = IOException::class)
    fun setUrl_http以外はException() {
        SingleHttpRequest.create().setUrl(URL("https://192.0.2.2:12345/cds/control"), true)
    }

    @Test(expected = IOException::class)
    fun setUrl_portがUnsignedShortMax以上ならException() {
        SingleHttpRequest.create().setUrl(URL("http://192.0.2.2:65536/cds/control"), true)
    }

    @Test
    fun setUrl_falseではHOSTに反映されない() {
        val request = SingleHttpRequest.create()
        request.setUrl(URL("http://192.0.2.2:12345/cds/control"))
        assertThat(request.getHeader(Http.HOST)).isNull()
    }

    @Test
    fun readData_読み込めること() {
        val inputStream = TestUtils.getResourceAsStream("browse-request-length.bin")
        val request = SingleHttpRequest.create()
        request.readData(inputStream)
        assertThat(request.getMethod()).isEqualTo(Http.POST)
        assertThat(request.getUri()).isEqualTo("/cds/control")
        assertThat(request.getHeader(Http.SOAPACTION)).isEqualTo(ACTION)
        assertThat(request.getHeader(Http.CONNECTION)).isEqualTo(Http.CLOSE)
        assertThat(request.getHeader(Http.CONTENT_TYPE)).isEqualTo(Http.CONTENT_TYPE_DEFAULT)
        assertThat(request.getBody()).isEqualTo(TestUtils.getResourceAsString("browse-request.xml"))
    }

    @Test
    fun readData_Chunk読み込めること() {
        val inputStream = TestUtils.getResourceAsStream("browse-request-chunked.bin")
        val request = SingleHttpRequest.create()
        request.readData(inputStream)
        assertThat(request.getMethod()).isEqualTo(Http.POST)
        assertThat(request.getUri()).isEqualTo("/cds/control")
        assertThat(request.getHeader(Http.SOAPACTION)).isEqualTo(ACTION)
        assertThat(request.getHeader(Http.CONNECTION)).isEqualTo(Http.CLOSE)
        assertThat(request.getHeader(Http.CONTENT_TYPE)).isEqualTo(Http.CONTENT_TYPE_DEFAULT)
        assertThat(request.getHeader(Http.TRANSFER_ENCODING)).isEqualTo(Http.CHUNKED)
        assertThat(request.getBody()).isEqualTo(TestUtils.getResourceAsString("browse-request.xml"))
    }

    @Test
    fun writeData_書き出しができること() {
        val soap = TestUtils.getResourceAsString("browse-request.xml")
        val request = SingleHttpRequest.create().apply {
            setMethod(Http.POST)
            setUrl(URL("http://192.0.2.2:12345/cds/control"), true)
            setHeader(Http.SOAPACTION, ACTION)
            setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
            setHeader(Http.CONNECTION, Http.CLOSE)
            setHeader(Http.CONTENT_TYPE, Http.CONTENT_TYPE_DEFAULT)
            setBody(soap, true)
        }
        val baos = ByteArrayOutputStream()
        request.writeData(baos)

        val bais = ByteArrayInputStream(baos.toByteArray())
        val readRequest = SingleHttpRequest.create()
        readRequest.readData(bais)

        assertThat(readRequest.startLine).isEqualTo(request.startLine)
        assertThat(readRequest.getBody()).isEqualTo(request.getBody())
    }

    @Test
    fun writeData_Chunk書き出しができること() {
        val soap = TestUtils.getResourceAsString("browse-request.xml")
        val request = SingleHttpRequest.create().apply {
            setMethod(Http.POST)
            setUrl(URL("http://192.0.2.2:12345/cds/control"), true)
            setHeader(Http.SOAPACTION, ACTION)
            setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
            setHeader(Http.CONNECTION, Http.CLOSE)
            setHeader(Http.CONTENT_TYPE, Http.CONTENT_TYPE_DEFAULT)
            setHeader(Http.TRANSFER_ENCODING, Http.CHUNKED)
            setBody(soap, false)
        }
        val baos = ByteArrayOutputStream()
        request.writeData(baos)

        val bais = ByteArrayInputStream(baos.toByteArray())
        val readRequest = SingleHttpRequest.create()
        readRequest.readData(bais)

        assertThat(readRequest.startLine).isEqualTo(request.startLine)
        assertThat(readRequest.getBody()).isEqualTo(request.getBody())
    }

    @Test
    fun setVersion_getVersionで取得できる() {
        val request = SingleHttpRequest.create()
        request.setVersion(Http.HTTP_1_0)

        assertThat(request.version).isEqualTo(Http.HTTP_1_0)
    }

    @Test
    fun setRequestLine_method_uri_versionに反映される() {
        val request = SingleHttpRequest.create()
        request.setStartLine("GET /cds/control HTTP/1.1")

        assertThat(request.getMethod()).isEqualTo(Http.GET)
        assertThat(request.getUri()).isEqualTo("/cds/control")
        assertThat(request.version).isEqualTo(Http.HTTP_1_1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setRequestLine_不足があればException() {
        val request = SingleHttpRequest.create()
        request.setStartLine("GET /cds/control")
    }

    @Test
    fun HttpRequest_ディープコピーができる() {
        val inputStream = TestUtils.getResourceAsStream("browse-request-length.bin")
        val request = SingleHttpRequest.create()
        request.readData(inputStream)

        val readRequest = SingleHttpRequest.copy(request)
        assertThat(readRequest.getMethod()).isEqualTo(request.getMethod())
        assertThat(readRequest.getUri()).isEqualTo(request.getUri())
        assertThat(readRequest.getHeader(Http.SOAPACTION)).isEqualTo(request.getHeader(Http.SOAPACTION))
        assertThat(readRequest.getHeader(Http.CONNECTION)).isEqualTo(request.getHeader(Http.CONNECTION))
        assertThat(readRequest.getHeader(Http.CONTENT_TYPE)).isEqualTo(request.getHeader(Http.CONTENT_TYPE))
        assertThat(readRequest.getBody()).isEqualTo(request.getBody())

        readRequest.setMethod(Http.GET)
        assertThat(request.getMethod()).isEqualTo(Http.POST)
        assertThat(readRequest.getMethod()).isEqualTo(Http.GET)
    }

    @Test(expected = IllegalStateException::class)
    fun getAddressString_未設定ならException() {
        val request = SingleHttpRequest.create()
        request.getAddressString()
    }

    @Test
    fun getAddressString_IPv4() {
        val address = "192.168.0.1"
        val port = 8080
        val request = SingleHttpRequest.create()
        request.address = InetAddress.getByName(address)
        assertThat(request.getAddressString()).isEqualTo(address)
        request.port = Http.DEFAULT_PORT
        assertThat(request.getAddressString()).isEqualTo(address)
        request.port = port
        assertThat(request.getAddressString()).isEqualTo("$address:$port")
    }

    @Test
    fun getAddressString_IPv6() {
        val address = "::1"
        val port = 8080
        val request = SingleHttpRequest.create()
        request.address = InetAddress.getByName(address)
        assertThat(request.getAddressString()).isEqualTo("[$address]")
        request.port = Http.DEFAULT_PORT
        assertThat(request.getAddressString()).isEqualTo("[$address]")
        request.port = port
        assertThat(request.getAddressString()).isEqualTo("[$address]:$port")
    }

    @Test(expected = IllegalStateException::class)
    fun getSocketAddress_未設定ならException() {
        val request = SingleHttpRequest.create()
        request.getSocketAddress()
    }

    @Test
    fun setHeaderLine_設定できる() {
        val request = SingleHttpRequest.create()
        request.setHeaderLine("SOAPACTION: $ACTION")
        assertThat(request.getHeader(Http.SOAPACTION)).isEqualTo(ACTION)
    }

    @Test
    fun setHeaderLine_フォーマットエラーでもExceptionは発生しない() {
        val request = SingleHttpRequest.create()
        request.setHeaderLine("SOAPACTION")
        assertThat(request.getHeader(Http.SOAPACTION)).isNull()
    }

    @Test
    fun isKeepAlive() {
        val request = SingleHttpRequest.create()
        request.setVersion(Http.HTTP_1_0)
        assertThat(request.isKeepAlive()).isFalse()
        request.setVersion(Http.HTTP_1_1)
        assertThat(request.isKeepAlive()).isTrue()

        request.setHeader(Http.CONNECTION, Http.KEEP_ALIVE)
        request.setVersion(Http.HTTP_1_0)
        assertThat(request.isKeepAlive()).isTrue()
        request.setVersion(Http.HTTP_1_1)
        assertThat(request.isKeepAlive()).isTrue()

        request.setHeader(Http.CONNECTION, Http.CLOSE)
        request.setVersion(Http.HTTP_1_0)
        assertThat(request.isKeepAlive()).isFalse()
        request.setVersion(Http.HTTP_1_1)
        assertThat(request.isKeepAlive()).isFalse()
    }

    @Test
    fun getContentLength_正常系() {
        val request = SingleHttpRequest.create()
        request.setHeader(Http.CONTENT_LENGTH, "10")
        assertThat(request.contentLength).isEqualTo(10)
    }

    @Test
    fun getContentLength_異常系() {
        val request = SingleHttpRequest.create()
        request.setHeader(Http.CONTENT_LENGTH, "length")
        assertThat(request.contentLength).isEqualTo(-1)
    }

    @Test
    fun setBody_getBodyで取得できる() {
        val body = "body"
        val request = SingleHttpRequest.create()
        request.setBody(body)
        assertThat(request.getBody()).isEqualTo(body)
    }

    @Test
    fun setBody_長さ0() {
        val body = ""
        val request = SingleHttpRequest.create()
        request.setBody(body, true)
        assertThat(request.getBody()).isEqualTo(body)
        assertThat(request.getBodyBinary()!!.size).isEqualTo(0)
        assertThat(request.getHeader(Http.CONTENT_LENGTH)).isEqualTo("0")
    }

    @Test
    fun setBodyBinary_長さ0() {
        val request = SingleHttpRequest.create()
        request.setBodyBinary(ByteArray(0))
        assertThat(request.getBody()!!.length).isEqualTo(0)
        assertThat(request.getBodyBinary()!!.size).isEqualTo(0)
    }

    companion object {
        private const val ACTION = "\"urn:schemas-upnp-org:service:ContentDirectory:1#Browse\""
    }
}
