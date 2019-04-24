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

internal class SubscribeManager(
    private val taskExecutors: TaskExecutors,
    private val listeners: Set<NotifyEventListener>,
    factory: DiFactory
) {
    private val subscribeHolder: SubscribeHolder = factory.createSubscribeHolder(taskExecutors)
    private val eventReceiver: EventReceiver = factory.createEventReceiver(
        taskExecutors, this::onEventReceived
    )

    fun getEventPort(): Int = eventReceiver.getLocalPort()

    fun onEventReceived(sid: String, seq: Long, properties: List<Pair<String, String>>): Boolean {
        Logger.d { "$sid $seq $properties" }
        val service = subscribeHolder.getService(sid)
        if (service == null) {
            Logger.e("service is null")
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

    fun initialize() {
        subscribeHolder.start()
    }

    fun start() {
        eventReceiver.start()
    }

    fun stop() {
        subscribeHolder.getServiceList().forEach {
            taskExecutors.io { it.unsubscribeSync() }
        }
        subscribeHolder.clear()
        eventReceiver.stop()
    }

    fun terminate() {
        subscribeHolder.stop()
    }

    fun getSubscribeService(subscriptionId: String): Service? {
        return subscribeHolder.getService(subscriptionId)
    }

    fun register(service: Service, timeout: Long, keep: Boolean) {
        subscribeHolder.add(service, timeout, keep)
    }

    fun renew(service: Service, timeout: Long) {
        subscribeHolder.renew(service, timeout)
    }

    fun setKeepRenew(service: Service, keep: Boolean) {
        subscribeHolder.setKeepRenew(service, keep)
    }

    fun unregister(service: Service) {
        subscribeHolder.remove(service)
    }
}
