/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.empty

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.mm2d.upnp.cp.Adapter.iconFilter
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EmptyControlPointTest {
    @Test
    fun initialize() {
        val controlPoint = EmptyControlPoint
        controlPoint.initialize()
    }

    @Test
    fun tearDown() {
        val controlPoint = EmptyControlPoint
        controlPoint.terminate()
    }

    @Test
    fun start() {
        val controlPoint = EmptyControlPoint
        controlPoint.start()
    }

    @Test
    fun stop() {
        val controlPoint = EmptyControlPoint
        controlPoint.stop()
    }

    @Test
    fun clearDeviceList() {
        val controlPoint = EmptyControlPoint
        controlPoint.clearDeviceList()
    }

    @Test
    fun search() {
        val controlPoint = EmptyControlPoint
        controlPoint.search()
    }

    @Test
    fun search1() {
        val controlPoint = EmptyControlPoint
        controlPoint.search("upnp:rootdevice")
    }

    @Test
    fun setSsdpMessageFilter() {
        val controlPoint = EmptyControlPoint
        controlPoint.setSsdpMessageFilter { true }
    }

    @Test
    fun setIconFilter() {
        val controlPoint = EmptyControlPoint
        controlPoint.setIconFilter(iconFilter { emptyList() })
    }

    @Test
    fun addDiscoveryListener() {
        val controlPoint = EmptyControlPoint
        controlPoint.addDiscoveryListener(mockk())
    }

    @Test
    fun removeDiscoveryListener() {
        val controlPoint = EmptyControlPoint
        controlPoint.removeDiscoveryListener(mockk())
    }

    @Test
    fun addEventListener() {
        val controlPoint = EmptyControlPoint
        controlPoint.addEventListener(mockk())
    }

    @Test
    fun removeEventListener() {
        val controlPoint = EmptyControlPoint
        controlPoint.removeEventListener(mockk())
    }

    @Test
    fun addMulticastEventListener() {
        val controlPoint = EmptyControlPoint
        controlPoint.addMulticastEventListener(mockk())
    }

    @Test
    fun removeMulticastEventListener() {
        val controlPoint = EmptyControlPoint
        controlPoint.removeMulticastEventListener(mockk())
    }

    @Test
    fun getDeviceListSize() {
        val controlPoint = EmptyControlPoint
        assertThat(controlPoint.deviceListSize).isEqualTo(0)
    }

    @Test
    fun getDeviceList() {
        val controlPoint = EmptyControlPoint
        assertThat(controlPoint.deviceList).isNotNull()
    }

    @Test
    fun getDevice() {
        val controlPoint = EmptyControlPoint
        assertThat(controlPoint.getDevice("")).isNull()
    }

    @Test
    fun tryAddDevice() {
        val controlPoint = EmptyControlPoint
        controlPoint.tryAddDevice("", "")
    }

    @Test
    fun addPinnedDevice() {
        val controlPoint = EmptyControlPoint
        controlPoint.tryAddPinnedDevice("")
    }

    @Test
    fun removePinnedDevice() {
        val controlPoint = EmptyControlPoint
        controlPoint.removePinnedDevice("")
    }
}
