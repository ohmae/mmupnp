/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import net.mm2d.upnp.Action
import net.mm2d.upnp.Http
import net.mm2d.upnp.SingleHttpClient
import net.mm2d.upnp.SingleHttpRequest
import net.mm2d.upnp.SingleHttpResponse
import net.mm2d.upnp.internal.thread.TaskExecutors
import net.mm2d.xml.parser.XmlParser
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException
import java.net.URL
import java.util.*

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class ActionInvokeTest {
    private lateinit var httpResponse: SingleHttpResponse
    private lateinit var url: URL
    private lateinit var action: ActionImpl
    private lateinit var invokeDelegate: ActionInvokeDelegate
    private lateinit var mockHttpClient: SingleHttpClient

    @Before
    fun setUp() {
        mockkObject(ActionImpl.Companion)
        every { ActionImpl.createInvokeDelegate(any()) } answers { spyk(ActionInvokeDelegate(arg(0))) }
        url = URL("http://127.0.0.1:8888/test")
        val service: ServiceImpl = mockk(relaxed = true)
        every { service.serviceType } returns SERVICE_TYPE
        every { service.controlUrl } returns ""
        every { service.device.controlPoint.taskExecutors } returns TaskExecutors()
        action = ActionImpl.Builder()
            .setService(service)
            .setName(ACTION_NAME)
            .addArgumentBuilder(
                ArgumentImpl.Builder()
                    .setName(IN_ARG_NAME_1)
                    .setDirection("in")
                    .setRelatedStateVariableName("1")
                    .setRelatedStateVariable(
                        StateVariableImpl.Builder()
                            .setDataType("string")
                            .setName("1")
                            .build()
                    )
            )
            .addArgumentBuilder(
                ArgumentImpl.Builder()
                    .setName(IN_ARG_NAME_2)
                    .setDirection("in")
                    .setRelatedStateVariableName("2")
                    .setRelatedStateVariable(
                        StateVariableImpl.Builder()
                            .setDataType("string")
                            .setName("2")
                            .setDefaultValue(IN_ARG_DEFAULT_VALUE)
                            .build()
                    )
            )
            .addArgumentBuilder(
                ArgumentImpl.Builder()
                    .setName(OUT_ARG_NAME1)
                    .setDirection("out")
                    .setRelatedStateVariableName("3")
                    .setRelatedStateVariable(
                        StateVariableImpl.Builder()
                            .setDataType("string")
                            .setName("3")
                            .build()
                    )
            )
            .build()
        invokeDelegate = action.invokeDelegate
        every { invokeDelegate.makeAbsoluteControlUrl() } returns url
        mockHttpClient = spyk(SingleHttpClient())
        httpResponse = SingleHttpResponse.create()
        httpResponse.setStatus(Http.Status.HTTP_OK)
        httpResponse.setBody(
            """
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
            <s:Body>
            <u:${ACTION_NAME}Response xmlns:u="$SERVICE_TYPE">
            <$OUT_ARG_NAME1>$OUT_ARG_VALUE1</$OUT_ARG_NAME1>
            </u:${ACTION_NAME}Response>
            </s:Body>
            </s:Envelope>""".trimIndent()
        )
        mockkObject(SingleHttpClient.Companion)
        every { SingleHttpClient.create(false) } returns mockHttpClient
    }

    @After
    fun teardown() {
        unmockkObject(SingleHttpClient.Companion)
        unmockkObject(ActionImpl.Companion)
    }

    @Test(expected = IOException::class)
    fun invokeSync_postでIOExceptionが発生() {
        val client: SingleHttpClient = mockk(relaxed = true)
        every { client.post(any()) } throws IOException()
        every { SingleHttpClient.create(any()) } returns client
        runBlocking {
            action.invoke(emptyMap())
        }
    }

    @Test
    fun invokeSync_リクエストヘッダの確認() {
        val slot = slot<SingleHttpRequest>()
        every { mockHttpClient.post(capture(slot)) } returns httpResponse
        runBlocking {
            action.invoke(emptyMap())
        }

        val request = slot.captured
        assertThat(request.getMethod()).isEqualTo("POST")
        assertThat(request.getUri()).isEqualTo(url.path)
        assertThat(request.version).isEqualTo("HTTP/1.1")
        assertThat(request.getHeader(Http.HOST)).isEqualTo(url.host + ":" + url.port)
        assertThat<String>(request.getHeader(Http.CONTENT_LENGTH))
            .isEqualTo(request.getBodyBinary()?.size.toString())
    }

    @Test
    fun invokeSync_リクエストSOAPフォーマットの確認() {
        val slot = slot<SingleHttpRequest>()
        every { mockHttpClient.post(capture(slot)) } returns httpResponse
        runBlocking {
            action.invoke(emptyMap())
        }

        val request = slot.captured

        val envelope = XmlParser.parse(request.getBody()!!)!!

        assertThat(envelope.localName).isEqualTo("Envelope")
        assertThat(envelope.uri).isEqualTo(SOAP_NS)
        assertThat(envelope.getAttributeNSValue(SOAP_NS, "encodingStyle")).isEqualTo(SOAP_STYLE)

        val body = envelope.childElements.find { it.localName == "Body" }!!

        assertThat(body.uri).isEqualTo(SOAP_NS)

        val action = body.childElements.find { it.localName == ACTION_NAME }!!

        assertThat(action.uri).isEqualTo(SERVICE_TYPE)
    }

    @Test
    fun invokeSync_リクエストSOAPの引数確認_指定なしでの実行() {
        val slot = slot<SingleHttpRequest>()
        every { mockHttpClient.post(capture(slot)) } returns httpResponse
        runBlocking {
            action.invoke(emptyMap())
        }
        val request = slot.captured

        val envelope = XmlParser.parse(request.getBody()!!)!!
        val body = envelope.childElements.find { it.localName == "Body" }!!
        val action = body.childElements.find { it.localName == ACTION_NAME }!!

        assertThat(action.childElements).hasSize(2)
        assertThat(action.childElements[0].localName).isEqualTo(IN_ARG_NAME_1)
        assertThat(action.childElements[0].value).isEqualTo("")

        assertThat(action.childElements[1].localName).isEqualTo(IN_ARG_NAME_2)
        assertThat(action.childElements[1].value).isEqualTo(IN_ARG_DEFAULT_VALUE)
    }

    @Test
    fun invokeSync_リクエストSOAPの引数確認_指定ありでの実行() {
        val value1 = "value1"
        val value2 = "value2"
        val slot = slot<SingleHttpRequest>()
        every { mockHttpClient.post(capture(slot)) } returns httpResponse
        val argument = mapOf(
            IN_ARG_NAME_1 to value1,
            IN_ARG_NAME_2 to value2
        )
        runBlocking {
            action.invoke(argument)
        }
        val request = slot.captured

        val envelope = XmlParser.parse(request.getBody()!!)!!
        val body = envelope.childElements.find { it.localName == "Body" }!!
        val action = body.childElements.find { it.localName == ACTION_NAME }!!

        assertThat(action.childElements).hasSize(2)
        assertThat(action.childElements[0].localName).isEqualTo(IN_ARG_NAME_1)
        assertThat(action.childElements[0].value).isEqualTo(value1)

        assertThat(action.childElements[1].localName).isEqualTo(IN_ARG_NAME_2)
        assertThat(action.childElements[1].value).isEqualTo(value2)
    }

    @Test
    fun invokeSync_リクエストSOAPの引数確認_カスタム引数指定NSなし() {
        val value1 = "value1"
        val value2 = "value2"
        val name = "name"
        val value = "value"
        val slot = slot<SingleHttpRequest>()
        every { mockHttpClient.post(capture(slot)) } returns httpResponse
        val argument = mapOf(
            IN_ARG_NAME_1 to value1,
            IN_ARG_NAME_2 to value2
        )
        runBlocking {
            action.invokeCustom(argument, customArguments = Collections.singletonMap(name, value))
        }
        val request = slot.captured

        val envelope = XmlParser.parse(request.getBody()!!)!!
        val body = envelope.childElements.find { it.localName == "Body" }!!
        val action = body.childElements.find { it.localName == ACTION_NAME }!!

        assertThat(action.childElements.size).isEqualTo(3)
        assertThat(action.childElements[0].localName).isEqualTo(IN_ARG_NAME_1)
        assertThat(action.childElements[0].value).isEqualTo(value1)

        assertThat(action.childElements[1].localName).isEqualTo(IN_ARG_NAME_2)
        assertThat(action.childElements[1].value).isEqualTo(value2)

        assertThat(action.childElements[2].localName).isEqualTo(name)
        assertThat(action.childElements[2].value).isEqualTo(value)
    }

    @Test
    fun invokeSync_リクエストSOAPの引数確認_カスタム引数指定NSあり() {
        val value1 = "value1"
        val value2 = "value2"
        val prefix = "custom"
        val urn = "urn:schemas-custom-com:custom"
        val name = "name"
        val value = "value"
        val slot = slot<SingleHttpRequest>()
        every { mockHttpClient.post(capture(slot)) } returns httpResponse
        val argument = mapOf(
            IN_ARG_NAME_1 to value1,
            IN_ARG_NAME_2 to value2
        )

        runBlocking {
            action.invokeCustom(
                argument,
                customNamespace = Collections.singletonMap(prefix, urn),
                customArguments = Collections.singletonMap("$prefix:$name", value)
            )
        }
        val request = slot.captured

        val envelope = XmlParser.parse(request.getBody()!!)!!
        val body = envelope.childElements.find { it.localName == "Body" }!!
        val action = body.childElements.find { it.localName == ACTION_NAME }!!

        assertThat(action.childElements.size).isEqualTo(3)
        assertThat(action.childElements[0].localName).isEqualTo(IN_ARG_NAME_1)
        assertThat(action.childElements[0].value).isEqualTo(value1)

        assertThat(action.childElements[1].localName).isEqualTo(IN_ARG_NAME_2)
        assertThat(action.childElements[1].value).isEqualTo(value2)

        assertThat(action.childElements[2].localName).isEqualTo(name)
        assertThat(action.childElements[2].value).isEqualTo(value)
    }

    @Test
    fun invokeSync_200以外のレスポンスでIOExceptionが発生() {
        var statusCount = 0
        var exceptionCount = 0
        for (status in Http.Status.values()) {
            httpResponse.setStatus(status)
            every { mockHttpClient.post(any()) } returns httpResponse
            if (status === Http.Status.HTTP_OK) {
                runBlocking {
                    action.invoke(emptyMap())
                }
                continue
            }
            try {
                statusCount++
                runBlocking {
                    action.invoke(emptyMap())
                }
            } catch (ignored: IOException) {
                exceptionCount++
            }
        }
        assertThat(statusCount).isEqualTo(exceptionCount)
    }

    @Test(expected = IOException::class)
    fun invokeSync_BodyタグがないとIOExceptionが発生() {
        httpResponse.setBody(
            """
            |<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
            |</s:Envelope>
            """.trimIndent()
        )
        every { mockHttpClient.post(any()) } returns httpResponse

        runBlocking {
            action.invoke(emptyMap(), false)
        }
    }

    @Test(expected = IOException::class)
    fun invokeSync_ActionタグがないとIOExceptionが発生() {
        httpResponse.setBody(
            """
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                </s:Body>
                </s:Envelope>
                """.trimIndent()
        )
        every { mockHttpClient.post(any()) } returns httpResponse

        runBlocking {
            action.invoke(emptyMap(), false)
        }
    }

    @Test
    fun invokeSync_実行結果をパースしMapとして戻ること() {
        every { mockHttpClient.post(any()) } returns httpResponse
        val result = runBlocking {
            action.invoke(emptyMap())
        }
        assertThat(result[OUT_ARG_NAME1]).isEqualTo(OUT_ARG_VALUE1)
    }

    @Test
    fun invokeSync_argumentListにない結果が含まれていても結果に含まれる() {
        httpResponse.setBody(
            """
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
            <s:Body>
            <u:${ACTION_NAME}Response xmlns:u="$SERVICE_TYPE">
            <$OUT_ARG_NAME1>$OUT_ARG_VALUE1</$OUT_ARG_NAME1>
            <$OUT_ARG_NAME2>$OUT_ARG_VALUE2</$OUT_ARG_NAME2>
            </u:${ACTION_NAME}Response>
            </s:Body>
            </s:Envelope>
            """.trimIndent()
        )
        every { mockHttpClient.post(any()) } returns httpResponse
        val result = runBlocking {
            action.invoke(emptyMap())
        }
        assertThat(result[OUT_ARG_NAME1]).isEqualTo(OUT_ARG_VALUE1)
        assertThat(result).containsKey(OUT_ARG_NAME2)
    }

    @Test(expected = IOException::class)
    fun invokeSync_エラーレスポンスのときIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody(ERROR_RESPONSE)
        every { mockHttpClient.post(any()) } returns httpResponse
        runBlocking {
            action.invoke(emptyMap(), false)
        }
    }

    @Test(expected = IOException::class)
    fun invokeSync_エラーレスポンスにerrorCodeがないとIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody(
            """
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
            <s:Body>
            <s:Fault>
            <faultcode>s:Client</faultcode>
            <faultstring>UPnPError</faultstring>
            <detail>
            <UPnPError xmlns="urn:schemas-upnp-org:control-1-0">
            <errorDescription>Restricted object</errorDescription>
            </UPnPError>
            </detail>
            </s:Fault>
            </s:Body>
            </s:Envelope>
            """.trimIndent()
        )
        every { mockHttpClient.post(any()) } returns httpResponse
        runBlocking {
            action.invoke(emptyMap(), true)
        }
    }

    @Test(expected = IOException::class)
    fun invokeSync_エラーレスポンスにUPnPErrorがないとIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody(
            """
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                <s:Fault>
                <faultcode>s:Client</faultcode>
                <faultstring>UPnPError</faultstring>
                <detail>
                </detail>
                </s:Fault>
                </s:Body>
                </s:Envelope>
                """.trimIndent()
        )
        every { mockHttpClient.post(any()) } returns httpResponse
        runBlocking {
            action.invoke(emptyMap(), true)
        }
    }

    @Test(expected = IOException::class)
    fun invokeSync_エラーレスポンスにBodyがないとIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody(
            """
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
            </s:Envelope>
            """.trimIndent()
        )
        every { mockHttpClient.post(any()) } returns httpResponse
        runBlocking {
            action.invoke(emptyMap(), true)
        }
    }

    @Test(expected = IOException::class)
    fun invokeSync_エラーレスポンスにFaultがないとIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody(
            """
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
            <s:Body>
            </s:Body>
            </s:Envelope>
            """.trimIndent()
        )
        every { mockHttpClient.post(any()) } returns httpResponse
        runBlocking {
            action.invoke(emptyMap(), true)
        }
    }

    @Test
    fun invokeSync_エラーレスポンスもパースできる() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody(ERROR_RESPONSE)
        every { mockHttpClient.post(any()) } returns httpResponse
        val result = runBlocking {
            action.invoke(emptyMap(), true)
        }
        assertThat(result[Action.FAULT_CODE_KEY]).isEqualTo("s:Client")
        assertThat(result[Action.FAULT_STRING_KEY]).isEqualTo("UPnPError")
        assertThat(result[Action.ERROR_CODE_KEY]).isEqualTo("711")
        assertThat(result[Action.ERROR_DESCRIPTION_KEY]).isEqualTo("Restricted object")
    }

    @Test(expected = IOException::class)
    fun invokeSync_エラーレスポンスのときIOExceptionが発生2() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody(ERROR_RESPONSE)
        every { mockHttpClient.post(any()) } returns httpResponse
        runBlocking {
            action.invoke(emptyMap())
        }
    }

    @Test
    fun invokeSync_エラーレスポンスもパースできる2() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody(ERROR_RESPONSE)
        every { mockHttpClient.post(any()) } returns httpResponse
        val result = runBlocking {
            action.invoke(emptyMap(), true)
        }
        assertThat(result[Action.FAULT_CODE_KEY]).isEqualTo("s:Client")
        assertThat(result[Action.FAULT_STRING_KEY]).isEqualTo("UPnPError")
        assertThat(result[Action.ERROR_CODE_KEY]).isEqualTo("711")
        assertThat(result[Action.ERROR_DESCRIPTION_KEY]).isEqualTo("Restricted object")
    }

    @Test(expected = IOException::class)
    fun invokeSync_ステータスコードがOKで中身が空のときIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_OK)
        httpResponse.setBody("")
        every { mockHttpClient.post(any()) } returns httpResponse
        runBlocking {
            action.invoke(emptyMap(), true)
        }
    }

    @Test(expected = IOException::class)
    fun invokeSync_ステータスコードがエラーで中身が空のときIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody("")
        every { mockHttpClient.post(any()) } returns httpResponse
        runBlocking {
            action.invoke(emptyMap(), true)
        }
    }

    @Test(expected = IOException::class)
    fun invokeSync_ステータスコードがその他で中身が空のときIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_NOT_FOUND)
        httpResponse.setBody("")
        every { mockHttpClient.post(any()) } returns httpResponse
        runBlocking {
            action.invoke(emptyMap(), true)
        }
    }

    @Test(expected = IOException::class)
    fun invokeSync_ステータスコードがOKで中身がxmlとして異常のときIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_OK)
        httpResponse.setBody("<>")
        every { mockHttpClient.post(any()) } returns httpResponse
        runBlocking {
            action.invoke(emptyMap(), true)
        }
    }

    @Test(expected = IOException::class)
    fun invokeSync_ステータスコードがエラーで中身がxmlとして異常のときIOExceptionが発生() {
        httpResponse.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
        httpResponse.setBody("<>")
        every { mockHttpClient.post(any()) } returns httpResponse
        runBlocking {
            action.invoke(emptyMap(), true)
        }
    }

    companion object {
        private const val SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/"
        private const val SOAP_STYLE = "http://schemas.xmlsoap.org/soap/encoding/"
        private const val IN_ARG_NAME_1 = "InArgName1"
        private const val IN_ARG_NAME_2 = "InArgName2"
        private const val IN_ARG_DEFAULT_VALUE = "Default"
        private const val OUT_ARG_NAME1 = "OutArgName1"
        private const val OUT_ARG_VALUE1 = "OutArgValue1"
        private const val OUT_ARG_NAME2 = "OutArgName2"
        private const val OUT_ARG_VALUE2 = "OutArgValue2"
        private const val ACTION_NAME = "TestAction"
        private const val SERVICE_TYPE = "urn:schemas-upnp-org:service:TestServiceType:1"

        private val ERROR_RESPONSE = """
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
            <s:Body>
            <s:Fault>
            <faultcode>s:Client</faultcode>
            <faultstring>UPnPError</faultstring>
            <detail>
            <UPnPError xmlns="urn:schemas-upnp-org:control-1-0">
            <errorCode>711</errorCode>
            <errorDescription>Restricted object</errorDescription>
            </UPnPError>
            </detail>
            </s:Fault>
            </s:Body>
            </s:Envelope>
            """.trimIndent()
    }
}
