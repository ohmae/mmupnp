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
 * [Service]の実装
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
    internal fun makeAbsoluteUrl(url: String): URL {
        return Http.makeAbsoluteUrl(device.baseUrl, url, device.scopeId)
    }

    override fun findAction(name: String): Action? {
        return actionMap[name]
    }

    override fun findStateVariable(name: String?): StateVariable? {
        return stateVariableMap[name]
    }

    // VisibleForTesting
    internal fun createHttpClient(): HttpClient {
        return HttpClient(false)
    }

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
            Logger.e("fail to subscribe", e)
        }
        return false
    }

    // VisibleForTesting
    @Throws(IOException::class)
    internal fun subscribeInner(keepRenew: Boolean): Boolean {
        val request = makeSubscribeRequest()
        val response = createHttpClient().post(request)
        if (response.status != Http.Status.HTTP_OK) {
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
    private fun makeSubscribeRequest(): HttpRequest {
        return HttpRequest.create().apply {
            setMethod(Http.SUBSCRIBE)
            setUrl(makeAbsoluteUrl(eventSubUrl), true)
            setHeader(Http.NT, Http.UPNP_EVENT)
            setHeader(Http.CALLBACK, callback)
            setHeader(Http.TIMEOUT, "Second-300")
            setHeader(Http.CONTENT_LENGTH, "0")
        }
    }

    override fun renewSubscribeSync(): Boolean {
        try {
            return if (subscriptionId.isNullOrEmpty()) {
                subscribeInner(false)
            } else renewSubscribeInner()
        } catch (e: IOException) {
            Logger.e("fail to renewSubscribe", e)
        }
        return false
    }

    // VisibleForTesting
    @Throws(IOException::class)
    internal fun renewSubscribeInner(): Boolean {
        val request = makeRenewSubscribeRequest(subscriptionId!!)
        val response = createHttpClient().post(request)
        if (response.status != Http.Status.HTTP_OK) {
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
    private fun makeRenewSubscribeRequest(subscriptionId: String): HttpRequest {
        return HttpRequest.create().apply {
            setMethod(Http.SUBSCRIBE)
            setUrl(makeAbsoluteUrl(eventSubUrl), true)
            setHeader(Http.SID, subscriptionId)
            setHeader(Http.TIMEOUT, "Second-300")
            setHeader(Http.CONTENT_LENGTH, "0")
        }
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
            if (response.status != Http.Status.HTTP_OK) {
                Logger.w { "unsubscribe request:\n$request\nresponse:\n$response" }
                return false
            }
            Logger.v { "unsubscribe request:\n$request\nresponse:\n$response" }
            return true
        } catch (e: IOException) {
            Logger.e("fail to subscribe", e)
        }
        return false
    }

    @Throws(IOException::class)
    private fun makeUnsubscribeRequest(subscriptionId: String): HttpRequest {
        return HttpRequest.create().apply {
            setMethod(Http.UNSUBSCRIBE)
            setUrl(makeAbsoluteUrl(eventSubUrl), true)
            setHeader(Http.SID, subscriptionId)
            setHeader(Http.CONTENT_LENGTH, "0")
        }
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

    override suspend fun subscribeAsync(keepRenew: Boolean): Boolean {
        return suspendCoroutine { continuation ->
            device.controlPoint.taskExecutors.io {
                val result = subscribeSync(keepRenew)
                continuation.resume(result)
            }
        }
    }

    override suspend fun renewSubscribeAsync(): Boolean {
        return suspendCoroutine { continuation ->
            device.controlPoint.taskExecutors.io {
                val result = renewSubscribeSync()
                continuation.resume(result)
            }
            renewSubscribe { continuation.resume(it) }
        }
    }

    override suspend fun unsubscribeAsync(): Boolean {
        return suspendCoroutine { continuation ->
            device.controlPoint.taskExecutors.io {
                val result = unsubscribeSync()
                continuation.resume(result)
            }
        }
    }

    override fun hashCode(): Int {
        return device.hashCode() + serviceId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is Service) return false
        return device == other.device && serviceId == other.serviceId
    }

    companion object {
        private val DEFAULT_SUBSCRIPTION_TIMEOUT = TimeUnit.SECONDS.toMillis(300)
        private const val SECOND_PREFIX = "second-"

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
                    it.setRelatedStateVariable(service, variableMap)
                }
            }
            return builderList
                .map { it.build() }
                .map { it.name to it }
                .toMap()
        }

        private fun ArgumentImpl.Builder.setRelatedStateVariable(
            service: Service,
            variableMap: Map<String, StateVariable>
        ) {
            val name = getRelatedStateVariableName()
                ?: throw IllegalStateException("relatedStateVariable name is null")
            val variable = variableMap[name] ?: {
                // for AN-WLTU1
                val trimmedName = name.trim { it <= ' ' }
                val trimmedVariable = variableMap[trimmedName]
                    ?: throw IllegalStateException("There is no StateVariable $name")
                setRelatedStateVariableName(trimmedName)
                Logger.w { "Invalid description. relatedStateVariable name has unnecessary blanks [$name] on ${service.serviceId}" }
                trimmedVariable
            }.invoke()
            setRelatedStateVariable(variable)
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

    /**
     * DeviceDescriptionのパース時に使用するビルダー
     */
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

        /**
         * Serviceのインスタンスを作成する。
         *
         * @return Serviceのインスタンス
         * @throws IllegalStateException 必須パラメータが設定されていない場合
         */
        @Throws(IllegalStateException::class)
        fun build(): Service {
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

        /**
         * このServiceを保持するDeviceを登録する。
         *
         * @param device このServiceを保持するDevice
         */
        fun setDevice(device: DeviceImpl): Builder {
            this.device = device
            return this
        }

        /**
         * 購読状態マネージャを設定する。
         *
         * @param manager 購読状態マネージャ
         */
        fun setSubscribeManager(manager: SubscribeManager): Builder {
            subscribeManager = manager
            return this
        }

        /**
         * serviceTypeを登録する。
         *
         * @param serviceType serviceType
         */
        fun setServiceType(serviceType: String): Builder {
            this.serviceType = serviceType
            return this
        }

        /**
         * serviceIdを登録する
         *
         * @param serviceId serviceId
         */
        fun setServiceId(serviceId: String): Builder {
            this.serviceId = serviceId
            return this
        }

        /**
         * SCPDURLを登録する
         *
         * @param scpdUrl ScpdURL
         */
        fun setScpdUrl(scpdUrl: String): Builder {
            this.scpdUrl = scpdUrl
            return this
        }

        fun getScpdUrl(): String? {
            return scpdUrl
        }

        /**
         * controlURLを登録する。
         *
         * @param controlUrl controlURL
         */
        fun setControlUrl(controlUrl: String): Builder {
            this.controlUrl = controlUrl
            return this
        }

        /**
         * eventSubURLを登録する。
         *
         * @param eventSubUrl eventSubURL
         */
        fun setEventSubUrl(eventSubUrl: String): Builder {
            this.eventSubUrl = eventSubUrl
            return this
        }

        /**
         * Description XMLを登録する。
         *
         * @param description Description XML全内容
         */
        fun setDescription(description: String): Builder {
            this.description = description
            return this
        }

        /**
         * ActionのBuilderを登録する。
         *
         * @param builder Serviceで定義されているActionのBuilder
         */
        fun addActionBuilder(builder: ActionImpl.Builder): Builder {
            actionBuilderList.add(builder)
            return this
        }

        /**
         * StateVariableのBuilderを登録する。
         *
         * @param builder Serviceで定義されているStateVariableのBuilder
         */
        fun addStateVariable(builder: StateVariable): Builder {
            stateVariables.add(builder)
            return this
        }

        fun toDumpString(): String {
            return "ServiceBuilder:\n" +
                    "serviceType:$serviceType\n" +
                    "serviceId:$serviceId\n" +
                    "SCPDURL:$scpdUrl\n" +
                    "eventSubURL:$eventSubUrl\n" +
                    "controlURL:$controlUrl\n" +
                    "DESCRIPTION:$description"
        }
    }
}
