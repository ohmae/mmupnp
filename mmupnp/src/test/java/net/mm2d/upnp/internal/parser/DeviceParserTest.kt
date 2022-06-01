/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.parser

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import net.mm2d.upnp.SingleHttpClient
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.internal.impl.ControlPointImpl
import net.mm2d.upnp.internal.impl.DeviceImpl
import net.mm2d.upnp.internal.message.SsdpRequest
import net.mm2d.upnp.util.TestUtils
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException
import java.net.URL

@Suppress("TestFunctionName", "NonAsciiCharacters", "ClassName")
@RunWith(Enclosed::class)
class DeviceParserTest {
    @RunWith(JUnit4::class)
    class 全行程のテスト {
        private lateinit var httpClient: SingleHttpClient
        private lateinit var ssdpMessage: SsdpMessage
        private lateinit var controlPoint: ControlPointImpl

        @Before
        fun setUp() {
            httpClient = mockk(relaxed = true)
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
        }

        @Test
        fun loadDescription_正常系() {
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device.xml")

            val builder = DeviceImpl.Builder(controlPoint, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
            val device = builder.build()
            assertThat(device.iconList).hasSize(4)
            assertThat(device.serviceList).hasSize(3)

            assertThat(ControlPointImpl.collectUdn(device)).hasSize(1)
        }

        @Test
        fun loadDescription_想定外のタグは無視する() {
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device-with-garbage.xml")

            val builder = DeviceImpl.Builder(controlPoint, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
            val device = builder.build()
            assertThat(device.iconList).hasSize(4)
            assertThat(device.serviceList).hasSize(4)
        }

        @Test
        fun loadDescription_特別対応() {
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device.xml")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/mmupnp.xml"))
            } returns TestUtils.getResourceAsString("mmupnp-with-mistake.xml")

            val builder = DeviceImpl.Builder(controlPoint, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
            val device = builder.build()
            assertThat(device.iconList).hasSize(4)
            assertThat(device.serviceList).hasSize(3)
        }

