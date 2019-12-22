/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.impl

import net.mm2d.upnp.common.internal.property.ActionProperty
import net.mm2d.upnp.cp.Action
import net.mm2d.upnp.cp.Argument
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Implements for [Action].
 *
 * @author [大前良介(OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class ActionImpl(
    controlPoint: ControlPointImpl,
    property: ActionProperty,
    override val argumentList: List<ArgumentImpl>
) : Action {
    override lateinit var service: ServiceImpl
        internal set
    private val taskExecutors = controlPoint.taskExecutors
    // VisibleForTesting
    internal val invokeDelegate: ActionInvokeDelegate by lazy {
        createInvokeDelegate(this)
    }
    override val name: String = property.name
    internal val argumentMap: Map<String, Argument> by lazy {
        argumentList.map { it.name to it }.toMap()
    }

    override fun findArgument(name: String): Argument? = argumentMap[name]

    @Throws(IOException::class)
    override fun invokeSync(
        argumentValues: Map<String, String?>,
        returnErrorResponse: Boolean
    ): Map<String, String> = invokeCustomSync(argumentValues, emptyMap(), emptyMap(), returnErrorResponse)

    @Throws(IOException::class)
    override fun invokeCustomSync(
        argumentValues: Map<String, String?>,
        customNamespace: Map<String, String>,
        customArguments: Map<String, String>,
        returnErrorResponse: Boolean
    ): Map<String, String> = invokeDelegate.invoke(
        argumentValues, customNamespace, customArguments, returnErrorResponse
    )

    private fun invokeInner(
        argumentValues: Map<String, String?>,
        customNamespace: Map<String, String>,
        customArguments: Map<String, String>,
        returnErrorResponse: Boolean,
        onResult: (Map<String, String>) -> Unit,
        onError: (IOException) -> Unit
    ) {
        taskExecutors.io {
            try {
                onResult(invokeCustomSync(argumentValues, customNamespace, customArguments, returnErrorResponse))
            } catch (e: IOException) {
                onError(e)
            }
        }
    }

    override fun invoke(
        argumentValues: Map<String, String?>,
        returnErrorResponse: Boolean,
        onResult: ((Map<String, String>) -> Unit)?,
        onError: ((IOException) -> Unit)?
    ) = invokeCustom(argumentValues, emptyMap(), emptyMap(), returnErrorResponse, onResult, onError)

    override fun invokeCustom(
        argumentValues: Map<String, String?>,
        customNamespace: Map<String, String>,
        customArguments: Map<String, String>,
        returnErrorResponse: Boolean,
        onResult: ((Map<String, String>) -> Unit)?,
        onError: ((IOException) -> Unit)?
    ) = invokeInner(argumentValues, customNamespace, customArguments, returnErrorResponse, {
        onResult ?: return@invokeInner
        taskExecutors.callback { onResult(it) }
    }, {
        onError ?: return@invokeInner
        taskExecutors.callback { onError(it) }
    })

    override suspend fun invokeAsync(
        argumentValues: Map<String, String?>,
        returnErrorResponse: Boolean
    ): Map<String, String> = invokeCustomAsync(argumentValues, emptyMap(), emptyMap(), returnErrorResponse)

    override suspend fun invokeCustomAsync(
        argumentValues: Map<String, String?>,
        customNamespace: Map<String, String>,
        customArguments: Map<String, String>,
        returnErrorResponse: Boolean
    ): Map<String, String> = suspendCoroutine { continuation ->
        invokeInner(argumentValues, customNamespace, customArguments, returnErrorResponse,
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    companion object {
        // VisibleForTesting
        internal fun createInvokeDelegate(action: ActionImpl) = ActionInvokeDelegate(action)

        fun create(
            controlPoint: ControlPointImpl,
            property: ActionProperty,
            stateVariableList: List<StateVariableImpl>
        ): ActionImpl {
            val argumentList = property.argumentList.map { ArgumentImpl.create(it, stateVariableList) }
            return ActionImpl(controlPoint, property, argumentList)
        }
    }
}
