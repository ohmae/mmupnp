/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.upnp.Action
import net.mm2d.upnp.Argument

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
    // VisibleForTesting
    internal val invokeDelegate: ActionInvokeDelegate by lazy { createInvokeDelegate(this) }

    override val argumentList: List<Argument> by lazy {
        argumentMap.values.toList()
    }

    override fun findArgument(name: String): Argument? = argumentMap[name]

    override suspend fun invoke(
        argumentValues: Map<String, String?>,
        returnErrorResponse: Boolean
    ): Map<String, String> = invokeCustom(argumentValues, emptyMap(), emptyMap(), returnErrorResponse)

    override suspend fun invokeCustom(
        argumentValues: Map<String, String?>,
        customNamespace: Map<String, String>,
        customArguments: Map<String, String>,
        returnErrorResponse: Boolean
    ): Map<String, String> =
        invokeDelegate.invoke(argumentValues, customNamespace, customArguments, returnErrorResponse)

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
