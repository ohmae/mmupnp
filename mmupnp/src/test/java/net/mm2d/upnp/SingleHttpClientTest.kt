/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import net.mm2d.upnp.Http.Status
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.URL

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class SingleHttpClientTest {
    @Test
    fun downloadString_KeepAlive有効() {
        val responseBody = "responseBody"
        val server = HttpServerMock()
        server.setServerCore { _, inputStream, outputStream ->
            val request = SingleHttpRequest.create()
            request.readData(inputStream)
            val response = SingleHttpResponse.create()
            response.setStartLine("HTTP/1.1 200 OK")
            response.setBody(responseBody, true)
            if (request.isKeepAlive()) {
                response.setHeader(Http.CONNECTION, Http.KEEP_ALIVE)
                response.writeData(outputStream)
                true
            } else {
                response.setHeader(Http.CONNECTION, Http.CLOSE)
                response.writeData(outputStream)
                false
            }
        }
        server.open()
        val port = server.localPort

        try {
            val client = SingleHttpClient(true)
            assertThat(client.downloadString(URL("http://127.0.0.1:$port/"))).isEqualTo(responseBody)
            assertThat(client.isClosed).isFalse()

            client.isKeepAlive = false
            assertThat(client.downloadString(URL("http://127.0.0.1:$port/"))).isEqualTo(responseBody)
            client.close()
        } finally {
            server.close()
        }
    }

    @Test
    fun downloadBinary_KeepAlive有効() {
        val responseBody = "responseBody".toByteArray(charset("utf-8"))
        val server = HttpServerMock()
        server.setServerCore { _, inputStream, outputStream ->
            val request = SingleHttpRequest.create()
            request.readData(inputStream)
            val response = SingleHttpResponse.create()
            response.setStartLine("HTTP/1.1 200 OK")
            response.setBodyBinary(responseBody, true)
            if (request.isKeepAlive()) {
                response.setHeader(Http.CONNECTION, Http.KEEP_ALIVE)
                response.writeData(outputStream)
                true
            } else {
                response.setHeader(Http.CONNECTION, Http.CLOSE)
                response.writeData(outputStream)
                false
            }
        }
        server.open()
        val port = server.localPort

        try {
            val client = SingleHttpClient(true)
            assertThat(client.downloadBinary(URL("http://127.0.0.1:$port/"))).isEqualTo(responseBody)
            assertThat(client.isClosed).isFalse()
            assertThat(client.localAddress).isEqualTo(InetAddress.getByName("127.0.0.1"))

            client.isKeepAlive = false
            assertThat(client.downloadBinary(URL("http://127.0.0.1:$port/"))).isEqualTo(responseBody)
            client.close()
        } finally {
            server.close()
        }
    }

    @Test
    fun download_KeepAliveなのに切断されても取得にいく() {
        val responseBody = "responseBody"
        val server = HttpServerMock()
        server.setServerCore { _, inputStream, outputStream ->
            val request = SingleHttpRequest.create()
            request.readData(inputStream)
            val response = SingleHttpResponse.create()
            response.setStartLine("HTTP/1.1 200 OK")
            response.setBody(responseBody, true)
            if (request.isKeepAlive()) {
                response.setHeader(Http.CONNECTION, Http.KEEP_ALIVE)
                response.writeData(outputStream)
                false // コネクション切断
            } else {
                response.setHeader(Http.CONNECTION, Http.CLOSE)
                response.writeData(outputStream)
                false
            }
        }
        server.open()
        val port = server.localPort

        try {
            val client = SingleHttpClient(true)
            var response = client.download(URL("http://127.0.0.1:$port/"))
            assertThat(response.getBody()).isEqualTo(responseBody)
            assertThat(client.isKeepAlive).isTrue()
            response = client.download(URL("http://127.0.0.1:$port/"))
            assertThat(response.getBody()).isEqualTo(responseBody)
            assertThat(client.isKeepAlive).isFalse()
            client.close()
        } finally {
            server.close()
        }
    }

    @Test
    fun download_KeepAliveでリクエストしてもcloseが返されたらclose() {
        val responseBody = "responseBody"

        val server = HttpServerMock()
        server.setServerCore { _, inputStream, outputStream ->
            SingleHttpRequest.create().readData(inputStream)
            val response = SingleHttpResponse.create()
            response.setStartLine("HTTP/1.1 200 OK")
            response.setBody(responseBody, true)
            response.setHeader(Http.CONNECTION, Http.CLOSE)
            response.writeData(outputStream)
            false
        }
        server.open()
        val port = server.localPort
        try {
            val client = SingleHttpClient(true)
            val response = client.download(URL("http://127.0.0.1:$port/"))
            assertThat(response.getBody()).isEqualTo(responseBody)
            assertThat(client.isKeepAlive).isTrue()
            assertThat(client.isClosed).isTrue()
            client.close()
        } finally {
            server.close()
        }
    }

    @Test
    fun download_no_content_length() {
        val responseBody = "responseBody"

        val server = HttpServerMock()
        server.setServerCore { _, inputStream, outputStream ->
            SingleHttpRequest.create().readData(inputStream)
            val response = SingleHttpResponse.create()
            response.setStartLine("HTTP/1.1 200 OK")
            response.setBody(responseBody, false)
            response.setHeader(Http.CONNECTION, Http.CLOSE)
            response.writeData(outputStream)
            false
        }
        server.open()
        val port = server.localPort
        try {
            val client = SingleHttpClient(true)
            val response = client.download(URL("http://127.0.0.1:$port/"))
            assertThat(response.getBody()).isEqualTo(responseBody)
            assertThat(client.isKeepAlive).isTrue()
            assertThat(client.isClosed).isTrue()
            client.close()
        } finally {
            server.close()
        }
    }

    @Test
    fun download_no_content_length_in_keep_alive() {
        val responseBody = "responseBody"

        val server = HttpServerMock()
        server.setServerCore { _, inputStream, outputStream ->
            SingleHttpRequest.create().readData(inputStream)
            val response = SingleHttpResponse.create()
            response.setStartLine("HTTP/1.1 200 OK")
            response.setBody(responseBody, false)
            response.setHeader(Http.CONNECTION, Http.KEEP_ALIVE)
            response.writeData(outputStream)
            false
        }
        server.open()
        val port = server.localPort
        try {
            val client = SingleHttpClient(true)
            val response = client.download(URL("http://127.0.0.1:$port/"))
            assertThat(response.getBody()).isEmpty()
            client.close()
        } finally {
            server.close()
        }
    }

    @Test
    fun download_content_length_0() {
        val responseBody = "responseBody"

        val server = HttpServerMock()
        server.setServerCore { _, inputStream, outputStream ->
            SingleHttpRequest.create().readData(inputStream)
            val response = SingleHttpResponse.create()
            response.setStartLine("HTTP/1.1 200 OK")
            response.setBody(responseBody, false)
            response.setHeader(Http.CONTENT_LENGTH, "0")
            response.setHeader(Http.CONNECTION, Http.CLOSE)
            response.writeData(outputStream)
            false
        }
        server.open()
        val port = server.localPort
        try {
            val client = SingleHttpClient(true)
            val response = client.download(URL("http://127.0.0.1:$port/"))
            assertThat(response.getBody()).isEmpty()
            client.close()
        } finally {
            server.close()
        }
    }

    @Test(expected = IOException::class)
    fun download_no_content_length_204() {
        val responseBody = "responseBody"

        val server = HttpServerMock()
        server.setServerCore { _, inputStream, outputStream ->
            SingleHttpRequest.create().readData(inputStream)
            val response = SingleHttpResponse.create()
            response.setStartLine("HTTP/1.1 204 No Content")
            response.setBody(responseBody, false)
            response.setHeader(Http.CONNECTION, Http.CLOSE)
            response.writeData(outputStream)
            false
        }
        server.open()
        val port = server.localPort
        try {
            SingleHttpClient(false).download(URL("http://127.0.0.1:$port/"))
        } finally {
            server.close()
        }
    }

    @Test(timeout = 10000L)
    fun post_Redirectが無限ループしない() {
        val server = HttpServerMock()
        server.open()
        val port = server.localPort
        server.setServerCore { _, inputStream, outputStream ->
            SingleHttpRequest.create().readData(inputStream)
            val response = SingleHttpResponse.create()
            response.setHeader(Http.CONNECTION, Http.CLOSE)
            response.setStartLine("HTTP/1.1 301 Moved Permanently")
            response.setHeader(Http.LOCATION, "http://127.0.0.1:$port/b")
            response.setBody("a", true)
            response.writeData(outputStream)
            false
        }
        try {
            val client = SingleHttpClient(false)
            client.post(SingleHttpRequest.create().apply {
                setMethod(Http.GET)
                setUrl(URL("http://127.0.0.1:$port/a"), true)
                setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
                setHeader(Http.CONNECTION, Http.CLOSE)
            })
            client.close()
        } finally {
            server.close()
        }
    }

    @Test
    fun post_Redirectが動作する() {
        val server = HttpServerMock()
        server.open()
        val port = server.localPort
        server.setServerCore { _, inputStream, outputStream ->
            val request = SingleHttpRequest.create()
            request.readData(inputStream)
            val response = SingleHttpResponse.create()
            response.setHeader(Http.CONNECTION, Http.CLOSE)
            if (request.getUri() == "/b") {
                response.setStartLine("HTTP/1.1 200 OK")
                response.setBody("b", true)
                response.writeData(outputStream)
                false
            } else {
                response.setStartLine("HTTP/1.1 301 Moved Permanently")
                response.setHeader(Http.LOCATION, "http://127.0.0.1:$port/b")
                response.setBody("a", true)
                response.writeData(outputStream)
                false
            }
        }
        try {
            val client = SingleHttpClient(false)
            val response = client.post(SingleHttpRequest.create().apply {
                setMethod(Http.GET)
                setUrl(URL("http://127.0.0.1:$port/a"), true)
                setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
                setHeader(Http.CONNECTION, Http.CLOSE)
            })
            assertThat(response.getBody()).isEqualTo("b")
            client.close()
        } finally {
            server.close()
        }
    }

    @Test
    fun post_Redirectのlocationがなければひとまずそのまま取得する() {
        val server = HttpServerMock()
        server.setServerCore { _, inputStream, outputStream ->
            val request = SingleHttpRequest.create()
            request.readData(inputStream)
            val response = SingleHttpResponse.create()
            response.setHeader(Http.CONNECTION, Http.CLOSE)
            if (request.getUri() == "/b") {
                response.setStartLine("HTTP/1.1 200 OK")
                response.setBody("b", true)
                response.writeData(outputStream)
                false
            } else {
                response.setStartLine("HTTP/1.1 301 Moved Permanently")
                response.setBody("a", true)
                response.writeData(outputStream)
                false
            }
        }
        server.open()
        val port = server.localPort
        try {
            val client = SingleHttpClient(false)
            val response = client.post(SingleHttpRequest.create().apply {
                setMethod(Http.GET)
                setUrl(URL("http://127.0.0.1:$port/a"), true)
                setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
                setHeader(Http.CONNECTION, Http.CLOSE)
            })
            assertThat(response.getStatus()).isEqualTo(Status.HTTP_MOVED_PERM)
            assertThat(response.getBody()).isEqualTo("a")
            client.close()
        } finally {
            server.close()
        }
    }

    @Test(expected = IOException::class)
    fun post_応答がなければException() {
        val server = HttpServerMock()
        server.setServerCore { _, inputStream, _ ->
            SingleHttpRequest.create().readData(inputStream)
            false
        }
        server.open()
        val port = server.localPort
        try {
            val client = SingleHttpClient(false)
            client.post(SingleHttpRequest.create().apply {
                setMethod(Http.GET)
                setUrl(URL("http://127.0.0.1:$port/a"), true)
                setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
                setHeader(Http.CONNECTION, Http.CLOSE)
            })
            assertThat(client.isClosed).isTrue()
            client.close()
        } finally {
            server.close()
        }
    }

    @Test
    fun post_応答がなければcloseしてException() {
        val server = HttpServerMock()
        server.setServerCore { _, inputStream, _ ->
            SingleHttpRequest.create().readData(inputStream)
            false
        }
        server.open()
        val port = server.localPort
        try {
            val client = SingleHttpClient(false)
            try {
                client.post(SingleHttpRequest.create().apply {
                    setMethod(Http.GET)
                    setUrl(URL("http://127.0.0.1:$port/a"), true)
                    setHeader(Http.USER_AGENT, Property.USER_AGENT_VALUE)
                    setHeader(Http.CONNECTION, Http.CLOSE)
                })
            } catch (ignored: IOException) {
            }
            assertThat(client.isClosed).isTrue()
            client.close()
        } finally {
            server.close()
        }
    }

    @Test(expected = IOException::class)
    fun download_HTTP_OKでなければException() {
        val client = spyk(SingleHttpClient())
        val response = SingleHttpResponse.create()
        response.setStartLine("HTTP/1.1 404 Not Found")
        every { client.post(any()) } returns response
        client.download(URL("http://www.example.com/index.html"))
    }

    @Test(expected = IOException::class)
    fun download_bodyがnullならException() {
        val client = spyk(SingleHttpClient())
        val response = SingleHttpResponse.create()
        response.setStartLine("HTTP/1.1 200 OK")
        every { client.post(any()) } returns response
        client.download(URL("http://www.example.com/index.html"))
    }

    @Test
    fun canReuse_初期状態ではfalse() {
        val client = SingleHttpClient()
        val request = SingleHttpRequest.create()
        request.setUrl(URL("http://192.168.0.1/index.html"))
        assertThat(client.canReuse(request)).isFalse()
    }

    @Test
    fun canReuse_接続状態かつアドレスとポートが一致すればtrue() {
        val client = SingleHttpClient()
        val socket: Socket = mockk(relaxed = true)
        every { socket.isConnected } returns true
        every { socket.inetAddress } returns InetAddress.getByName("192.168.0.1")
        every { socket.port } returns 80
        val request = SingleHttpRequest.create()
        request.setUrl(URL("http://192.168.0.1/index.html"))
        assertThat(with(client) { socket.canReuse(request) }).isTrue()
    }

    @Test
    fun canReuse_ポートが不一致ならfalse() {
        val client = SingleHttpClient()
        val socket: Socket = mockk(relaxed = true)
        every { socket.isConnected } returns true
        every { socket.inetAddress } returns InetAddress.getByName("192.168.0.1")
        every { socket.port } returns 80
        val request = SingleHttpRequest.create()
        request.setUrl(URL("http://192.168.0.1:8080/index.html"))
        assertThat(with(client) { socket.canReuse(request) }).isFalse()
    }

    @Test
    fun canReuse_アドレスが不一致ならfalse() {
        val client = SingleHttpClient()
        val socket: Socket = mockk(relaxed = true)
        every { socket.isConnected } returns true
        every { socket.inetAddress } returns InetAddress.getByName("192.168.0.2")
        every { socket.port } returns 80
        val request = SingleHttpRequest.create()
        request.setUrl(URL("http://192.168.0.1/index.html"))
        assertThat(with(client) { socket.canReuse(request) }).isFalse()
    }

    @Test
    fun canReuse_接続状態でなければfalse() {
        val client = SingleHttpClient()
        val socket: Socket = mockk(relaxed = true)
        every { socket.isConnected } returns false
        every { socket.inetAddress } returns InetAddress.getByName("192.168.0.1")
        every { socket.port } returns 80
        val request = SingleHttpRequest.create()
        request.setUrl(URL("http://192.168.0.1/index.html"))
        assertThat(with(client) { socket.canReuse(request) }).isFalse()
    }
}
