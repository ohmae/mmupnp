/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager

import net.mm2d.log.Logger
import net.mm2d.upnp.ControlPoint.NotifyEventListener
import net.mm2d.upnp.Service
import net.mm2d.upnp.internal.impl.DiFactory
import net.mm2d.upnp.internal.server.EventReceiver
import net.mm2d.upnp.internal.thread.TaskExecutors

internal class SubscribeManagerImpl(
    private val taskExecutors: TaskExecutors,
    private val listeners: Set<NotifyEventListener>,
    factory: DiFactory
) : SubscribeManager {
    private val subscribeHolder: SubscribeHolder = factory.createSubscribeHolder(taskExecutors)
    private val eventReceiver: EventReceiver = factory.createEventReceiver(
        taskExecutors, this::onEventReceived
    )

    // VisibleForTesting
    internal fun onEventReceived(sid: String, seq: Long, properties: List<Pair<String, String>>): Boolean {
        Logger.d { "$sid $seq $properties" }
        val service = subscribeHolder.getService(sid)
        if (service == null) {
            Logger.w { "no service to receive: $sid" }
            return false
        }
        return taskExecutors.callback {
            properties.forEach {
                notifyEvent(service, seq, it.first, it.second)
            }
        }
    }

    private fun notifyEvent(service: Service, seq: Long, name: String?, value: String?) {
        val variable = service.findStateVariable(name)
        if (variable?.isSendEvents != true || value == null) {
            Logger.w { "illegal notify argument: $name $value" }
            return
        }
        listeners.forEach {
            it.onNotifyEvent(service, seq, variable.name, value)
        }
    }

    override fun getEventPort(): Int =
        eventReceiver.getLocalPort()

    override fun initialize(): Unit =
        subscribeHolder.start()

    override fun start(): Unit =
        eventReceiver.start()

    override fun stop() {
        subscribeHolder.getServiceList().forEach {
            taskExecutors.io { it.unsubscribeSync() }
        }
        subscribeHolder.clear()
        eventReceiver.stop()
    }

    override fun terminate(): Unit =
        subscribeHolder.stop()

    override fun getSubscribeService(subscriptionId: String): Service? =
        subscribeHolder.getService(subscriptionId)

    override fun register(service: Service, timeout: Long, keep: Boolean): Unit =
        subscribeHolder.add(service, timeout, keep)

    override fun renew(service: Service, timeout: Long): Unit =
        subscribeHolder.renew(service, timeout)

    override fun setKeepRenew(service: Service, keep: Boolean): Unit =
        subscribeHolder.setKeepRenew(service, keep)

    override fun unregister(service: Service): Unit =
        subscribeHolder.remove(service)
}
