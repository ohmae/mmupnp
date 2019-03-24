/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EmptyDeviceTest {

    @Test
    fun loadIconBinary() {
        val device = EmptyDevice
        device.loadIconBinary(mockk()) { it }
    }

    @Test
    fun getControlPoint() {
        val device = EmptyDevice
        assertThat(device.controlPoint).isNotNull()
    }

    @Test
    fun updateSsdpMessage() {
        val device = EmptyDevice
        device.updateSsdpMessage(mockk())
    }

    @Test
    fun getSsdpMessage() {
        val device = EmptyDevice
        assertThat(device.ssdpMessage).isNotNull()
    }

    @Test
    fun getExpireTime() {
        val device = EmptyDevice
        assertThat(device.expireTime).isAtLeast(0)
    }

    @Test
    fun getDescription() {
        val device = EmptyDevice
        assertThat(device.description).isNotNull()
    }

    @Test
    fun getScopeId() {
        val device = EmptyDevice
        assertThat(device.scopeId).isEqualTo(0)
    }

    @Test
    fun getValue() {
        val device = EmptyDevice
        assertThat(device.getValue("")).isNull()
    }

    @Test
    fun getValueWithNamespace() {
        val device = EmptyDevice
        assertThat(device.getValueWithNamespace("", "")).isNull()
    }

    @Test
    fun getLocation() {
        val device = EmptyDevice
        assertThat(device.location).isNotNull()
    }

    @Test
    fun getBaseUrl() {
        val device = EmptyDevice
        assertThat(device.baseUrl).isNotNull()
    }

    @Test
    fun getIpAddress() {
        val device = EmptyDevice
        assertThat(device.ipAddress).isNotNull()
    }

    @Test
    fun getUdn() {
        val device = EmptyDevice
        assertThat(device.udn).isNotNull()
    }

    @Test
    fun getUpc() {
        val device = EmptyDevice
        assertThat(device.upc).isNull()
    }

    @Test
    fun getDeviceType() {
        val device = EmptyDevice
        assertThat(device.deviceType).isNotNull()
    }

    @Test
    fun getFriendlyName() {
        val device = EmptyDevice
        assertThat(device.friendlyName).isNotNull()
    }

    @Test
    fun getManufacture() {
        val device = EmptyDevice
        assertThat(device.manufacture).isNull()
    }

    @Test
    fun getManufactureUrl() {
        val device = EmptyDevice
        assertThat(device.manufactureUrl).isNull()
    }

    @Test
    fun getModelName() {
        val device = EmptyDevice
        assertThat(device.modelName).isNotNull()
    }

    @Test
    fun getModelUrl() {
        val device = EmptyDevice
        assertThat(device.modelUrl).isNull()
    }

    @Test
    fun getModelDescription() {
        val device = EmptyDevice
        assertThat(device.modelDescription).isNull()
    }

    @Test
    fun getModelNumber() {
        val device = EmptyDevice
        assertThat(device.modelNumber).isNull()
    }

    @Test
    fun getSerialNumber() {
        val device = EmptyDevice
        assertThat(device.serialNumber).isNull()
    }

    @Test
    fun getPresentationUrl() {
        val device = EmptyDevice
        assertThat(device.presentationUrl).isNull()
    }

    @Test
    fun getIconList() {
        val device = EmptyDevice
        assertThat(device.iconList).isNotNull()
    }

    @Test
    fun getServiceList() {
        val device = EmptyDevice
        assertThat(device.serviceList).isNotNull()
    }

    @Test
    fun findServiceById() {
        val device = EmptyDevice
        assertThat(device.findServiceById("")).isNull()
    }

    @Test
    fun findServiceByType() {
        val device = EmptyDevice
        assertThat(device.findServiceByType("")).isNull()
    }

    @Test
    fun findAction() {
        val device = EmptyDevice
        assertThat(device.findAction("")).isNull()
    }

    @Test
    fun isEmbeddedDevice() {
        val device = EmptyDevice
        assertThat(device.isEmbeddedDevice).isFalse()
    }

    @Test
    fun getParent() {
        val device = EmptyDevice
        assertThat(device.parent).isNull()
    }

    @Test
    fun getDeviceList() {
        val device = EmptyDevice
        assertThat(device.deviceList).isNotNull()
    }

    @Test
    fun findDeviceByType() {
        val device = EmptyDevice
        assertThat(device.findDeviceByType("")).isNull()
    }

    @Test
    fun findDeviceByTypeRecursively() {
        val device = EmptyDevice
        assertThat(device.findDeviceByTypeRecursively("")).isNull()
    }

    @Test
    fun isPinned() {
        val device = EmptyDevice
        assertThat(device.isPinned).isFalse()
    }
}
