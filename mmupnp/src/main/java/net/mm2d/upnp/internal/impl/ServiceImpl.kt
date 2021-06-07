/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.upnp.Action
import net.mm2d.upnp.Service
import net.mm2d.upnp.StateVariable
import net.mm2d.upnp.internal.manager.SubscribeManager
import net.mm2d.upnp.internal.thread.TaskExecutors
import net.mm2d.upnp.log.Logger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Implements for [Service].
 *
 * @author [大前良介(OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class ServiceImpl(
    override val device: DeviceImpl,
    override val description: String,
    override val serviceType: String,
    override val serviceId: String,
    override val scpdUrl: String,
    override val controlUrl: String,
    override val eventSubUrl: String,
    actionBuilderList: List<ActionImpl.Builder>,
    stateVariables: List<StateVariable>
) : Service {
    private val subscribeManager: SubscribeManager = device.controlPoint.subscribeManager
    private val taskExecutors: TaskExecutors = device.controlPoint.taskExecutors
    private val actionMap: Map<String, Action>
    private val stateVariableMap = stateVariables.map { it.name to it }.toMap()

    // VisibleForTesting
    internal val subscribeDelegate: SubscribeDelegate by lazy { createSubscribeDelegate(this) }
    override val subscriptionId: String?
        get() = subscribeDelegate.subscriptionId

    init {
        actionMap = buildActionMap(this, stateVariableMap, actionBuilderList)
    }

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
    }

    internal class Builder {
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

        fun setServiceType(serviceType: String): Builder = apply {
            this.serviceType = serviceType
        }

        fun setServiceId(serviceId: String): Builder = apply {
            this.serviceId = serviceId
        }

        fun setScpdUrl(scpdUrl: String): Builder = apply {
            this.scpdUrl = scpdUrl
        }

        fun getScpdUrl(): String? = scpdUrl

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
