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
import io.mockk.verify
import net.mm2d.upnp.SingleHttpClient
import net.mm2d.upnp.Icon
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.internal.message.FakeSsdpMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.net.InetAddress

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class DeviceBuilderTest {
    @Test
    fun build() {
        val cp: ControlPointImpl = mockk(relaxed = true)
        val message: SsdpMessage = mockk(relaxed = true)
        val location = "location"
        val uuid = "uuid"
        every { message.location } returns location
        every { message.uuid } returns uuid
        val description = "description"
        val upc = "upc"
        val deviceType = "deviceType"
        val friendlyName = "friendlyName"
        val manufacture = "manufacture"
        val manufactureUrl = "manufactureUrl"
        val modelName = "modelName"
        val modelUrl = "modelUrl"
        val modelDescription = "modelDescription"
        val modelNumber = "modelNumber"
        val serialNumber = "serialNumber"
        val presentationUrl = "presentationUrl"
        val urlBase = "urlBase"
        val icon: Icon = mockk(relaxed = true)
        val service: ServiceImpl = mockk(relaxed = true)
        val serviceBuilder: ServiceImpl.Builder = mockk(relaxed = true)
        every { serviceBuilder.setDevice(any()) } returns serviceBuilder
        every { serviceBuilder.build() } returns service

        val device = DeviceImpl.Builder(cp, message)
            .setDescription(description)
            .setUdn(uuid)
            .setUpc(upc)
            .setDeviceType(deviceType)
            .setFriendlyName(friendlyName)
            .setManufacture(manufacture)
            .setManufactureUrl(manufactureUrl)
            .setModelName(modelName)
            .setModelUrl(modelUrl)
            .setModelDescription(modelDescription)
            .setModelNumber(modelNumber)
            .setSerialNumber(serialNumber)
            .setPresentationUrl(presentationUrl)
            .setUrlBase(urlBase)
            .addIcon(icon)
            .addServiceBuilder(serviceBuilder)
            .build()

        assertThat(device.description).isEqualTo(description)
        assertThat(device.udn).isEqualTo(uuid)
        assertThat(device.upc).isEqualTo(upc)
        assertThat(device.deviceType).isEqualTo(deviceType)
        assertThat(device.friendlyName).isEqualTo(friendlyName)
        assertThat(device.manufacture).isEqualTo(manufacture)
        assertThat(device.manufactureUrl).isEqualTo(manufactureUrl)
        assertThat(device.modelName).isEqualTo(modelName)
        assertThat(device.modelUrl).isEqualTo(modelUrl)
        assertThat(device.modelDescription).isEqualTo(modelDescription)
        assertThat(device.modelNumber).isEqualTo(modelNumber)
        assertThat(device.serialNumber).isEqualTo(serialNumber)
        assertThat(device.presentationUrl).isEqualTo(presentationUrl)
        assertThat(device.baseUrl).isEqualTo(urlBase)
        assertThat(device.iconList).contains(icon)
        assertThat(device.serviceList).contains(service)
    }

    @Test
    fun build_最低限のパラメータ() {
        val cp: ControlPointImpl = mockk(relaxed = true)
        val message: SsdpMessage = mockk(relaxed = true)
        val location = "location"
        val uuid = "uuid"
        every { message.location } returns location
        every { message.uuid } returns uuid
        val description = "description"
        val upc = "upc"
        val deviceType = "deviceType"
        val friendlyName = "friendlyName"
        val manufacture = "manufacture"
        val modelName = "modelName"

        val device = DeviceImpl.Builder(cp, message)
            .setDescription(description)
            .setUdn(uuid)
            .setUpc(upc)
            .setDeviceType(deviceType)
            .setFriendlyName(friendlyName)
            .setManufacture(manufacture)
            .setModelName(modelName)
            .build()

        assertThat(device.description).isEqualTo(description)
        assertThat(device.udn).isEqualTo(uuid)
        assertThat(device.upc).isEqualTo(upc)
        assertThat(device.deviceType).isEqualTo(deviceType)
        assertThat(device.friendlyName).isEqualTo(friendlyName)
        assertThat(device.manufacture).isEqualTo(manufacture)
        assertThat(device.modelName).isEqualTo(modelName)
    }

    @Test(expected = IllegalStateException::class)
    fun build_Description不足() {
        val cp: ControlPointImpl = mockk(relaxed = true)
        val message: SsdpMessage = mockk(relaxed = true)
        val location = "location"
        val uuid = "uuid"
        every { message.location } returns location
        every { message.uuid } returns uuid
        val deviceType = "deviceType"
        val friendlyName = "friendlyName"
        val manufacture = "manufacture"
        val modelName = "modelName"

        DeviceImpl.Builder(cp, message)
            .setUdn(uuid)
            .setDeviceType(deviceType)
            .setFriendlyName(friendlyName)
            .setManufacture(manufacture)
            .setModelName(modelName)
            .build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_DeviceType不足() {
        val cp: ControlPointImpl = mockk(relaxed = true)
        val message: SsdpMessage = mockk(relaxed = true)
        val location = "location"
        val uuid = "uuid"
        every { message.location } returns location
        every { message.uuid } returns uuid
        val description = "description"
        val friendlyName = "friendlyName"
        val manufacture = "manufacture"
        val modelName = "modelName"

        DeviceImpl.Builder(cp, message)
            .setDescription(description)
            .setUdn(uuid)
            .setFriendlyName(friendlyName)
            .setManufacture(manufacture)
            .setModelName(modelName)
            .build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_FriendlyName不足() {
        val cp: ControlPointImpl = mockk(relaxed = true)
        val message: SsdpMessage = mockk(relaxed = true)
        val location = "location"
        val uuid = "uuid"
        every { message.location } returns location
        every { message.uuid } returns uuid
        val description = "description"
        val deviceType = "deviceType"
        val manufacture = "manufacture"
        val modelName = "modelName"

        DeviceImpl.Builder(cp, message)
            .setDescription(description)
            .setUdn(uuid)
            .setDeviceType(deviceType)
            .setManufacture(manufacture)
            .setModelName(modelName)
            .build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_Manufacture不足() {
        val cp: ControlPointImpl = mockk(relaxed = true)
        val message: SsdpMessage = mockk(relaxed = true)
        val location = "location"
        val uuid = "uuid"
        every { message.location } returns location
        every { message.uuid } returns uuid
        val description = "description"
        val deviceType = "deviceType"
        val friendlyName = "friendlyName"
        val modelName = "modelName"

        DeviceImpl.Builder(cp, message)
            .setDescription(description)
            .setUdn(uuid)
            .setDeviceType(deviceType)
            .setFriendlyName(friendlyName)
            .setModelName(modelName)
            .build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_ModelName不足() {
        val cp: ControlPointImpl = mockk(relaxed = true)
        val message: SsdpMessage = mockk(relaxed = true)
        val location = "location"
        val uuid = "uuid"
        every { message.location } returns location
        every { message.uuid } returns uuid
        val description = "description"
        val deviceType = "deviceType"
        val friendlyName = "friendlyName"
        val manufacture = "manufacture"

        DeviceImpl.Builder(cp, message)
            .setDescription(description)
            .setUdn(uuid)
            .setDeviceType(deviceType)
            .setFriendlyName(friendlyName)
            .setManufacture(manufacture)
            .build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_Udn不足() {
        val cp: ControlPointImpl = mockk(relaxed = true)
        val message: SsdpMessage = mockk(relaxed = true)
        val location = "location"
        val uuid = "uuid"
        every { message.location } returns location
        every { message.uuid } returns uuid
        val description = "description"
        val deviceType = "deviceType"
        val friendlyName = "friendlyName"
        val manufacture = "manufacture"
        val modelName = "modelName"

        DeviceImpl.Builder(cp, message)
            .setDescription(description)
            .setDeviceType(deviceType)
            .setFriendlyName(friendlyName)
            .setManufacture(manufacture)
            .setModelName(modelName)
            .build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_Udn不一致() {
        val cp: ControlPointImpl = mockk(relaxed = true)
        val message: SsdpMessage = mockk(relaxed = true)
        val location = "location"
        val uuid = "uuid"
        every { message.location } returns location
        every { message.uuid } returns uuid
        val description = "description"
        val udn = "udn"
        val upc = "upc"
        val deviceType = "deviceType"
        val friendlyName = "friendlyName"
        val manufacture = "manufacture"
        val modelName = "modelName"

        DeviceImpl.Builder(cp, message)
            .setDescription(description)
            .setUdn(udn)
            .setUpc(upc)
            .setDeviceType(deviceType)
            .setFriendlyName(friendlyName)
            .setManufacture(manufacture)
            .setModelName(modelName)
            .build()
    }

    @Test
    fun build_PinnedSsdpMessage_update() {
        val message: FakeSsdpMessage = mockk(relaxed = true)
        every { message.location } returns "location"
        every { message.isPinned } returns true
        val newMessage: SsdpMessage = mockk(relaxed = true)
        val builder = DeviceImpl.Builder(mockk(relaxed = true), message)
        builder.updateSsdpMessage(newMessage)
        assertThat(builder.getSsdpMessage()).isEqualTo(message)
    }

    @Test(expected = IllegalArgumentException::class)
    fun build_不正なSsdpMessage1() {
        val illegalMessage: SsdpMessage = mockk(relaxed = true)
        every { illegalMessage.location } returns null
        DeviceImpl.Builder(mockk(relaxed = true), illegalMessage)
    }

    @Test(expected = IllegalArgumentException::class)
    fun build_不正なSsdpMessage2() {
        val message: SsdpMessage = mockk(relaxed = true)
        val location = "location"
        val uuid = "uuid"
        every { message.location } returns location
        every { message.uuid } returns uuid
        val illegalMessage: SsdpMessage = mockk(relaxed = true)
        every { illegalMessage.location } returns null
        DeviceImpl.Builder(mockk(relaxed = true), message)
            .updateSsdpMessage(illegalMessage)
    }

    @Test
    fun updateSsdpMessage_EmbeddedDeviceへの伝搬() {
        val message: SsdpMessage = mockk(relaxed = true)
        val location = "location"
        val uuid = "uuid"
        every { message.location } returns location
        every { message.uuid } returns uuid
        val embeddedDeviceBuilder: DeviceImpl.Builder = mockk(relaxed = true)
        DeviceImpl.Builder(mockk(relaxed = true), message)
            .setEmbeddedDeviceBuilderList(listOf(embeddedDeviceBuilder))
            .updateSsdpMessage(message)

        verify(exactly = 1) { embeddedDeviceBuilder.updateSsdpMessage(message) }
    }

    @Test
    fun putTag_namespaceのnullと空白は等価() {
        val message: SsdpMessage = mockk(relaxed = true)
        every { message.location } returns "location"
        every { message.uuid } returns "uuid"
        val tag1 = "tag1"
        val value1 = "value1"
        val tag2 = "tag2"
        val value2 = "value2"

        val device = DeviceImpl.Builder(mockk(relaxed = true), message)
            .setDescription("description")
            .setUdn("uuid")
            .setUpc("upc")
            .setDeviceType("deviceType")
            .setFriendlyName("friendlyName")
            .setManufacture("manufacture")
            .setModelName("modelName")
            .putTag(null, tag1, value1)
            .putTag("", tag2, value2)
            .build()
        assertThat(device.getValueWithNamespace("", tag1)).isEqualTo(value1)
        assertThat(device.getValueWithNamespace("", tag2)).isEqualTo(value2)
    }

    @Test
    fun onDownloadDescription() {
        val message: FakeSsdpMessage = mockk(relaxed = true)
        every { message.location } returns "location"
        val builder = DeviceImpl.Builder(mockk(relaxed = true), message)
        val client: SingleHttpClient = mockk(relaxed = true)
        val address = InetAddress.getByName("127.0.0.1")
        every { client.localAddress } returns address
        builder.setDownloadInfo(client)

        verify(exactly = 1) { client.localAddress }
        verify(exactly = 1) { message.localAddress = address }
    }

    @Test(expected = IllegalStateException::class)
    fun onDownloadDescription_before_download() {
        val message: FakeSsdpMessage = mockk(relaxed = true)
        every { message.location } returns "location"
        val builder = DeviceImpl.Builder(mockk(relaxed = true), message)
        val client = SingleHttpClient()
        builder.setDownloadInfo(client)
    }
}
