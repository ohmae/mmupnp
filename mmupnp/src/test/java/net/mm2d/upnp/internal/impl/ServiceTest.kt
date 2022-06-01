/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import net.mm2d.upnp.Device
import net.mm2d.upnp.Http
import net.mm2d.upnp.SingleHttpClient
import net.mm2d.upnp.SingleHttpRequest
import net.mm2d.upnp.SingleHttpResponse
import net.mm2d.upnp.Property
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.internal.manager.SubscribeManagerImpl
import net.mm2d.upnp.internal.message.SsdpRequest
import net.mm2d.upnp.internal.parser.DeviceParser
import net.mm2d.upnp.internal.thread.TaskExecutors
import net.mm2d.upnp.util.TestUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.TimeUnit

@Suppress("TestFunctionName", "NonAsciiCharacters", "ClassName")
@RunWith(Enclosed::class)
class ServiceTest {
    @RunWith(JUnit4::class)
    class Builderによる生成からのテスト {
        @Test
        fun build_成功() {
            val service = ServiceImpl.Builder()
                .setDevice(mockk(relaxed = true))
                .setServiceType("serviceType")
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .build()

            assertThat(service).isNotNull()
        }

        @Test(expected = IllegalStateException::class)
        fun build_Device不足() {
            ServiceImpl.Builder()
                .setServiceType("serviceType")
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .build()
        }

        @Test(expected = IllegalStateException::class)
        fun build_ServiceType不足() {
            ServiceImpl.Builder()
                .setDevice(mockk(relaxed = true))
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .build()
        }

        @Test(expected = IllegalStateException::class)
        fun build_ServiceId不足() {
            ServiceImpl.Builder()
                .setDevice(mockk(relaxed = true))
                .setServiceType("serviceType")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .build()
        }

        @Test(expected = IllegalStateException::class)
        fun build_ScpdUrl不足() {
            ServiceImpl.Builder()
                .setDevice(mockk(relaxed = true))
                .setServiceType("serviceType")
                .setServiceId("serviceId")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .build()
        }

        @Test(expected = IllegalStateException::class)
        fun build_ControlUrl不足() {
            ServiceImpl.Builder()
                .setDevice(mockk(relaxed = true))
                .setServiceType("serviceType")
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .build()
        }

        @Test(expected = IllegalStateException::class)
        fun build_EventSubUrl不足() {
            ServiceImpl.Builder()
                .setDevice(mockk(relaxed = true))
                .setServiceType("serviceType")
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setDescription("description")
                .build()
        }

        @Test(expected = IllegalStateException::class)
        fun build_argumentのRelatedStateVariableNameが指定されていない() {
            val actionBuilder = ActionImpl.Builder()
                .setName("action")
                .addArgumentBuilder(
                    ArgumentImpl.Builder()
                        .setName("argumentName")
                        .setDirection("in")
                )
            val service = ServiceImpl.Builder()
                .setDevice(mockk(relaxed = true))
                .setServiceType("serviceType")
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .addActionBuilder(actionBuilder)
                .build()

            assertThat(service).isNotNull()
        }

        @Test(expected = IllegalStateException::class)
        fun build_argumentのRelatedStateVariableNameがに対応するStateVariableがない() {
            val actionBuilder = ActionImpl.Builder()
                .setName("action")
                .addArgumentBuilder(
                    ArgumentImpl.Builder()
                        .setName("argumentName")
                        .setDirection("in")
                        .setRelatedStateVariableName("StateVariableName")
                )
            val service = ServiceImpl.Builder()
                .setDevice(mockk(relaxed = true))
                .setServiceType("serviceType")
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .addActionBuilder(actionBuilder)
                .build()

            assertThat(service).isNotNull()
        }

