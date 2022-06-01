/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.upnp.Http
import net.mm2d.upnp.SingleHttpClient
import net.mm2d.upnp.SingleHttpRequest
import net.mm2d.upnp.SingleHttpResponse
import net.mm2d.upnp.internal.manager.SubscribeManager
import net.mm2d.upnp.log.Logger
import net.mm2d.upnp.util.toAddressString
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

internal class SubscribeDelegate(
    private val service: ServiceImpl
) {
    private val device: DeviceImpl = service.device
    private val subscribeManager: SubscribeManager = device.controlPoint.subscribeManager
    var subscriptionId: String? = null
        private set

    internal val callback: String
        get() {
            val address = device.ssdpMessage.localAddress ?: return ""
            val port = subscribeManager.getEventPort()
            return "<http://${address.toAddressString(port)}/>"
        }

    private fun createHttpClient(): SingleHttpClient = SingleHttpClient.create(false)

    // VisibleForTesting
    @Throws(MalformedURLException::class)
    internal fun makeAbsoluteUrl(url: String): URL = Http.makeAbsoluteUrl(device.baseUrl, url, device.scopeId)

    suspend fun subscribe(keepRenew: Boolean): Boolean {
        try {
            val sId = subscriptionId
            if (!sId.isNullOrEmpty()) {
                if (renewSubscribeActual(sId)) {
                    subscribeManager.setKeepRenew(service, keepRenew)
                    return true
                }
                return false
            }
            return subscribeActual(keepRenew)
        } catch (e: IOException) {
            Logger.e(e, "fail to subscribe")
        }
        return false
    }

    // VisibleForTesting
    @Throws(IOException::class)
    internal fun subscribeActual(keepRenew: Boolean): Boolean {
        val request = makeSubscribeRequest()
        val response = createHttpClient().post(request)
        if (response.getStatus() != Http.Status.HTTP_OK) {
            Logger.w { "error subscribe request:\n$request\nresponse:\n$response" }
            return false
        }
        val sid = response.getHeader(Http.SID)
        val timeout = parseTimeout(response)
        if (sid.isNullOrEmpty() || timeout <= 0) {
            Logger.w { "error subscribe response:\n$response" }
            return false
        }
        Logger.v { "subscribe request:\n$request\nresponse:\n$response" }
        subscriptionId = sid
        subscribeManager.register(service, timeout, keepRenew)
        return true
    }

    @Throws(IOException::class)
    private fun makeSubscribeRequest(): SingleHttpRequest =
        SingleHttpRequest.create().apply {
            setMethod(Http.SUBSCRIBE)
            setUrl(makeAbsoluteUrl(service.eventSubUrl), true)
            setHeader(Http.NT, Http.UPNP_EVENT)
            setHeader(Http.CALLBACK, callback)
            setHeader(Http.TIMEOUT, "Second-300")
            setHeader(Http.CONTENT_LENGTH, "0")
        }

    suspend fun renewSubscribe(): Boolean {
        return try {
            val sId = subscriptionId
            if (sId.isNullOrEmpty()) subscribeActual(false)
            else renewSubscribeActual(sId)
        } catch (e: IOException) {
            Logger.e(e, "fail to renewSubscribe")
            false
        }
    }

    // VisibleForTesting
    @Throws(IOException::class)
    internal fun renewSubscribeActual(subscriptionId: String): Boolean {
        val request = makeRenewSubscribeRequest(subscriptionId)
        val response = createHttpClient().post(request)
        if (response.getStatus() != Http.Status.HTTP_OK) {
            Logger.w { "renewSubscribe request:\n$request\nresponse:\n$response" }
            return false
        }
        val sid = response.getHeader(Http.SID)
        val timeout = parseTimeout(response)
        if (sid != subscriptionId || timeout <= 0) {
            Logger.w { "renewSubscribe response:\n$response" }
            return false
        }
        Logger.v { "renew subscribe request:\n$request\nresponse:\n$response" }
        subscribeManager.renew(service, timeout)
        return true
    }

    @Throws(IOException::class)
    private fun makeRenewSubscribeRequest(subscriptionId: String): SingleHttpRequest =
        SingleHttpRequest.create().apply {
            setMethod(Http.SUBSCRIBE)
            setUrl(makeAbsoluteUrl(service.eventSubUrl), true)
            setHeader(Http.SID, subscriptionId)
            setHeader(Http.TIMEOUT, "Second-300")
            setHeader(Http.CONTENT_LENGTH, "0")
        }

    suspend fun unsubscribe(): Boolean {
        val sId = subscriptionId
        if (sId.isNullOrEmpty()) {
            return false
        }
        try {
            val request = makeUnsubscribeRequest(sId)
            val response = createHttpClient().post(request)
            subscribeManager.unregister(service)
            subscriptionId = null
            if (response.getStatus() != Http.Status.HTTP_OK) {
                Logger.w { "unsubscribe request:\n$request\nresponse:\n$response" }
                return false
            }
            Logger.v { "unsubscribe request:\n$request\nresponse:\n$response" }
            return true
        } catch (e: IOException) {
            Logger.w(e, "fail to unsubscribe")
        }
        return false
    }

    @Throws(IOException::class)
    private fun makeUnsubscribeRequest(subscriptionId: String): SingleHttpRequest =
        SingleHttpRequest.create().apply {
            setMethod(Http.UNSUBSCRIBE)
            setUrl(makeAbsoluteUrl(service.eventSubUrl), true)
            setHeader(Http.SID, subscriptionId)
            setHeader(Http.CONTENT_LENGTH, "0")
        }

    companion object {
        private val DEFAULT_SUBSCRIPTION_TIMEOUT = TimeUnit.SECONDS.toMillis(300)
        private const val SECOND_PREFIX = "second-"

        // VisibleForTesting
        internal fun parseTimeout(response: SingleHttpResponse): Long {
            val timeout = response.getHeader(Http.TIMEOUT)?.lowercase(Locale.ENGLISH)
            if (timeout.isNullOrEmpty() || timeout.contains("infinite")) {
                // infiniteはUPnP2.0でdeprecated扱い、有限な値にする。
                return DEFAULT_SUBSCRIPTION_TIMEOUT
            }
            val pos = timeout.indexOf(SECOND_PREFIX)
            if (pos < 0) {
                return DEFAULT_SUBSCRIPTION_TIMEOUT
            }
            val secondSection = timeout.substring(pos + SECOND_PREFIX.length)
                .toLongOrNull()
                ?: return DEFAULT_SUBSCRIPTION_TIMEOUT
            return TimeUnit.SECONDS.toMillis(secondSection)
        }
    }
}
