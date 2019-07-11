/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.log.Logger
import net.mm2d.upnp.*
import net.mm2d.upnp.internal.manager.SubscribeManager
import net.mm2d.upnp.util.toAddressString
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Implements for [Service].
 *
 * @author [大前良介(OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class ServiceImpl(
    override val device: DeviceImpl,
    private val subscribeManager: SubscribeManager,
    override val description: String,
    override val serviceType: String,
    override val serviceId: String,
    override val scpdUrl: String,
    override val controlUrl: String,
    override val eventSubUrl: String,
    actionBuilderList: List<ActionImpl.Builder>,
    stateVariables: List<StateVariable>
) : Service {
    private val actionMap: Map<String, Action>
    private val stateVariableMap = stateVariables.map { it.name to it }.toMap()
    override var subscriptionId: String? = null
        private set

    init {
        actionMap = buildActionMap(this, stateVariableMap, actionBuilderList)
    }

    override val actionList: List<Action> by lazy {
        actionMap.values.toList()
    }
    override val stateVariableList: List<StateVariable> by lazy {
        stateVariableMap.values.toList()
    }

    // VisibleForTesting
    internal val callback: String
        get() {
            val address = device.ssdpMessage.localAddress ?: return ""
            val port = subscribeManager.getEventPort()
            return "<http://${address.toAddressString(port)}/>"
        }

    // VisibleForTesting
    @Throws(MalformedURLException::class)
    internal fun makeAbsoluteUrl(url: String): URL = Http.makeAbsoluteUrl(device.baseUrl, url, device.scopeId)

    override fun findAction(name: String): Action? = actionMap[name]

    override fun findStateVariable(name: String?): StateVariable? = stateVariableMap[name]

    // VisibleForTesting
    internal fun createHttpClient(): HttpClient = HttpClient(false)

    override fun subscribeSync(keepRenew: Boolean): Boolean {
        try {
            if (!subscriptionId.isNullOrEmpty()) {
                if (renewSubscribeInner()) {
                    subscribeManager.setKeepRenew(this, keepRenew)
                    return true
                }
                return false
            }
            return subscribeInner(keepRenew)
        } catch (e: IOException) {
            Logger.e(e, "fail to subscribe")
        }
        return false
    }

    // VisibleForTesting
    @Throws(IOException::class)
    internal fun subscribeInner(keepRenew: Boolean): Boolean {
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
        subscribeManager.register(this, timeout, keepRenew)
        return true
    }

    @Throws(IOException::class)
    private fun makeSubscribeRequest(): HttpRequest =
        HttpRequest.create().apply {
            setMethod(Http.SUBSCRIBE)
            setUrl(makeAbsoluteUrl(eventSubUrl), true)
            setHeader(Http.NT, Http.UPNP_EVENT)
            setHeader(Http.CALLBACK, callback)
            setHeader(Http.TIMEOUT, "Second-300")
            setHeader(Http.CONTENT_LENGTH, "0")
        }

    override fun renewSubscribeSync(): Boolean {
        try {
            return if (subscriptionId.isNullOrEmpty()) {
                subscribeInner(false)
            } else renewSubscribeInner()
        } catch (e: IOException) {
            Logger.e(e, "fail to renewSubscribe")
        }
        return false
    }

    // VisibleForTesting
    @Throws(IOException::class)
    internal fun renewSubscribeInner(): Boolean {
        val request = makeRenewSubscribeRequest(subscriptionId!!)
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
        subscribeManager.renew(this, timeout)
        return true
    }

    @Throws(IOException::class)
    private fun makeRenewSubscribeRequest(subscriptionId: String): HttpRequest =
        HttpRequest.create().apply {
            setMethod(Http.SUBSCRIBE)
            setUrl(makeAbsoluteUrl(eventSubUrl), true)
            setHeader(Http.SID, subscriptionId)
            setHeader(Http.TIMEOUT, "Second-300")
            setHeader(Http.CONTENT_LENGTH, "0")
        }

    override fun unsubscribeSync(): Boolean {
        if (subscriptionId.isNullOrEmpty()) {
            return false
        }
        try {
            val request = makeUnsubscribeRequest(subscriptionId!!)
            val response = createHttpClient().post(request)
            subscribeManager.unregister(this)
            subscriptionId = null
            if (response.getStatus() != Http.Status.HTTP_OK) {
                Logger.w { "unsubscribe request:\n$request\nresponse:\n$response" }
                return false
            }
            Logger.v { "unsubscribe request:\n$request\nresponse:\n$response" }
            return true
        } catch (e: IOException) {
            Logger.w(e, "fail to subscribe")
        }
        return false
    }

    @Throws(IOException::class)
    private fun makeUnsubscribeRequest(subscriptionId: String): HttpRequest =
        HttpRequest.create().apply {
            setMethod(Http.UNSUBSCRIBE)
            setUrl(makeAbsoluteUrl(eventSubUrl), true)
            setHeader(Http.SID, subscriptionId)
            setHeader(Http.CONTENT_LENGTH, "0")
        }

    override fun subscribe(keepRenew: Boolean, callback: ((Boolean) -> Unit)?) {
        val executors = device.controlPoint.taskExecutors
        executors.io {
            val result = subscribeSync(keepRenew)
            callback?.let { executors.callback { it(result) } }
        }
    }

    override fun renewSubscribe(callback: ((Boolean) -> Unit)?) {
        val executors = device.controlPoint.taskExecutors
        executors.io {
            val result = renewSubscribeSync()
            callback?.let { executors.callback { it(result) } }
        }
    }

    override fun unsubscribe(callback: ((Boolean) -> Unit)?) {
        val executors = device.controlPoint.taskExecutors
        executors.io {
            val result = unsubscribeSync()
            callback?.let { executors.callback { it(result) } }
        }
    }

    override suspend fun subscribeAsync(keepRenew: Boolean): Boolean =
        suspendCoroutine { continuation ->
            device.controlPoint.taskExecutors.io {
                continuation.resume(subscribeSync(keepRenew))
            }
        }

    override suspend fun renewSubscribeAsync(): Boolean =
        suspendCoroutine { continuation ->
            device.controlPoint.taskExecutors.io {
                continuation.resume(renewSubscribeSync())
            }
        }

    override suspend fun unsubscribeAsync(): Boolean =
        suspendCoroutine { continuation ->
            device.controlPoint.taskExecutors.io {
                continuation.resume(unsubscribeSync())
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
        private val DEFAULT_SUBSCRIPTION_TIMEOUT = TimeUnit.SECONDS.toMillis(300)
        private const val SECOND_PREFIX = "second-"

        @Throws(IllegalStateException::class)
        private fun buildActionMap(
            service: ServiceImpl,
            variableMap: Map<String, StateVariable>,
            builderList: List<ActionImpl.Builder>
        ): Map<String, Action> {
            if (builderList.isEmpty()) {
                return emptyMap()
            }
            builderList.forEach { builder ->
                builder.setService(service)
                builder.getArgumentBuilderList().forEach {
                    it.setRelatedStateVariable(variableMap)
                }
            }
            return builderList
                .map { it.build() }
                .map { it.name to it }
                .toMap()
        }

        @Throws(IllegalStateException::class)
        private fun ArgumentImpl.Builder.setRelatedStateVariable(
            variableMap: Map<String, StateVariable>
        ) {
            val name = getRelatedStateVariableName()
                ?: throw IllegalStateException("relatedStateVariable name is null")
            val variable = variableMap[name] ?: repairInvalidFormatAndGet(name, variableMap)
            setRelatedStateVariable(variable)
        }

        // Implement the remedies because there is a device that has the wrong format of XML
        // That indented in the text content.
        // e.g. AN-WLTU1
        @Throws(IllegalStateException::class)
        private fun ArgumentImpl.Builder.repairInvalidFormatAndGet(
            name: String,
            variableMap: Map<String, StateVariable>
        ): StateVariable {
            val trimmedName = name.trim()
            val trimmedVariable = variableMap[trimmedName]
                ?: throw IllegalStateException("There is no StateVariable [$name]")
            setRelatedStateVariableName(trimmedName)
            Logger.i { "Invalid description. relatedStateVariable name has unnecessary blanks [$name]" }
            return trimmedVariable
        }

        // VisibleForTesting
        internal fun parseTimeout(response: HttpResponse): Long {
            val timeout = response.getHeader(Http.TIMEOUT)?.toLowerCase(Locale.ENGLISH)
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

    internal class Builder {
        private var subscribeManager: SubscribeManager? = null
        private var device: DeviceImpl? = null
        private var serviceType: String? = null
        private var serviceId: String? = null
        private var scpdUrl: String? = null
        private var controlUrl: String? = null
        private var eventSubUrl: String? = null
        private var description: String? = null
        private val actionBuilderList = mutableListOf<ActionImpl.Builder>()
        private val stateVariables = mutableListOf<StateVariable>()

        @Throws(IllegalStateException::class)
        fun build(): ServiceImpl {
            val device = device
                ?: throw IllegalStateException("device must be set.")
            val subscribeManager = subscribeManager
                ?: throw IllegalStateException("subscribeManager must be set.")
            val serviceType = serviceType
                ?: throw IllegalStateException("serviceType must be set.")
            val serviceId = serviceId
                ?: throw IllegalStateException("serviceId must be set.")
            val scpdUrl = scpdUrl
                ?: throw IllegalStateException("SCPDURL must be set.")
            val controlUrl = controlUrl
                ?: throw IllegalStateException("controlURL must be set.")
            val eventSubUrl = eventSubUrl
                ?: throw IllegalStateException("eventSubURL must be set.")
            val description = description ?: ""
            return ServiceImpl(
                device = device,
                subscribeManager = subscribeManager,
                serviceType = serviceType,
                serviceId = serviceId,
                scpdUrl = scpdUrl,
                controlUrl = controlUrl,
                eventSubUrl = eventSubUrl,
                description = description,
                actionBuilderList = actionBuilderList,
                stateVariables = stateVariables
            )
        }

        fun setDevice(device: DeviceImpl): Builder = apply {
            this.device = device
        }

        fun setSubscribeManager(manager: SubscribeManager): Builder = apply {
            subscribeManager = manager
        }

        fun setServiceType(serviceType: String): Builder = apply {
            this.serviceType = serviceType
        }

        fun setServiceId(serviceId: String): Builder = apply {
            this.serviceId = serviceId
        }

        fun setScpdUrl(scpdUrl: String): Builder = apply {
            this.scpdUrl = scpdUrl
        }

        fun getScpdUrl(): String? {
            return scpdUrl
        }

        fun setControlUrl(controlUrl: String): Builder = apply {
            this.controlUrl = controlUrl
        }

        fun setEventSubUrl(eventSubUrl: String): Builder = apply {
            this.eventSubUrl = eventSubUrl
        }

        fun setDescription(description: String): Builder = apply {
            this.description = description
        }

        fun addActionBuilder(builder: ActionImpl.Builder): Builder = apply {
            actionBuilderList.add(builder)
        }

        fun addStateVariable(builder: StateVariable): Builder = apply {
            stateVariables.add(builder)
        }

        fun toDumpString(): String = "ServiceBuilder:\n" +
                "serviceType:$serviceType\n" +
                "serviceId:$serviceId\n" +
                "SCPDURL:$scpdUrl\n" +
                "eventSubURL:$eventSubUrl\n" +
                "controlURL:$controlUrl\n" +
                "DESCRIPTION:$description"
    }
}