        @Test
        fun getCallback() {
            val cp: ControlPointImpl = mockk(relaxed = true)
            val manager: SubscribeManagerImpl = mockk(relaxed = true)
            every { cp.subscribeManager } returns manager
            val message: SsdpMessage = mockk(relaxed = true)
            every { message.location } returns "location"
            every { message.uuid } returns "uuid"
            val device = DeviceImpl.Builder(cp, message)
                .setDescription("description")
                .setUdn("uuid")
                .setUpc("upc")
                .setDeviceType("deviceType")
                .setFriendlyName("friendlyName")
                .setManufacture("manufacture")
                .setModelName("modelName")
                .build()
            val service = ServiceImpl.Builder()
                .setDevice(device)
                .setServiceType("serviceType")
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .build()
            every { message.localAddress } returns InetAddress.getByName("192.168.0.1")
            every { manager.getEventPort() } returns 80

            assertThat(service.subscribeDelegate.callback).isEqualTo("<http://192.168.0.1/>")

            every { manager.getEventPort() } returns 8080

            assertThat(service.subscribeDelegate.callback).isEqualTo("<http://192.168.0.1:8080/>")
        }

        @Test
        fun hashCode_Exceptionが発生しない() {
            val service = ServiceImpl.Builder()
                .setDevice(mockk(relaxed = true))
                .setServiceType("serviceType")
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .build()
            service.hashCode()
        }

        @Test
        fun equals_比較可能() {
            val service = ServiceImpl.Builder()
                .setDevice(mockk(relaxed = true))
                .setServiceType("serviceType")
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .build()
            assertThat(service == null).isFalse()
            assertThat(service == service).isTrue()
        }

        @Test
        fun equals_同一の情報() {
            val message: SsdpMessage = mockk(relaxed = true)
            every { message.location } returns "location"
            every { message.uuid } returns "uuid"
            val device = DeviceImpl.Builder(mockk(relaxed = true), message)
                .setDescription("description")
                .setUdn("uuid")
                .setUpc("upc")
                .setDeviceType("deviceType")
                .setFriendlyName("friendlyName")
                .setManufacture("manufacture")
                .setModelName("modelName")
                .build()
            val service1 = ServiceImpl.Builder()
                .setDevice(device)
                .setServiceType("serviceType")
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .build()
            val service2 = ServiceImpl.Builder()
                .setDevice(device)
                .setServiceType("serviceType")
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .build()
            assertThat(service1 == service2).isTrue()
        }

        @Test
        @Throws(Exception::class)
        fun equals_不一致を無視() {
            val message: SsdpMessage = mockk(relaxed = true)
            every { message.location } returns "location"
            every { message.uuid } returns "uuid"
            val device = DeviceImpl.Builder(mockk(relaxed = true), message)
                .setDescription("description")
                .setUdn("uuid")
                .setUpc("upc")
                .setDeviceType("deviceType")
                .setFriendlyName("friendlyName")
                .setManufacture("manufacture")
                .setModelName("modelName")
                .build()
            val service1 = ServiceImpl.Builder()
                .setDevice(device)
                .setServiceType("serviceType1")
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl1")
                .setControlUrl("controlUrl1")
                .setEventSubUrl("eventSubUrl1")
                .setDescription("description1")
                .build()
            val service2 = ServiceImpl.Builder()
                .setDevice(device)
                .setServiceType("serviceType2")
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl2")
                .setControlUrl("controlUrl2")
                .setEventSubUrl("eventSubUrl2")
                .setDescription("description2")
                .build()
            assertThat(service1 == service2).isTrue()
        }

        @Test
        fun equals_ServiceId不一致() {
            val message: SsdpMessage = mockk(relaxed = true)
            every { message.location } returns "location"
            every { message.uuid } returns "uuid"
            val device = DeviceImpl.Builder(mockk(relaxed = true), message)
                .setDescription("description")
                .setUdn("uuid")
                .setUpc("upc")
                .setDeviceType("deviceType")
                .setFriendlyName("friendlyName")
                .setManufacture("manufacture")
                .setModelName("modelName")
                .build()
            val service1 = ServiceImpl.Builder()
                .setDevice(device)
                .setServiceType("serviceType")
                .setServiceId("serviceId1")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .build()
            val service2 = ServiceImpl.Builder()
                .setDevice(device)
                .setServiceType("serviceType")
                .setServiceId("serviceId2")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .build()
            assertThat(service1 == service2).isFalse()
        }

