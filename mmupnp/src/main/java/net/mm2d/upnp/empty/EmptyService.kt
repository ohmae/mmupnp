/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty

import net.mm2d.upnp.Action
import net.mm2d.upnp.ControlPoints
import net.mm2d.upnp.Device
import net.mm2d.upnp.Service
import net.mm2d.upnp.StateVariable

/**
 * Empty implementation of [Service].
 */
object EmptyService : Service {
    override val device: Device = ControlPoints.emptyDevice()
    override val serviceType: String = ""
    override val serviceId: String = ""
    override val scpdUrl: String = ""
    override val controlUrl: String = ""
    override val eventSubUrl: String = ""
    override val description: String = ""
    override val actionList: List<Action> = emptyList()
    override val stateVariableList: List<StateVariable> = emptyList()
    override val subscriptionId: String? = null

    override fun findAction(name: String): Action? = null
    override fun findStateVariable(name: String?): StateVariable? = null
    override fun subscribeSync(keepRenew: Boolean): Boolean = false
    override fun renewSubscribeSync(): Boolean = false
    override fun unsubscribeSync(): Boolean = false
    override suspend fun subscribeAsync(keepRenew: Boolean): Boolean = false
    override suspend fun renewSubscribeAsync(): Boolean = false
    override suspend fun unsubscribeAsync(): Boolean = false

    override fun subscribe(keepRenew: Boolean, callback: ((Boolean) -> Unit)?) {
        callback?.invoke(false)
    }

    override fun renewSubscribe(callback: ((Boolean) -> Unit)?) {
        callback?.invoke(false)
    }

    override fun unsubscribe(callback: ((Boolean) -> Unit)?) {
        callback?.invoke(false)
    }
}
