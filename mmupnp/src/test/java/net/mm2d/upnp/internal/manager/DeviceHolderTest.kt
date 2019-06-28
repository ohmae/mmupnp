/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.mm2d.upnp.Device
import net.mm2d.upnp.internal.thread.TaskExecutors
import org.junit.After
import org.junit.Before

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class DeviceHolderTest {
    private lateinit var taskExecutors: TaskExecutors

    @Before
    fun setUp() {
        taskExecutors = TaskExecutors()
    }

    @After
    fun terminate() {
        taskExecutors.terminate()
    }

    @Test(timeout = 10000L)
    fun start_shutdown_デッドロックしない() {
        val holder = DeviceHolder(taskExecutors, mockk(relaxed = true))
        holder.start()
        Thread.sleep(1)
        holder.stop()
        holder.stop()
    }

    @Test
    fun add() {
        val holder = DeviceHolder(taskExecutors, mockk(relaxed = true))
        val device: Device = mockk(relaxed = true)
        every { device.udn } returns UDN
        holder.add(device)

        assertThat(holder[UDN]).isSameInstanceAs(device)
    }

    @Test
    fun remove_device() {
        val holder = DeviceHolder(taskExecutors, mockk(relaxed = true))
        val device: Device = mockk(relaxed = true)
        every { device.udn } returns UDN
        holder.add(device)

        assertThat(holder[UDN]).isSameInstanceAs(device)

        holder.remove(device)
        assertThat(holder[UDN]).isNull()
    }

    @Test
    fun remove_udn() {
        val holder = DeviceHolder(taskExecutors, mockk(relaxed = true))
        val device: Device = mockk(relaxed = true)
        every { device.udn } returns UDN
        holder.add(device)

        assertThat(holder[UDN]).isSameInstanceAs(device)

        holder.remove(UDN)
        assertThat(holder[UDN]).isNull()
    }

    @Test
    fun clear() {
        val holder = DeviceHolder(taskExecutors, mockk(relaxed = true))
        val device: Device = mockk(relaxed = true)
        every { device.udn } returns UDN
        holder.add(device)

        assertThat(holder[UDN]).isSameInstanceAs(device)

        holder.clear()
        assertThat(holder.deviceList).hasSize(0)
    }

    @Test
    fun getDeviceList() {
        val holder = DeviceHolder(taskExecutors, mockk(relaxed = true))
        val device: Device = mockk(relaxed = true)
        every { device.udn } returns UDN
        holder.add(device)

        assertThat(holder.deviceList).hasSize(1)
        assertThat(holder.deviceList).contains(device)
    }

    @Test
    fun size() {
        val holder = DeviceHolder(taskExecutors, mockk(relaxed = true))
        val device: Device = mockk(relaxed = true)
        every { device.udn } returns UDN
        holder.add(device)

        assertThat(holder.size).isEqualTo(1)
    }

    @Test(timeout = 10000L)
    fun stop() {
        val holder = DeviceHolder(taskExecutors, mockk(relaxed = true))

        holder.stop()
        holder.run()
    }

    @Test(timeout = 20000L)
    fun expireDevice_時間経過後に削除される() {
        val expireListener: (Device) -> Unit = mockk(relaxed = true)
        val holder = DeviceHolder(taskExecutors, expireListener)
        val device1: Device = mockk(relaxed = true)
        every { device1.udn } returns UDN
        val device2: Device = mockk(relaxed = true)
        every { device2.udn } returns UDN + "2"

        every { device1.expireTime } returns System.currentTimeMillis() + 100L
        every { device2.expireTime } returns System.currentTimeMillis() + 200L

        holder.start()
        Thread.sleep(1)
        holder.add(device1)
        holder.add(device2)

        assertThat(holder.size).isEqualTo(2)

        Thread.sleep(11000L) // 内部で10秒のマージンを持っているため十分な時間を開ける

        assertThat(holder.size).isEqualTo(0)
        verify(exactly = 1) { expireListener.invoke(device1) }
    }

    companion object {
        private const val UDN = "uuid:01234567-89ab-cdef-0123-456789abcdef"
    }
}
