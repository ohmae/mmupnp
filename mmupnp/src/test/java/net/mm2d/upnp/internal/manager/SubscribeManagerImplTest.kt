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
import io.mockk.spyk
import io.mockk.verify
import net.mm2d.upnp.ControlPoint.NotifyEventListener
import net.mm2d.upnp.Service
import net.mm2d.upnp.internal.impl.DiFactory
import net.mm2d.upnp.internal.server.EventReceiver
import net.mm2d.upnp.internal.thread.TaskExecutors
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SubscribeManagerImplTest {
    @Test
    fun onEventReceived_has_no_service() {
        val holder: SubscribeServiceHolder = mockk(relaxed = true)
        val executors: TaskExecutors = mockk(relaxed = true)
        val listener: NotifyEventListener = mockk(relaxed = true)
        val factory = spyk(DiFactory())
        every { factory.createSubscribeServiceHolder(any()) } returns holder

        val manager = SubscribeManagerImpl(executors, setOf(listener), factory)
        assertThat(manager.onEventReceived("", 0, emptyList())).isFalse()
    }

    @Test
    fun onEventReceived() {
        val holder: SubscribeServiceHolder = mockk(relaxed = true)
        val taskExecutors = TaskExecutors()
        val listener: NotifyEventListener = mockk(relaxed = true)
        val factory = spyk(DiFactory())
        every { factory.createSubscribeServiceHolder(any()) } returns holder
        val manager = SubscribeManagerImpl(taskExecutors, setOf(listener), factory)
        val sid = "sid"
        val service: Service = mockk(relaxed = true)
        every { holder.getService(sid) } returns service

        assertThat(manager.onEventReceived(sid, 0, listOf("" to ""))).isTrue()

        taskExecutors.terminate()
    }

    @Test
    fun initialize() {
        val holder: SubscribeServiceHolder = mockk(relaxed = true)
        val executors: TaskExecutors = mockk(relaxed = true)
        val listener: NotifyEventListener = mockk(relaxed = true)
        val factory = spyk(DiFactory())
        every { factory.createSubscribeServiceHolder(any()) } returns holder
        val manager = SubscribeManagerImpl(executors, setOf(listener), factory)
        manager.initialize()
        verify(exactly = 1) { holder.start() }
    }

    @Test
    fun start() {
        val receiver: EventReceiver = mockk(relaxed = true)
        val executors: TaskExecutors = mockk(relaxed = true)
        val listener: NotifyEventListener = mockk(relaxed = true)
        val factory = spyk(DiFactory())
        every { factory.createEventReceiver(any(), any()) } returns receiver
        val manager = SubscribeManagerImpl(executors, setOf(listener), factory)
        manager.start()
        verify(exactly = 1) { receiver.start() }
    }

    @Test
    fun stop() {
        val holder: SubscribeServiceHolder = mockk(relaxed = true)
        val receiver: EventReceiver = mockk(relaxed = true)
        val executors: TaskExecutors = mockk(relaxed = true)
        val listener: NotifyEventListener = mockk(relaxed = true)
        val factory = spyk(DiFactory())
        every { factory.createSubscribeServiceHolder(any()) } returns holder
        every { factory.createEventReceiver(any(), any()) } returns receiver
        val manager = SubscribeManagerImpl(executors, setOf(listener), factory)
        manager.stop()

        verify(exactly = 1) { holder.clear() }
        verify(exactly = 1) { receiver.stop() }
    }

    @Test
    fun terminate() {
        val holder: SubscribeServiceHolder = mockk(relaxed = true)
        val executors: TaskExecutors = mockk(relaxed = true)
        val listener: NotifyEventListener = mockk(relaxed = true)
        val factory = spyk(DiFactory())
        every { factory.createSubscribeServiceHolder(any()) } returns holder
        val manager = SubscribeManagerImpl(executors, setOf(listener), factory)
        manager.terminate()
        verify(exactly = 1) { holder.stop() }
    }

    @Test
    fun getEventPort() {
        val receiver: EventReceiver = mockk(relaxed = true)
        val executors: TaskExecutors = mockk(relaxed = true)
        val listener: NotifyEventListener = mockk(relaxed = true)
        val factory = spyk(DiFactory())
        every { factory.createEventReceiver(any(), any()) } returns receiver
        val manager = SubscribeManagerImpl(executors, setOf(listener), factory)
        val port = 80
        every { receiver.getLocalPort() } returns port

        assertThat(manager.getEventPort()).isEqualTo(port)

        verify(exactly = 1) { receiver.getLocalPort() }
    }

    @Test
    fun getSubscribeService() {
        val holder: SubscribeServiceHolder = mockk(relaxed = true)
        val executors: TaskExecutors = mockk(relaxed = true)
        val listener: NotifyEventListener = mockk(relaxed = true)
        val factory = spyk(DiFactory())
        every { factory.createSubscribeServiceHolder(any()) } returns holder
        val manager = SubscribeManagerImpl(executors, setOf(listener), factory)
        val id = "id"
        val service: Service = mockk(relaxed = true)
        every { holder.getService(id) } returns service

        assertThat(manager.getSubscribeService(id)).isEqualTo(service)

        verify(exactly = 1) { holder.getService(id) }
    }

    @Test
    fun register() {
        val holder: SubscribeServiceHolder = mockk(relaxed = true)
        val executors: TaskExecutors = mockk(relaxed = true)
        val listener: NotifyEventListener = mockk(relaxed = true)
        val factory = spyk(DiFactory())
        every { factory.createSubscribeServiceHolder(any()) } returns holder
        val manager = SubscribeManagerImpl(executors, setOf(listener), factory)
        val service: Service = mockk(relaxed = true)
        val timeout = 1000L

        manager.register(service, timeout, true)

        verify(exactly = 1) { holder.add(service, timeout, true) }
    }

    @Test
    fun renew() {
        val executors: TaskExecutors = mockk(relaxed = true)
        val listener: NotifyEventListener = mockk(relaxed = true)
        val factory = DiFactory()
        val manager = SubscribeManagerImpl(executors, setOf(listener), factory)
        val service: Service = mockk(relaxed = true)
        val id = "id"
        every { service.subscriptionId } returns id
        val timeout = 1000L

        manager.renew(service, timeout)
        manager.setKeepRenew(service, false)
        manager.register(service, timeout, true)
        manager.renew(service, timeout)
        manager.setKeepRenew(service, false)
    }

    @Test
    fun unregister() {
        val holder: SubscribeServiceHolder = mockk(relaxed = true)
        val executors: TaskExecutors = mockk(relaxed = true)
        val listener: NotifyEventListener = mockk(relaxed = true)
        val factory = spyk(DiFactory())
        every { factory.createSubscribeServiceHolder(any()) } returns holder
        val manager = SubscribeManagerImpl(executors, setOf(listener), factory)
        val service: Service = mockk(relaxed = true)

        manager.unregister(service)

        verify(exactly = 1) { holder.remove(service) }
    }
}
