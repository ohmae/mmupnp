/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager

import net.mm2d.upnp.Service

internal interface SubscribeManager {
    fun initialize()
    fun start()
    fun stop()
    fun terminate()
    suspend fun getEventPort(): Int
    suspend fun getSubscribeService(subscriptionId: String): Service?
    suspend fun register(service: Service, timeout: Long, keep: Boolean)
    suspend fun renew(service: Service, timeout: Long)
    suspend fun setKeepRenew(service: Service, keep: Boolean)
    suspend fun unregister(service: Service)
}