        @Test
        fun equals_device不一致() {
            val message1: SsdpMessage = mockk(relaxed = true)
            every { message1.location } returns "location"
            every { message1.uuid } returns "uuid1"
            val device1 = DeviceImpl.Builder(mockk(relaxed = true), message1)
                .setDescription("description")
                .setUdn("uuid1")
                .setUpc("upc")
                .setDeviceType("deviceType")
                .setFriendlyName("friendlyName")
                .setManufacture("manufacture")
                .setModelName("modelName")
                .build()
            val message2: SsdpMessage = mockk(relaxed = true)
            every { message2.location } returns "location"
            every { message2.uuid } returns "uuid2"
            val device2 = DeviceImpl.Builder(mockk(relaxed = true), message2)
                .setDescription("description")
                .setUdn("uuid2")
                .setUpc("upc")
                .setDeviceType("deviceType")
                .setFriendlyName("friendlyName")
                .setManufacture("manufacture")
                .setModelName("modelName")
                .build()
            val service1 = ServiceImpl.Builder()
                .setDevice(device1)
                .setServiceType("serviceType")
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .build()
            val service2 = ServiceImpl.Builder()
                .setDevice(device2)
                .setServiceType("serviceType")
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .build()
            assertThat(service1 == service2).isFalse()
        }

        @Test
        fun toDumpString() {
            ServiceImpl.Builder()
                .setDevice(mockk(relaxed = true))
                .setServiceType("serviceType")
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .toDumpString()
        }
    }

    @RunWith(JUnit4::class)
    class DeviceParserによる生成からのテスト {
        private lateinit var controlPoint: ControlPointImpl
        private lateinit var subscribeManager: SubscribeManagerImpl
        private lateinit var device: Device
        private lateinit var cms: ServiceImpl
        private lateinit var cds: ServiceImpl
        private lateinit var mmupnp: ServiceImpl

