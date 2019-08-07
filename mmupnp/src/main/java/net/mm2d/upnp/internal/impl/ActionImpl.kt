/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.upnp.Action
import net.mm2d.upnp.Argument
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
    override val service: ServiceImpl,
    override val name: String,
    internal val argumentMap: Map<String, Argument>
) : Action {
    private val taskExecutors = service.device.controlPoint.taskExecutors
    // VisibleForTesting
    internal val invokeDelegate: ActionInvokeDelegate by lazy { createInvokeDelegate(this) }

    override val argumentList: List<Argument> by lazy {
        argumentMap.values.toList()
    }

    override fun findArgument(name: String): Argument? = argumentMap[name]

    @Throws(IOException::class)
    override fun invokeSync(
        argumentValues: Map<String, String?>,
        returnErrorResponse: Boolean
    ): Map<String, String> = invokeDelegate.invoke(argumentValues, returnErrorResponse)

    @Throws(IOException::class)
    override fun invokeCustomSync(
        argumentValues: Map<String, String?>,
        customNamespace: Map<String, String>,
        customArguments: Map<String, String>,
        returnErrorResponse: Boolean
    ): Map<String, String> = invokeDelegate.invokeCustom(
        argumentValues, customNamespace, customArguments, returnErrorResponse
    )

    private fun invokeInner(
        argumentValues: Map<String, String?>,
        returnErrorResponse: Boolean,
        onResult: (Map<String, String>) -> Unit,
        onError: (IOException) -> Unit
    ) {
        taskExecutors.io {
            try {
                onResult(invokeSync(argumentValues, returnErrorResponse))
            } catch (e: IOException) {
                onError(e)
            }
        }
    }

    private fun invokeCustomInner(
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
    ) {
        invokeInner(argumentValues, returnErrorResponse, {
            onResult ?: return@invokeInner
            taskExecutors.callback { onResult(it) }
        }, {
            onError ?: return@invokeInner
            taskExecutors.callback { onError(it) }
        })
    }

    override fun invokeCustom(
        argumentValues: Map<String, String?>,
        customNamespace: Map<String, String>,
        customArguments: Map<String, String>,
        returnErrorResponse: Boolean,
        onResult: ((Map<String, String>) -> Unit)?,
        onError: ((IOException) -> Unit)?
    ) {
        invokeCustomInner(argumentValues, customNamespace, customArguments, returnErrorResponse, {
            onResult ?: return@invokeCustomInner
            taskExecutors.callback { onResult(it) }
        }, {
            onError ?: return@invokeCustomInner
            taskExecutors.callback { onError(it) }
        })
    }

    override suspend fun invokeAsync(
        argumentValues: Map<String, String?>,
        returnErrorResponse: Boolean
    ): Map<String, String> =
        suspendCoroutine { continuation ->
            invokeInner(argumentValues, returnErrorResponse,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }

    override suspend fun invokeCustomAsync(
        argumentValues: Map<String, String?>,
        customNamespace: Map<String, String>,
        customArguments: Map<String, String>,
        returnErrorResponse: Boolean
    ): Map<String, String> =
        suspendCoroutine { continuation ->
            invokeCustomInner(argumentValues, customNamespace, customArguments, returnErrorResponse,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }

    class Builder {
        private var service: ServiceImpl? = null
        private var name: String? = null
        private val argumentList: MutableList<ArgumentImpl.Builder> = mutableListOf()

        @Throws(IllegalStateException::class)
        fun build(): ActionImpl {
            val service = service
                ?: throw IllegalStateException("service must be set.")
            val name = name
                ?: throw IllegalStateException("name must be set.")
            return ActionImpl(
                service = service,
                name = name,
                argumentMap = argumentList
                    .map { it.build() }
                    .map { it.name to it }
                    .toMap()
            )
        }

        fun getArgumentBuilderList(): List<ArgumentImpl.Builder> = argumentList

        fun setService(service: ServiceImpl): Builder = apply {
            this.service = service
        }

        fun setName(name: String): Builder = apply {
            this.name = name
        }

        // Actionのインスタンス作成後にArgumentを登録することはできない
        fun addArgumentBuilder(argument: ArgumentImpl.Builder): Builder = apply {
            argumentList.add(argument)
        }
    }

    companion object {
        // VisibleForTesting
        internal fun createInvokeDelegate(action: ActionImpl) = ActionInvokeDelegate(action)
    }
}
