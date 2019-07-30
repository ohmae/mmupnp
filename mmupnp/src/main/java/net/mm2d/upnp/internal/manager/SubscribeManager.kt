/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager

import net.mm2d.upnp.Service

interface SubscribeManager {
    fun getEventPort(): Int
    fun initialize()
    fun start()
    fun stop()
    fun terminate()
    fun getSubscribeService(subscriptionId: String): Service?
    fun register(service: Service, timeout: Long, keep: Boolean)
    fun renew(service: Service, timeout: Long)
    fun setKeepRenew(service: Service, keep: Boolean)
    fun unregister(service: Service)
}