        @Before
        fun setUp() {
            val httpClient: SingleHttpClient = mockk(relaxed = true)
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device.xml")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/cds.xml"))
            } returns TestUtils.getResourceAsString("cds.xml")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/cms.xml"))
            } returns TestUtils.getResourceAsString("cms.xml")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/mmupnp.xml"))
            } returns TestUtils.getResourceAsString("mmupnp.xml")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/icon/icon120.jpg"))
            } returns TestUtils.getResourceAsString("icon/icon120.jpg")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/icon/icon48.jpg"))
            } returns TestUtils.getResourceAsString("icon/icon48.jpg")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/icon/icon120.png"))
            } returns TestUtils.getResourceAsString("icon/icon120.png")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/icon/icon48.png"))
            } returns TestUtils.getResourceAsString("icon/icon48.png")

            val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")
            val ssdpMessage = SsdpRequest.create(InetAddress.getByName(INTERFACE_ADDRESS), data, data.size)
            controlPoint = mockk(relaxed = true)
            subscribeManager = mockk(relaxed = true)
            every { subscribeManager.getEventPort() } returns EVENT_PORT
            every { controlPoint.subscribeManager } returns subscribeManager
            val builder = DeviceImpl.Builder(controlPoint, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
            device = builder.build()
            cms = device.findServiceById("urn:upnp-org:serviceId:ConnectionManager") as ServiceImpl
            cds = spyk(device.findServiceById("urn:upnp-org:serviceId:ContentDirectory") as ServiceImpl)
            mmupnp = device.findServiceById("urn:upnp-org:serviceId:X_mmupnp") as ServiceImpl
        }

        @Test
        fun getDevice() {
            assertThat(cms.device).isEqualTo(device)
        }

        @Test
        fun getServiceType() {
            assertThat(cms.serviceType).isEqualTo("urn:schemas-upnp-org:service:ConnectionManager:1")
            assertThat(cds.serviceType).isEqualTo("urn:schemas-upnp-org:service:ContentDirectory:1")
            assertThat(mmupnp.serviceType).isEqualTo("urn:schemas-mm2d-net:service:X_mmupnp:1")
        }

        @Test
        fun getServiceId() {
            assertThat(cms.serviceId).isEqualTo("urn:upnp-org:serviceId:ConnectionManager")
            assertThat(cds.serviceId).isEqualTo("urn:upnp-org:serviceId:ContentDirectory")
            assertThat(mmupnp.serviceId).isEqualTo("urn:upnp-org:serviceId:X_mmupnp")
        }

        @Test
        fun getScpdUrl() {
            assertThat(cms.scpdUrl).isEqualTo("/cms.xml")
            assertThat(cds.scpdUrl).isEqualTo("/cds.xml")
            assertThat(mmupnp.scpdUrl).isEqualTo("/mmupnp.xml")
        }

        @Test
        fun getControlUrl() {
            assertThat(cms.controlUrl).isEqualTo("/cms/control")
            assertThat(cds.controlUrl).isEqualTo("/cds/control")
            assertThat(mmupnp.controlUrl).isEqualTo("/mmupnp/control")
        }

        @Test
        fun getEventSubUrl() {
            assertThat(cms.eventSubUrl).isEqualTo("/cms/event")
            assertThat(cds.eventSubUrl).isEqualTo("/cds/event")
            assertThat(mmupnp.eventSubUrl).isEqualTo("/mmupnp/event")
        }

        @Test
        fun getDescription() {
            assertThat(cms.description).isEqualTo(TestUtils.getResourceAsString("cms.xml"))
            assertThat(cds.description).isEqualTo(TestUtils.getResourceAsString("cds.xml"))
            assertThat(mmupnp.description).isEqualTo(TestUtils.getResourceAsString("mmupnp.xml"))
        }

        @Test
        fun getActionList() {
            assertThat(cms.actionList).hasSize(3)
            assertThat(cds.actionList).hasSize(4)
            assertThat(mmupnp.actionList).hasSize(1)
            assertThat(cms.actionList).isEqualTo(cms.actionList)
        }

        @Test
        fun findAction() {
            assertThat(cms.findAction("Browse")).isNull()
            assertThat(cds.findAction("Browse")).isNotNull()
        }

        @Test
        @Throws(Exception::class)
        fun getStateVariableList() {
            assertThat(cds.stateVariableList).hasSize(11)
            assertThat(cds.stateVariableList).isEqualTo(cds.stateVariableList)
        }

        @Test
        fun getStateVariableParam() {
            val type = mmupnp.findStateVariable("A_ARG_TYPE_Type")
            assertThat(type!!.name).isEqualTo("A_ARG_TYPE_Type")
            assertThat(type.isSendEvents).isFalse()
            assertThat(type.dataType).isEqualTo("string")

            val value = mmupnp.findStateVariable("A_ARG_TYPE_Value")
            assertThat(value!!.name).isEqualTo("A_ARG_TYPE_Value")
            assertThat(value.isSendEvents).isTrue()
            assertThat(value.dataType).isEqualTo("i4")
            assertThat(value.defaultValue).isEqualTo("10")
            assertThat(value.step).isEqualTo("1")
            assertThat(value.minimum).isEqualTo("0")
            assertThat(value.maximum).isEqualTo("100")
        }

        @Test
        fun findStateVariable() {
            val name = "A_ARG_TYPE_BrowseFlag"
            val variable = cds.findStateVariable(name)
            assertThat(variable!!.name).isEqualTo(name)
        }

        private fun createSubscribeResponse(): SingleHttpResponse {
            return SingleHttpResponse.create().apply {
                setStatus(Http.Status.HTTP_OK)
                setHeader(Http.SERVER, Property.SERVER_VALUE)
                setHeader(Http.DATE, Http.formatDate(System.currentTimeMillis()))
                setHeader(Http.CONNECTION, Http.CLOSE)
                setHeader(Http.SID, SID)
                setHeader(Http.TIMEOUT, "Second-300")
                setHeader(Http.CONTENT_LENGTH, "0")
            }
        }

        @Test
        fun subscribe() {
            val client = spyk(SingleHttpClient())
            val slot = slot<SingleHttpRequest>()
            every { client.post(capture(slot)) } returns createSubscribeResponse()
            mockkObject(SingleHttpClient.Companion)
            every { SingleHttpClient.create(any()) } returns client
            runBlocking {
                cds.subscribe()
            }

            val request = slot.captured
            assertThat(request.getUri()).isEqualTo(cds.eventSubUrl)
            verify(exactly = 1) { subscribeManager.register(any(), TimeUnit.SECONDS.toMillis(300), false) }

            val callback = request.getHeader(Http.CALLBACK)
            assertThat(callback!![0]).isEqualTo('<')
            assertThat(callback[callback.length - 1]).isEqualTo('>')
            val url = URL(callback.substring(1, callback.length - 2))
            assertThat(url.host).isEqualTo(INTERFACE_ADDRESS)
            assertThat(url.port).isEqualTo(EVENT_PORT)
            unmockkObject(SingleHttpClient.Companion)
        }

        @Test
        fun subscribe_keep() {
            val client = spyk(SingleHttpClient())
            val slot = slot<SingleHttpRequest>()
            every { client.post(capture(slot)) } returns createSubscribeResponse()
            mockkObject(SingleHttpClient.Companion)
            every { SingleHttpClient.create(any()) } returns client
            runBlocking {
                cds.subscribe(true)
            }

            val request = slot.captured
            assertThat(request.getUri()).isEqualTo(cds.eventSubUrl)
            verify(exactly = 1) { subscribeManager.register(any(), TimeUnit.SECONDS.toMillis(300), true) }
            unmockkObject(SingleHttpClient.Companion)
        }

        @Test
        fun renewSubscribe1() {
            val client = spyk(SingleHttpClient())
            val slot = slot<SingleHttpRequest>()
            every { client.post(capture(slot)) } returns createSubscribeResponse()
            mockkObject(SingleHttpClient.Companion)
            every { SingleHttpClient.create(any()) } returns client
            runBlocking {
                cds.subscribe()
                cds.renewSubscribe()
            }

            val request = slot.captured
            assertThat(request.getUri()).isEqualTo(cds.eventSubUrl)
            unmockkObject(SingleHttpClient.Companion)
        }

        @Test
        fun renewSubscribe2() {
            val client = spyk(SingleHttpClient())
            val slot = slot<SingleHttpRequest>()
            every { client.post(capture(slot)) } returns createSubscribeResponse()
            mockkObject(SingleHttpClient.Companion)
            every { SingleHttpClient.create(any()) } returns client
            runBlocking {
                cds.subscribe()
                cds.subscribe()
            }

            val request = slot.captured
            assertThat(request.getUri()).isEqualTo(cds.eventSubUrl)
            unmockkObject(SingleHttpClient.Companion)
        }

        @Test
        fun unsubscribe() {
            val client = spyk(SingleHttpClient())
            val slot = slot<SingleHttpRequest>()
            every { client.post(capture(slot)) } returns createSubscribeResponse()
            mockkObject(SingleHttpClient.Companion)
            every { SingleHttpClient.create(any()) } returns client
            runBlocking {
                cds.subscribe()
                cds.unsubscribe()
            }

            val request = slot.captured
            assertThat(request.getUri()).isEqualTo(cds.eventSubUrl)
            verify(exactly = 1) { subscribeManager.unregister(any()) }
            unmockkObject(SingleHttpClient.Companion)
        }

        @Test
        fun getSubscriptionId() {
            val client = spyk(SingleHttpClient())
            every { client.post(any()) } returns createSubscribeResponse()
            mockkObject(SingleHttpClient.Companion)
            every { SingleHttpClient.create(any()) } returns client
            runBlocking {
                cds.subscribe()
            }

            assertThat(cds.subscriptionId).isEqualTo(SID)
            unmockkObject(SingleHttpClient.Companion)
        }

        companion object {
            private const val SID = "11234567-89ab-cdef-0123-456789abcdef"
            private const val INTERFACE_ADDRESS = "192.0.2.3"
            private const val EVENT_PORT = 100
        }
    }

    @RunWith(JUnit4::class)
    class subscribe_パーサー機能のテスト {

        @Test
        fun parseTimeout_情報がない場合デフォルト() {
            val response = SingleHttpResponse.create()
            response.setStartLine("HTTP/1.1 200 OK")
            assertThat(SubscribeDelegate.parseTimeout(response)).isEqualTo(DEFAULT_SUBSCRIPTION_TIMEOUT)
        }

        @Test
        fun parseTimeout_infiniteの場合デフォルト() {
            val response = SingleHttpResponse.create()
            response.setStartLine("HTTP/1.1 200 OK")
            response.setHeader(Http.TIMEOUT, "infinite")
            assertThat(SubscribeDelegate.parseTimeout(response)).isEqualTo(DEFAULT_SUBSCRIPTION_TIMEOUT)
        }

        @Test
        fun parseTimeout_secondの指定通り() {
            val response = SingleHttpResponse.create()
            response.setStartLine("HTTP/1.1 200 OK")
            response.setHeader(Http.TIMEOUT, "second-100")
            assertThat(SubscribeDelegate.parseTimeout(response)).isEqualTo(TimeUnit.SECONDS.toMillis(100))
        }

        @Test
        fun parseTimeout_フォーマットエラーはデフォルト() {
            val response = SingleHttpResponse.create()
            response.setStartLine("HTTP/1.1 200 OK")
            response.setHeader(Http.TIMEOUT, "seconds-100")
            assertThat(SubscribeDelegate.parseTimeout(response)).isEqualTo(DEFAULT_SUBSCRIPTION_TIMEOUT)

            response.setHeader(Http.TIMEOUT, "second-ff")
            assertThat(SubscribeDelegate.parseTimeout(response)).isEqualTo(DEFAULT_SUBSCRIPTION_TIMEOUT)

            response.setHeader(Http.TIMEOUT, "")
            assertThat(SubscribeDelegate.parseTimeout(response)).isEqualTo(DEFAULT_SUBSCRIPTION_TIMEOUT)
        }

        companion object {
            private val DEFAULT_SUBSCRIPTION_TIMEOUT = TimeUnit.SECONDS.toMillis(300)
        }
    }

    @RunWith(JUnit4::class)
    class subscribe_機能のテスト {
        private lateinit var controlPoint: ControlPointImpl
        private lateinit var device: DeviceImpl
        private lateinit var service: ServiceImpl
        private lateinit var httpClient: SingleHttpClient
        private lateinit var subscribeDelegate: SubscribeDelegate

        @Before
        fun setUp() {
            controlPoint = mockk(relaxed = true)
            every { controlPoint.taskExecutors } returns TaskExecutors()
            device = mockk(relaxed = true)
            every { device.controlPoint } returns controlPoint
            mockkObject(ServiceImpl.Companion)
            every { ServiceImpl.createSubscribeDelegate(any()) } answers {
                spyk(SubscribeDelegate(arg(0)), recordPrivateCalls = true)
            }
            service = ServiceImpl.Builder()
                .setDevice(device)
                .setServiceType("serviceType")
                .setServiceId("serviceId")
                .setScpdUrl("scpdUrl")
                .setControlUrl("controlUrl")
                .setEventSubUrl("eventSubUrl")
                .setDescription("description")
                .build()
            subscribeDelegate = service.subscribeDelegate
            every { subscribeDelegate.makeAbsoluteUrl(any()) } returns URL("http://192.0.2.2/")
            every { subscribeDelegate.callback } returns ""

            httpClient = mockk(relaxed = true)
            mockkObject(SingleHttpClient.Companion)
            every { SingleHttpClient.create(false) } returns httpClient
        }

        @After
        fun teardown() {
            unmockkObject(SingleHttpClient.Companion)
            unmockkObject(ServiceImpl.Companion)
        }

        @Test
        fun subscribeSync_成功() {
            val response = SingleHttpResponse.create()
            response.setStatus(Http.Status.HTTP_OK)
            response.setHeader(Http.SID, "sid")
            response.setHeader(Http.TIMEOUT, "second-300")
            every { httpClient.post(any()) } returns response

            runBlocking {
                assertThat(service.subscribe()).isTrue()
            }
        }

        @Test
        @Throws(Exception::class)
        fun subscribeSync_SIDなし() {
            val response = SingleHttpResponse.create()
            response.setStatus(Http.Status.HTTP_OK)
            response.setHeader(Http.TIMEOUT, "second-300")
            every { httpClient.post(any()) } returns response

            runBlocking {
                assertThat(service.subscribe()).isFalse()
            }
        }

        @Test
        fun subscribeSync_Timeout値異常() {
            val response = SingleHttpResponse.create()
            response.setStatus(Http.Status.HTTP_OK)
            response.setHeader(Http.SID, "sid")
            response.setHeader(Http.TIMEOUT, "second-0")
            every { httpClient.post(any()) } returns response

            runBlocking {
                assertThat(service.subscribe()).isFalse()
            }
        }

        @Test
        fun subscribeSync_Http応答異常() {
            val response = SingleHttpResponse.create()
            response.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
            response.setHeader(Http.SID, "sid")
            response.setHeader(Http.TIMEOUT, "second-300")
            every { httpClient.post(any()) } returns response

            runBlocking {
                assertThat(service.subscribe()).isFalse()
            }
        }

        @Test
        fun subscribeSync_Exception() {
            every { httpClient.post(any()) } throws IOException()

            runBlocking {
                assertThat(service.subscribe()).isFalse()
            }
        }

        @Test
        fun subscribeSync_2回目はrenewがコールされfalseが返されると失敗() {
            val response = SingleHttpResponse.create()
            response.setStatus(Http.Status.HTTP_OK)
            response.setHeader(Http.SID, "sid")
            response.setHeader(Http.TIMEOUT, "second-300")
            every { httpClient.post(any()) } returns response

            runBlocking {
                assertThat(service.subscribe()).isTrue()
            }

            every { subscribeDelegate["renewSubscribeActual"]("sid") } returns false

            runBlocking {
                assertThat(service.subscribe()).isFalse()
            }

            verify(exactly = 1) { subscribeDelegate.renewSubscribeActual("sid") }
        }

        @Test
        fun renewSubscribeSync_subscribe前はsubscribeが実行される() {
            val response = SingleHttpResponse.create()
            response.setStatus(Http.Status.HTTP_OK)
            response.setHeader(Http.SID, "sid")
            response.setHeader(Http.TIMEOUT, "second-300")
            every { httpClient.post(any()) } returns response

            runBlocking {
                assertThat(service.renewSubscribe()).isTrue()
            }

            verify(exactly = 1) { subscribeDelegate.subscribeActual(any()) }
        }

        @Test
        fun renewSubscribeSync_2回目はrenewが実行される() {
            val response = SingleHttpResponse.create()
            response.setStatus(Http.Status.HTTP_OK)
            response.setHeader(Http.SID, "sid")
            response.setHeader(Http.TIMEOUT, "second-300")
            every { httpClient.post(any()) } returns response

            runBlocking {
                assertThat(service.renewSubscribe()).isTrue()
            }
            verify(exactly = 1) { subscribeDelegate.subscribeActual(any()) }

            runBlocking {
                assertThat(service.renewSubscribe()).isTrue()
            }
            verify(exactly = 1) { subscribeDelegate.renewSubscribeActual("sid") }
        }

        @Test
        fun renewSubscribeSync_renewの応答ステータス異常で失敗() {
            val response = SingleHttpResponse.create()
            response.setStatus(Http.Status.HTTP_OK)
            response.setHeader(Http.SID, "sid")
            response.setHeader(Http.TIMEOUT, "second-300")
            every { httpClient.post(any()) } returns response

            runBlocking {
                assertThat(service.renewSubscribe()).isTrue()
            }

            response.setStatus(Http.Status.HTTP_INTERNAL_ERROR)
            runBlocking {
                assertThat(service.renewSubscribe()).isFalse()
            }
        }

        @Test
        fun renewSubscribeSync_sid不一致() {
            val response = SingleHttpResponse.create()
            response.setStatus(Http.Status.HTTP_OK)
            response.setHeader(Http.SID, "sid")
            response.setHeader(Http.TIMEOUT, "second-300")
            every { httpClient.post(any()) } returns response

            runBlocking {
                assertThat(service.renewSubscribe()).isTrue()
            }

            response.setHeader(Http.SID, "sid2")
            runBlocking {
                assertThat(service.renewSubscribe()).isFalse()
            }
        }

        @Test
        fun renewSubscribeSync_Timeout値異常() {
            val response = SingleHttpResponse.create()
            response.setStatus(Http.Status.HTTP_OK)
            response.setHeader(Http.SID, "sid")
            response.setHeader(Http.TIMEOUT, "second-300")
            every { httpClient.post(any()) } returns response

            runBlocking {
                assertThat(service.renewSubscribe()).isTrue()
            }

            response.setHeader(Http.TIMEOUT, "second-0")
            runBlocking {
                assertThat(service.renewSubscribe()).isFalse()
            }
        }

        @Test
        fun renewSubscribeSync_Exception() {
            val response = SingleHttpResponse.create()
            response.setStatus(Http.Status.HTTP_OK)
            response.setHeader(Http.SID, "sid")
            response.setHeader(Http.TIMEOUT, "second-300")
            every { httpClient.post(any()) } returns response

            runBlocking {
                assertThat(service.renewSubscribe()).isTrue()
            }

            every { httpClient.post(any()) } throws IOException()

            runBlocking {
                assertThat(service.renewSubscribe()).isFalse()
            }
        }

        @Test
        fun unsubscribeSync_subscribeする前に実行すると失敗() {
            runBlocking {
                assertThat(service.unsubscribe()).isFalse()
            }
        }

        @Test
        fun unsubscribeSync_正常() {
            val response = SingleHttpResponse.create()
            response.setStatus(Http.Status.HTTP_OK)
            response.setHeader(Http.SID, "sid")
            response.setHeader(Http.TIMEOUT, "second-300")
            every { httpClient.post(any()) } returns response

            runBlocking {
                assertThat(service.subscribe()).isTrue()
            }

            runBlocking {
                assertThat(service.unsubscribe()).isTrue()
            }
        }

        @Test
        fun unsubscribeSync_OK以外は失敗() {
            val response = SingleHttpResponse.create()
            response.setStatus(Http.Status.HTTP_OK)
            response.setHeader(Http.SID, "sid")
            response.setHeader(Http.TIMEOUT, "second-300")
            every { httpClient.post(any()) } returns response
            runBlocking {
                assertThat(service.subscribe()).isTrue()
            }

            response.setStatus(Http.Status.HTTP_INTERNAL_ERROR)

            runBlocking {
                assertThat(service.unsubscribe()).isFalse()
            }
        }

        @Test
        fun unsubscribeSync_Exception() {
            val response = SingleHttpResponse.create()
            response.setStatus(Http.Status.HTTP_OK)
            response.setHeader(Http.SID, "sid")
            response.setHeader(Http.TIMEOUT, "second-300")
            every { httpClient.post(any()) } returns response

            runBlocking {
                assertThat(service.subscribe()).isTrue()
            }

            every { httpClient.post(any()) } throws IOException()

            runBlocking {
                assertThat(service.unsubscribe()).isFalse()
            }
        }

        @Test
        fun subscribeAsync() {
            coEvery { subscribeDelegate.subscribe(any()) } returns true
            runBlocking {
                assertThat(service.subscribe()).isTrue()
            }
        }

        @Test
        fun renewSubscribeAsync() {
            coEvery { subscribeDelegate.renewSubscribe() } returns true
            runBlocking {
                assertThat(service.renewSubscribe()).isTrue()
            }
        }

        @Test
        fun unsubscribeAsync() {
            coEvery { subscribeDelegate.unsubscribe() } returns true
            runBlocking {
                assertThat(service.unsubscribe()).isTrue()
            }
        }
    }
}
