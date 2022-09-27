/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager

import net.mm2d.upnp.Service

internal class EmptySubscribeManager : SubscribeManager {
    override fun initialize() = Unit
    override fun start() = Unit
    override fun stop() = Unit
    override fun terminate() = Unit
    override suspend fun getEventPort(): Int = 0
    override suspend fun getSubscribeService(subscriptionId: String): Service? = null
    override suspend fun register(service: Service, timeout: Long, keep: Boolean) = Unit
    override suspend fun renew(service: Service, timeout: Long) = Unit
    override suspend fun setKeepRenew(service: Service, keep: Boolean) = Unit
    override suspend fun unregister(service: Service) = Unit
}
