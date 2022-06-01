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
import io.mockk.spyk
import net.mm2d.upnp.Adapter.iconFilter
import net.mm2d.upnp.Http
import net.mm2d.upnp.SingleHttpClient
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.internal.message.FakeSsdpMessage
import net.mm2d.upnp.internal.message.SsdpRequest
import net.mm2d.upnp.internal.parser.DeviceParser
import net.mm2d.upnp.util.TestUtils
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException
import java.net.URL

@Suppress("TestFunctionName", "NonAsciiCharacters", "ClassName")
@RunWith(Enclosed::class)
class DeviceTest {
    @RunWith(JUnit4::class)
    class DeviceParserによる生成からのテスト {
        private lateinit var httpClient: SingleHttpClient
        private lateinit var ssdpMessage: SsdpMessage
        private lateinit var controlPoint: ControlPointImpl
        private lateinit var builder: DeviceImpl.Builder

        @Before
        fun setUp() {
            httpClient = mockk(relaxed = true)
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
            ssdpMessage = SsdpRequest.create(mockk(relaxed = true), data, data.size)
            controlPoint = mockk(relaxed = true)
            builder = DeviceImpl.Builder(controlPoint, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
        }

        @Test
        fun loadIconBinary_NONE() {
            val device = builder.build()
            device.loadIconBinary(httpClient, iconFilter { emptyList() })
            device.iconList.forEach {
                assertThat(it.binary).isNull()
            }
        }

        @Test
        fun loadIconBinary_ALL() {
            val device = builder.build()
            device.loadIconBinary(httpClient, iconFilter { it })
            device.iconList.forEach {
                assertThat(it.binary).isNotNull()
            }
        }

        @Test
        fun loadIconBinary_取得に問題があっても他のファイルは取得可能() {
            every {
                httpClient.downloadBinary(URL("http://192.0.2.2:12345/icon/icon120.jpg"))
            } throws IOException()
            val device = builder.build()
            device.loadIconBinary(httpClient, iconFilter { it })
            device.iconList.forEach {
                if (it.url == "/icon/icon120.jpg") {
                    assertThat(it.binary).isNull()
                } else {
                    assertThat(it.binary).isNotNull()
                }
            }
        }

        @Test
        fun getControlPoint() {
            val device = builder.build()
            assertThat(device.controlPoint).isEqualTo(controlPoint)
        }

        @Test
        fun updateSsdpMessage() {
            val device = builder.build()
            val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive1.bin")
            val message = SsdpRequest.create(mockk(relaxed = true), data, data.size)

            assertThat(device.expireTime).isEqualTo(ssdpMessage.expireTime)
            device.updateSsdpMessage(message)

            assertThat(device.ssdpMessage).isEqualTo(message)
            assertThat(device.expireTime).isEqualTo(message.expireTime)
        }

        @Test
        fun updateSsdpMessage_pinned() {
            val originalMessage: FakeSsdpMessage = mockk(relaxed = true)
            every { originalMessage.location } returns "location"
            every { originalMessage.isPinned } returns true
            builder.updateSsdpMessage(originalMessage)
            val device = builder.build()
            val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive1.bin")
            val message = spyk(SsdpRequest.create(mockk(relaxed = true), data, data.size))
            every { message.uuid } returns "uuid:"
            device.updateSsdpMessage(message)

            assertThat(device.ssdpMessage).isEqualTo(originalMessage)
        }

        @Test(expected = IllegalArgumentException::class)
        fun updateSsdpMessage_uuid不一致() {
            val device = builder.build()
            val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive1.bin")
            val message = spyk(SsdpRequest.create(mockk(relaxed = true), data, data.size))
            every { message.uuid } returns "uuid:"
            device.updateSsdpMessage(message)
        }

        @Test(expected = IllegalArgumentException::class)
        fun updateSsdpMessage_location不正() {
            val device = builder.build()
            val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive1.bin")
            val message = spyk(SsdpRequest.create(mockk(relaxed = true), data, data.size))
            every { message.location } returns null
            device.updateSsdpMessage(message)
        }

        @Test
        fun getSsdpMessage() {
            val device = builder.build()
            assertThat(device.ssdpMessage).isEqualTo(ssdpMessage)
        }

        @Test
        fun getExpireTime() {
            val device = builder.build()
            assertThat(device.expireTime).isEqualTo(ssdpMessage.expireTime)
        }

        @Test
        fun getDescription() {
            val device = builder.build()
            assertThat(device.description).isEqualTo(TestUtils.getResourceAsString("device.xml"))
        }

        @Test
        fun getBaseUrl_URLBaseがなければLocationの値() {
            val location = "http://10.0.0.1:1000/"
            val device = builder.build()
            val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive2.bin")
            val message = SsdpRequest.create(mockk(relaxed = true), data, data.size)
            message.setHeader(Http.LOCATION, location)
            message.updateLocation()
            device.updateSsdpMessage(message)

            assertThat(device.baseUrl).isEqualTo(location)
        }

        @Test
        fun getBaseUrl_URLBaseがあればURLBaseの値() {
            val location = "http://10.0.0.1:1000/"
            val urlBase = "http://10.0.0.1:1001/"
            builder.setUrlBase(urlBase)
            val device = builder.build()
            val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive2.bin")
            val message = SsdpRequest.create(mockk(relaxed = true), data, data.size)
            message.setHeader(Http.LOCATION, location)
            message.updateLocation()
            device.updateSsdpMessage(message)

            assertThat(device.baseUrl).isEqualTo(urlBase)
        }

        @Test
        fun getValue() {
            val device = builder.build()
            assertThat(device.getValue("deviceType"))
                .isEqualTo("urn:schemas-upnp-org:device:MediaServer:1")
        }

        @Test
        fun getValue_存在しないタグはnull() {
            val device = builder.build()
            assertThat(device.getValue("deviceType1"))
                .isNull()
        }

        @Test
        fun getValueWithNamespace() {
            val device = builder.build()
            assertThat(device.getValueWithNamespace("urn:schemas-upnp-org:device-1-0", "deviceType"))
                .isEqualTo("urn:schemas-upnp-org:device:MediaServer:1")
        }

        @Test
        fun getValueWithNamespace_namespace間違ったらnull() {
            val device = builder.build()
            assertThat(device.getValueWithNamespace("urn:schemas-upnp-org:device-1-1", "deviceType"))
                .isNull()
        }

        @Test
        fun getLocation() {
            val device = builder.build()
            assertThat(device.location).isEqualTo(ssdpMessage.location)
        }

        @Test
        fun getIpAddress() {
            val device = builder.build()
            assertThat(device.ipAddress).isEqualTo("192.0.2.2")
        }

        @Test
        fun getUdn() {
            val device = builder.build()
            assertThat(device.udn).isEqualTo("uuid:01234567-89ab-cdef-0123-456789abcdef")
        }

        @Test
        fun getDeviceType() {
            val device = builder.build()
            assertThat(device.deviceType).isEqualTo("urn:schemas-upnp-org:device:MediaServer:1")
        }

        @Test
        fun getFriendlyName() {
            val device = builder.build()
            assertThat(device.friendlyName).isEqualTo("mmupnp")
        }

        @Test
        fun getManufacture() {
            val device = builder.build()
            assertThat(device.manufacture).isEqualTo("mm2d.net")
        }

        @Test
        fun getManufactureUrl() {
            val device = builder.build()
            assertThat(device.manufactureUrl).isEqualTo("http://www.mm2d.net/")
        }

        @Test
        fun getModelName() {
            val device = builder.build()
            assertThat(device.modelName).isEqualTo("mmupnp")
        }

        @Test
        fun getModelUrl() {
            val device = builder.build()
            assertThat(device.modelUrl).isEqualTo("http://www.mm2d.net/")
        }

        @Test
        fun getModelDescription() {
            val device = builder.build()
            assertThat(device.modelDescription).isEqualTo("mmupnp test server")
        }

        @Test
        fun getModelNumber() {
            val device = builder.build()
            assertThat(device.modelNumber).isEqualTo("ABCDEFG")
        }

        @Test
        fun getSerialNumber() {
            val device = builder.build()
            assertThat(device.serialNumber).isEqualTo("0123456789ABC")
        }

        @Test
        fun getPresentationUrl() {
            val device = builder.build()
            assertThat(device.presentationUrl).isEqualTo("http://192.0.2.2:12346/")
        }

        @Test
        fun getIconList() {
            val device = builder.build()
            val list = device.iconList
            for (icon in list) {
                assertThat(icon.mimeType).isAnyOf("image/jpeg", "image/png")
                assertThat(icon.width).isAnyOf(48, 120)
                assertThat(icon.height).isAnyOf(48, 120)
                assertThat(icon.depth).isEqualTo(24)
                assertThat(icon.url).isAnyOf(
                    "/icon/icon120.jpg",
                    "/icon/icon48.jpg",
                    "/icon/icon120.png",
                    "/icon/icon48.png"
                )
            }
        }

        @Test
        fun getServiceList() {
            val device = builder.build()
            assertThat(device.serviceList).hasSize(3)
        }

        @Test
        fun findServiceById() {
            val device = builder.build()
            val cds = device.findServiceById("urn:upnp-org:serviceId:ContentDirectory")

            assertThat(cds).isNotNull()
            assertThat(cds!!.device).isEqualTo(device)
        }

        @Test
        fun findServiceById_見つからなければnull() {
            val device = builder.build()
            assertThat(device.findServiceById("urn:upnp-org:serviceId:ContentDirectory1")).isNull()
        }

        @Test
        fun findServiceByType() {
            val device = builder.build()
            val cds = device.findServiceByType("urn:schemas-upnp-org:service:ContentDirectory:1")

            assertThat(cds).isNotNull()
            assertThat(cds!!.device).isEqualTo(device)
        }

        @Test
        fun findServiceByType_見つからなければnull() {
            val device = builder.build()
            assertThat(device.findServiceByType("urn:schemas-upnp-org:service:ContentDirectory:11")).isNull()
        }

        @Test
        fun findAction() {
            val device = builder.build()
            val browse = device.findAction("Browse")

            assertThat(browse).isNotNull()
            assertThat(browse!!.service.device).isEqualTo(device)

            val hogehoge = device.findAction("hogehoge")

            assertThat(hogehoge).isNull()
        }

        @Test
        fun toString_Nullでない() {
            val device = builder.build()
            assertThat(device.toString()).isNotNull()
        }

        @Test
        fun hashCode_Exceptionが発生しない() {
            val device = builder.build()
            device.hashCode()
        }

        @Test
        fun equals_null比較可能() {
            val device = builder.build()
            assertThat(device == null).isEqualTo(false)
        }
    }

    @RunWith(JUnit4::class)
    class DeviceBuilderによる生成からのテスト {
        @Test
        fun getIpAddress_正常() {
            val message: SsdpMessage = mockk(relaxed = true)
            every { message.location } returns "http://192.168.0.1/"
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
            assertThat(device.ipAddress).isEqualTo("192.168.0.1")
        }

        @Test
        fun getIpAddress_異常() {
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

            assertThat(device.ipAddress).isEqualTo("")
        }

        @Test
        fun loadIconBinary_何も起こらない() {
            val message: SsdpMessage = mockk(relaxed = true)
            every { message.location } returns "http://192.168.0.1/"
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

            device.loadIconBinary(mockk(relaxed = true), iconFilter { it })
        }
    }
}
