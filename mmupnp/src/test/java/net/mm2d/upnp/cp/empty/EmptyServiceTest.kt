/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.empty

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EmptyServiceTest {
    @Test
    fun getDevice() {
        val service = EmptyService
        assertThat(service.device).isNotNull()
    }

    @Test
    fun getServiceType() {
        val service = EmptyService
        assertThat(service.serviceType).isNotNull()
    }

    @Test
    fun getServiceId() {
        val service = EmptyService
        assertThat(service.serviceId).isNotNull()
    }

    @Test
    fun getScpdUrl() {
        val service = EmptyService
        assertThat(service.scpdUrl).isNotNull()
    }

    @Test
    fun getControlUrl() {
        val service = EmptyService
        assertThat(service.controlUrl).isNotNull()
    }

    @Test
    fun getEventSubUrl() {
        val service = EmptyService
        assertThat(service.eventSubUrl).isNotNull()
    }

    @Test
    fun getDescription() {
        val service = EmptyService
        assertThat(service.description).isNotNull()
    }

    @Test
    fun getActionList() {
        val service = EmptyService
        assertThat(service.actionList).isNotNull()
    }

    @Test
    fun findAction() {
        val service = EmptyService
        assertThat(service.findAction("")).isNull()
    }

    @Test
    fun getStateVariableList() {
        val service = EmptyService
        assertThat(service.stateVariableList).isNotNull()
    }

    @Test
    fun findStateVariable() {
        val service = EmptyService
        assertThat(service.findStateVariable("")).isNull()
    }

    @Test
    fun getSubscriptionId() {
        val service = EmptyService
        assertThat(service.subscriptionId).isNull()
    }

    @Test
    fun subscribeSync() {
        val service = EmptyService
        assertThat(service.subscribeSync()).isFalse()
    }

    @Test
    fun subscribeSync1() {
        val service = EmptyService
        assertThat(service.subscribeSync(true)).isFalse()
    }

    @Test
    fun renewSubscribeSync() {
        val service = EmptyService
        assertThat(service.renewSubscribeSync()).isFalse()
    }

    @Test
    fun unsubscribeSync() {
        val service = EmptyService
        assertThat(service.unsubscribeSync()).isFalse()
    }

    @Test
    fun subscribe() {
        val service = EmptyService
        val callback: (Boolean) -> Unit = mockk()
        val slot = slot<Boolean>()
        every { callback.invoke(capture(slot)) } answers { nothing }
        service.subscribe(callback = callback)
        assertThat(slot.captured).isFalse()
    }

    @Test
    fun subscribe1() {
        val service = EmptyService
        val callback: (Boolean) -> Unit = mockk()
        val slot = slot<Boolean>()
        every { callback.invoke(capture(slot)) } answers { nothing }
        service.subscribe(true, callback)
        assertThat(slot.captured).isFalse()
    }

    @Test
    fun subscribe2() {
        val service = EmptyService
        service.subscribe(callback = null)
    }

    @Test
    fun subscribe3() {
        val service = EmptyService
        service.subscribe(true, null)
    }

    @Test
    fun renewSubscribe() {
        val service = EmptyService
        val callback: (Boolean) -> Unit = mockk()
        val slot = slot<Boolean>()
        every { callback.invoke(capture(slot)) } answers { nothing }
        service.renewSubscribe(callback)
        assertThat(slot.captured).isFalse()
    }

    @Test
    fun renewSubscribe1() {
        val service = EmptyService
        service.renewSubscribe(null)
    }

    @Test
    fun unsubscribe() {
        val service = EmptyService
        val callback: (Boolean) -> Unit = mockk()
        val slot = slot<Boolean>()
        every { callback.invoke(capture(slot)) } answers { nothing }
        service.unsubscribe(callback)
        assertThat(slot.captured).isFalse()
    }

    @Test
    fun unsubscribe1() {
        val service = EmptyService
        service.unsubscribe(null)
    }

    @Test
    fun subscribeAsync() {
        val service = EmptyService
        runBlocking {
            assertThat(service.subscribeAsync()).isFalse()
        }
    }

    @Test
    fun subscribeAsync1() {
        val service = EmptyService
        runBlocking {
            assertThat(service.subscribeAsync(true)).isFalse()
        }
    }

    @Test
    fun renewSubscribeAsync() {
        val service = EmptyService
        runBlocking {
            assertThat(service.renewSubscribeAsync()).isFalse()
        }
    }

    @Test
    fun unsubscribeAsync() {
        val service = EmptyService
        runBlocking {
            assertThat(service.unsubscribeAsync()).isFalse()
        }
    }
}
