/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty

import com.google.common.truth.Truth.assertThat
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
    fun subscribe() {
        val service = EmptyService
        assertThat(service.subscribeSync()).isFalse()
    }

    @Test
    fun subscribe1() {
        val service = EmptyService
        assertThat(service.subscribeSync(true)).isFalse()
    }

    @Test
    fun renewSubscribe() {
        val service = EmptyService
        assertThat(service.renewSubscribeSync()).isFalse()
    }

    @Test
    fun unsubscribe() {
        val service = EmptyService
        assertThat(service.unsubscribeSync()).isFalse()
    }

    @Test
    fun getSubscriptionId() {
        val service = EmptyService
        assertThat(service.subscriptionId).isNull()
    }
}
