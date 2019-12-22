/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.property

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class DevicePropertyBuilderTest {
    @Test
    fun build() {
        val uuid = "uuid"
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
        val icon: IconProperty = mockk(relaxed = true)
        val service: ServiceProperty = mockk(relaxed = true)
        val serviceBuilder: ServiceProperty.Builder = mockk(relaxed = true)
        every { serviceBuilder.build() } returns service

        val device = DeviceProperty.Builder().also {
            it.description = description
            it.udn = uuid
            it.upc = upc
            it.deviceType = deviceType
            it.friendlyName = friendlyName
            it.manufacture = manufacture
            it.manufactureUrl = manufactureUrl
            it.modelName = modelName
            it.modelUrl = modelUrl
            it.modelDescription = modelDescription
            it.modelNumber = modelNumber
            it.serialNumber = serialNumber
            it.presentationUrl = presentationUrl
            it.urlBase = urlBase
            it.iconList.add(icon)
            it.serviceBuilderList.add(serviceBuilder)
        }.build()

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
        assertThat(device.urlBase).isEqualTo(urlBase)
        assertThat(device.iconList).contains(icon)
        assertThat(device.serviceList).contains(service)
    }

    @Test
    fun build_最低限のパラメータ() {
        val uuid = "uuid"
        val description = "description"
        val deviceType = "deviceType"
        val friendlyName = "friendlyName"
        val manufacture = "manufacture"
        val modelName = "modelName"

        val device = DeviceProperty.Builder().also {
            it.description = description
            it.udn = uuid
            it.deviceType = deviceType
            it.friendlyName = friendlyName
            it.manufacture = manufacture
            it.modelName = modelName
        }.build()

        assertThat(device.description).isEqualTo(description)
        assertThat(device.udn).isEqualTo(uuid)
        assertThat(device.deviceType).isEqualTo(deviceType)
        assertThat(device.friendlyName).isEqualTo(friendlyName)
        assertThat(device.manufacture).isEqualTo(manufacture)
        assertThat(device.modelName).isEqualTo(modelName)
    }

    @Test(expected = IllegalStateException::class)
    fun build_Description不足() {
        val uuid = "uuid"
        val deviceType = "deviceType"
        val friendlyName = "friendlyName"
        val manufacture = "manufacture"
        val modelName = "modelName"

        DeviceProperty.Builder().also {
            it.udn = uuid
            it.deviceType = deviceType
            it.friendlyName = friendlyName
            it.manufacture = manufacture
            it.modelName = modelName
        }.build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_DeviceType不足() {
        val uuid = "uuid"
        val description = "description"
        val friendlyName = "friendlyName"
        val manufacture = "manufacture"
        val modelName = "modelName"

        DeviceProperty.Builder().also {
            it.description = description
            it.udn = uuid
            it.friendlyName = friendlyName
            it.manufacture = manufacture
            it.modelName = modelName
        }.build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_FriendlyName不足() {
        val uuid = "uuid"
        val description = "description"
        val deviceType = "deviceType"
        val manufacture = "manufacture"
        val modelName = "modelName"

        DeviceProperty.Builder().also {
            it.description = description
            it.udn = uuid
            it.deviceType = deviceType
            it.manufacture = manufacture
            it.modelName = modelName
        }.build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_Manufacture不足() {
        val uuid = "uuid"
        val description = "description"
        val deviceType = "deviceType"
        val friendlyName = "friendlyName"
        val modelName = "modelName"

        DeviceProperty.Builder().also {
            it.description = description
            it.udn = uuid
            it.deviceType = deviceType
            it.friendlyName = friendlyName
            it.modelName = modelName
        }.build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_ModelName不足() {
        val uuid = "uuid"
        val description = "description"
        val deviceType = "deviceType"
        val friendlyName = "friendlyName"
        val manufacture = "manufacture"

        DeviceProperty.Builder().also {
            it.description = description
            it.udn = uuid
            it.deviceType = deviceType
            it.friendlyName = friendlyName
            it.manufacture = manufacture
        }.build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_Udn不足() {
        val description = "description"
        val deviceType = "deviceType"
        val friendlyName = "friendlyName"
        val manufacture = "manufacture"
        val modelName = "modelName"

        DeviceProperty.Builder().also {
            it.description = description
            it.deviceType = deviceType
            it.friendlyName = friendlyName
            it.manufacture = manufacture
            it.modelName = modelName
        }.build()
    }

    @Test
    fun putTag_namespaceのnullと空白は等価() {
        val tag1 = "tag1"
        val value1 = "value1"
        val tag2 = "tag2"
        val value2 = "value2"

        val device = DeviceProperty.Builder().also {
            it.description = "description"
            it.udn = "uuid"
            it.deviceType = "deviceType"
            it.friendlyName = "friendlyName"
            it.manufacture = "manufacture"
            it.modelName = "modelName"
            it.putTag(null, tag1, value1)
            it.putTag("", tag2, value2)
        }.build()
        assertThat(device.getValueWithNamespace("", tag1)).isEqualTo(value1)
        assertThat(device.getValueWithNamespace("", tag2)).isEqualTo(value2)
    }

}
