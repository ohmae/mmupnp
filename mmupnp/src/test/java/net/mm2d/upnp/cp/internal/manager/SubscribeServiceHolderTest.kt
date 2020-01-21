/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.manager

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.mm2d.upnp.common.internal.thread.TaskExecutors
import net.mm2d.upnp.cp.Service
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class SubscribeServiceHolderTest {
    private lateinit var taskExecutors: TaskExecutors

    @Before
    fun setUp() {
        taskExecutors = TaskExecutors()
    }

    @After
    fun tearDown() {
        taskExecutors.terminate()
    }

    @Test(timeout = 60000L)
    fun start_stop_でロックしない() {
        val subscribeHolder = SubscribeServiceHolder(taskExecutors)
        subscribeHolder.start()
        subscribeHolder.stop()
    }

    @Test
    fun add_getServiceできる() {
        val id = "id"
        val service: Service = mockk(relaxed = true)
        every { service.subscriptionId } returns id
        val subscribeHolder = SubscribeServiceHolder(taskExecutors)

        subscribeHolder.add(service, 1000L, false)

        assertThat(subscribeHolder.getService(id)).isEqualTo(service)
    }

    @Test
    fun remove_getServiceできない() {
        val id1 = "id1"
        val service1: Service = mockk(relaxed = true)
        every { service1.subscriptionId } returns id1
        val id2 = "id2"
        val service2: Service = mockk(relaxed = true)
        every { service2.subscriptionId } returns id2
        val subscribeHolder = SubscribeServiceHolder(taskExecutors)

        subscribeHolder.add(service1, 1000L, false)
        subscribeHolder.add(service2, 1000L, false)

        assertThat(subscribeHolder.getService(id1)).isEqualTo(service1)
        assertThat(subscribeHolder.getService(id2)).isEqualTo(service2)

        subscribeHolder.remove(service1)

        assertThat(subscribeHolder.getService(id1)).isNull()
        assertThat(subscribeHolder.getService(id2)).isEqualTo(service2)
    }

    @Test
    fun getServiceList_subscriptionIdがnullだとaddできない() {
        val service1: Service = mockk(relaxed = true)
        val subscribeHolder = SubscribeServiceHolder(taskExecutors)
        subscribeHolder.add(service1, 1000L, false)

        assertThat(subscribeHolder.getService(service1.serviceId)).isNull()
    }

    @Test
    fun clear_すべて取得できなくなる() {
        val id1 = "id1"
        val service1: Service = mockk(relaxed = true)
        every { service1.subscriptionId } returns id1
        val id2 = "id2"
        val service2: Service = mockk(relaxed = true)
        every { service2.subscriptionId } returns id2
        val subscribeHolder = SubscribeServiceHolder(taskExecutors)

        subscribeHolder.add(service1, 1000L, false)
        subscribeHolder.add(service2, 1000L, false)

        assertThat(subscribeHolder.getService(id1)).isEqualTo(service1)
        assertThat(subscribeHolder.getService(id2)).isEqualTo(service2)

        subscribeHolder.clear()

        assertThat(subscribeHolder.getService(id1)).isNull()
        assertThat(subscribeHolder.getService(id2)).isNull()
    }

    @Test(timeout = 60000L)
    fun expire_時間経過で削除される() {
        val id1 = "id1"
        val service1: Service = mockk(relaxed = true)
        every { service1.subscriptionId } returns id1
        val id2 = "id2"
        val service2: Service = mockk(relaxed = true)
        every { service2.subscriptionId } returns id2
        val subscribeHolder = SubscribeServiceHolder(taskExecutors)
        subscribeHolder.start()

        subscribeHolder.add(service1, 1000L, false)
        subscribeHolder.add(service2, 4000L, false)

        assertThat(subscribeHolder.getService(id1)).isEqualTo(service1)
        assertThat(subscribeHolder.getService(id2)).isEqualTo(service2)

        Thread.sleep(3000L)

        assertThat(subscribeHolder.getService(id1)).isNull()
        assertThat(subscribeHolder.getService(id2)).isEqualTo(service2)

        Thread.sleep(3000L)

        assertThat(subscribeHolder.getService(id1)).isNull()
        assertThat(subscribeHolder.getService(id2)).isNull()

        subscribeHolder.stop()
    }

    @Test(timeout = 60000L)
    fun renew_定期的にrenewが実行される() {
        val service1: Service = mockk(relaxed = true)
        every { service1.renewSubscribeSync() } returns true
        every { service1.subscriptionId } returns "id1"

        val service2: Service = mockk(relaxed = true)
        every { service2.renewSubscribeSync() } returns true
        every { service2.subscriptionId } returns "id2"

        val subscribeHolder = SubscribeServiceHolder(taskExecutors)
        subscribeHolder.start()

        subscribeHolder.add(service1, 1000L, true)
        subscribeHolder.add(service2, 500L, true)
        verify(inverse = true) { service1.renewSubscribeSync() }

        Thread.sleep(2000L)
        verify(atLeast = 1) { service1.renewSubscribeSync() }

        subscribeHolder.stop()
    }

    @Test(timeout = 60000L)
    fun renew_失敗したら削除される() {
        val id = "id"
        val service: Service = mockk(relaxed = true)
        every { service.renewSubscribeSync() } returns false
        every { service.subscriptionId } returns id
        val subscribeHolder = SubscribeServiceHolder(taskExecutors)
        subscribeHolder.start()

        subscribeHolder.add(service, 1000L, true)
        Thread.sleep(3000L)
        assertThat(subscribeHolder.getService(id)).isNull()

        subscribeHolder.stop()
    }
}