        @Test
        fun loadDescription_特殊パターン() {
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device.xml")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/mmupnp.xml"))
            } returns TestUtils.getResourceAsString("mmupnp-special.xml")

            val builder = DeviceImpl.Builder(controlPoint, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
            val device = builder.build()
            assertThat(device.iconList).hasSize(4)
            assertThat(device.serviceList).hasSize(3)
        }

        @Test(expected = IllegalStateException::class)
        fun loadDescription_acid_service() {
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device.xml")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/mmupnp.xml"))
            } returns TestUtils.getResourceAsString("mmupnp-acid.xml")

            val builder = DeviceImpl.Builder(controlPoint, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
        }

        @Test(expected = IllegalStateException::class)
        fun loadDescription_acid_device() {
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device-acid.xml")

            val builder = DeviceImpl.Builder(controlPoint, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
        }

        @Test
        fun loadDescription_acid2_device() {
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device-acid2.xml")

            val builder = DeviceImpl.Builder(controlPoint, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
        }


        @Test(expected = IOException::class)
        fun loadDescription_no_scpd_url() {
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device-no-scpd-url.xml")

            val builder = DeviceImpl.Builder(controlPoint, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
        }

        @Test
        fun loadDescription_google_dial() {
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device-google-dial.xml")

            val builder = DeviceImpl.Builder(controlPoint, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
            val device = builder.build()
            assertThat(device.iconList).hasSize(4)
            assertThat(device.serviceList).hasSize(1)
        }

        @Test
        fun loadDescription_no_icon_device() {
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device-no-icon.xml")

            val builder = DeviceImpl.Builder(controlPoint, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
            val device = builder.build()
            assertThat(device.iconList).hasSize(0)
            assertThat(device.serviceList).hasSize(3)
        }

        @Test
        fun loadDescription_no_service_device() {
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device-no-service.xml")

            val builder = DeviceImpl.Builder(controlPoint, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
            val device = builder.build()
            assertThat(device.iconList).hasSize(4)
            assertThat(device.serviceList).hasSize(0)
        }

        @Test
        fun loadDescription_with_url_base() {
            every {
                httpClient.downloadString(URL("http://192.0.2.3:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device-with-url-base.xml")
            val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0-for-url-base.bin")
            ssdpMessage = SsdpRequest.create(mockk(relaxed = true), data, data.size)

            val builder = DeviceImpl.Builder(controlPoint, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
            val device = builder.build()
            assertThat(device.baseUrl).isEqualTo("http://192.0.2.2:12345/")
        }

        @Test
        fun loadDescription_with_embedded_device() {
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device-with-embedded-device.xml")

            val builder = DeviceImpl.Builder(controlPoint, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
            val device = builder.build()

            assertThat(device.isEmbeddedDevice).isEqualTo(false)
            assertThat(device.parent).isNull()
            // 無限ループしない
            assertThat(device.findDeviceByTypeRecursively("")).isNull()
            assertThat(device.deviceList).hasSize(1)
            val device1 = device.findDeviceByType("urn:schemas-upnp-org:device:WANDevice:1") ?: return fail()
            assertThat(device1.deviceList).hasSize(1)
            assertThat(device1.findServiceById("urn:upnp-org:serviceId:WANCommonIFC1")).isNotNull()
            assertThat(device1.parent).isEqualTo(device)
            assertThat(device1.isEmbeddedDevice).isEqualTo(true)

            val embeddedDevice = device.findDeviceByTypeRecursively("urn:schemas-upnp-org:device:WANConnectionDevice:1")
                ?: return fail()
            assertThat(embeddedDevice.findServiceById("urn:upnp-org:serviceId:WANIPConn1")).isNotNull()

            assertThat(embeddedDevice.upc).isEqualTo("000000000000")
            assertThat(embeddedDevice.parent).isEqualTo(device1)
            assertThat(embeddedDevice.isEmbeddedDevice).isEqualTo(true)

            val udnSet = ControlPointImpl.collectUdn(device)
            assertThat(udnSet).contains("uuid:01234567-89ab-cdef-0123-456789abcdee")
            assertThat(udnSet).contains("uuid:01234567-89ab-cdef-0123-456789abcded")
            assertThat(udnSet).contains("uuid:01234567-89ab-cdef-0123-456789abcdef")
        }

        @Test
        fun loadDescription_with_embedded_device_異常系() {
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device-with-embedded-device.xml")

            val builder = DeviceImpl.Builder(controlPoint, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
            val device = builder.build()

            assertThat(device.findDeviceByType("urn:schemas-upnp-org:device:WANDevice:11")).isNull()
            assertThat(device.findDeviceByTypeRecursively("urn:schemas-upnp-org:device:WANConnectionDevice:11")).isNull()
        }

        @Test
        fun loadDescription_with_embedded_device_embedded_deviceへの伝搬() {
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device-with-embedded-device.xml")

            val builder = DeviceImpl.Builder(controlPoint, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
            val device = builder.build()

            val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive1.bin")
            val message = SsdpRequest.create(mockk(relaxed = true), data, data.size)
            device.updateSsdpMessage(message)

            val device1 = device.findDeviceByType("urn:schemas-upnp-org:device:WANDevice:1") ?: return fail()
            assertThat(device1.ssdpMessage).isEqualTo(message)
        }
    }

    @RunWith(JUnit4::class)
    class 機能ごとのテスト {
        @Test(expected = IOException::class)
        fun loadDescription_ダウンロード失敗でIOException() {
            val builder: DeviceImpl.Builder = mockk(relaxed = true)
            every { builder.getLocation() } returns "http://192.168.0.1/"
            every { builder.getSsdpMessage() } returns mockk(relaxed = true)
            DeviceParser.loadDescription(mockk(relaxed = true), builder)
        }

        @Test(expected = IOException::class)
        fun parseDescription_deviceノードのないXMLを渡すとException() {
            DeviceParser.parseDescription(
                mockk(relaxed = true),
                "<?xml version=\"1.0\"?>\n" +
                    "<root xmlns=\"urn:schemas-upnp-org:device-1-0\">\n" +
                    "</root>"
            )
        }
    }
}
