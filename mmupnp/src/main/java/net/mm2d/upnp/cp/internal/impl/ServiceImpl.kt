/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.impl

import net.mm2d.upnp.common.internal.property.ServiceProperty
import net.mm2d.upnp.common.internal.thread.TaskExecutors
import net.mm2d.upnp.cp.Action
import net.mm2d.upnp.cp.Service
import net.mm2d.upnp.cp.StateVariable
import net.mm2d.upnp.cp.internal.manager.SubscribeManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Implements for [Service].
 *
 * @author [大前良介(OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class ServiceImpl(
    override val device: DeviceImpl,
    private val property: ServiceProperty
) : Service {
    private val subscribeManager: SubscribeManager = device.controlPoint.subscribeManager
    private val taskExecutors: TaskExecutors = device.controlPoint.taskExecutors
    override val description: String = property.description
    override val serviceType: String = property.serviceType
    override val serviceId: String = property.serviceId
    override val scpdUrl: String = property.scpdUrl
    override val controlUrl: String = property.controlUrl
    override val eventSubUrl: String = property.eventSubUrl
    private val stateVariableMap: Map<String, StateVariable> = property.stateVariableList
        .map { StateVariableImpl(it) }
        .map { it.name to it }
        .toMap()
    private val actionMap: Map<String, Action> = property.actionList
        .map { ActionImpl(this, it, stateVariableMap) }
        .map { it.name to it }
        .toMap()
    // VisibleForTesting
    internal val subscribeDelegate: SubscribeDelegate by lazy { createSubscribeDelegate(this) }
    override val subscriptionId: String?
        get() = subscribeDelegate.subscriptionId

    override val actionList: List<Action> by lazy {
        actionMap.values.toList()
    }
    override val stateVariableList: List<StateVariable> by lazy {
        stateVariableMap.values.toList()
    }

    override fun findAction(name: String): Action? = actionMap[name]

    override fun findStateVariable(name: String?): StateVariable? = stateVariableMap[name]

    private fun subscribeInner(keepRenew: Boolean, callback: (Boolean) -> Unit) {
        taskExecutors.io { callback(subscribeDelegate.subscribe(keepRenew)) }
    }

    private fun renewSubscribeInner(callback: (Boolean) -> Unit) {
        taskExecutors.io { callback(subscribeDelegate.renewSubscribe()) }
    }

    private fun unsubscribeInner(callback: (Boolean) -> Unit) {
        taskExecutors.io { callback(subscribeDelegate.unsubscribe()) }
    }

    override fun subscribeSync(keepRenew: Boolean): Boolean {
        subscribeManager.checkEnabled()
        return subscribeDelegate.subscribe(keepRenew)
    }

    override fun renewSubscribeSync(): Boolean {
        subscribeManager.checkEnabled()
        return subscribeDelegate.renewSubscribe()
    }

    override fun unsubscribeSync(): Boolean {
        subscribeManager.checkEnabled()
        return subscribeDelegate.unsubscribe()
    }

    override fun subscribe(keepRenew: Boolean, callback: ((Boolean) -> Unit)?) {
        subscribeManager.checkEnabled()
        subscribeInner(keepRenew) {
            callback ?: return@subscribeInner
            taskExecutors.callback { callback(it) }
        }
    }

    override fun renewSubscribe(callback: ((Boolean) -> Unit)?) {
        subscribeManager.checkEnabled()
        renewSubscribeInner {
            callback ?: return@renewSubscribeInner
            taskExecutors.callback { callback(it) }
        }
    }

    override fun unsubscribe(callback: ((Boolean) -> Unit)?) {
        subscribeManager.checkEnabled()
        unsubscribeInner {
            callback ?: return@unsubscribeInner
            taskExecutors.callback { callback(it) }
        }
    }

    override suspend fun subscribeAsync(keepRenew: Boolean): Boolean {
        subscribeManager.checkEnabled()
        return suspendCoroutine { continuation ->
            subscribeInner(keepRenew) { continuation.resume(it) }
        }
    }

    override suspend fun renewSubscribeAsync(): Boolean {
        subscribeManager.checkEnabled()
        return suspendCoroutine { continuation ->
            renewSubscribeInner { continuation.resume(it) }
        }
    }

    override suspend fun unsubscribeAsync(): Boolean {
        subscribeManager.checkEnabled()
        return suspendCoroutine { continuation ->
            unsubscribeInner { continuation.resume(it) }
        }
    }

    override fun hashCode(): Int = device.hashCode() + serviceId.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is Service) return false
        return device == other.device && serviceId == other.serviceId
    }

    companion object {
        // VisibleForTesting
        internal fun createSubscribeDelegate(service: ServiceImpl) = SubscribeDelegate(service)
    }
}
