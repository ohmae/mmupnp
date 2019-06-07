/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.upnp.StateVariable

/**
 * Implements for [StateVariable].
 *
 * @author [大前良介(OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class StateVariableImpl(
    override val isSendEvents: Boolean,
    override val isMulticast: Boolean,
    override val name: String,
    override val dataType: String,
    override val allowedValueList: List<String>,
    override val defaultValue: String?,
    override val minimum: String?,
    override val maximum: String?,
    override val step: String?
) : StateVariable {
    internal class Builder {
        private var sendEvents = true
        private var multicast = false
        private var name: String? = null
        private var dataType: String? = null
        private val allowedValueList = mutableListOf<String>()
        private var defaultValue: String? = null
        private var minimum: String? = null
        private var maximum: String? = null
        private var step: String? = null

        fun build(): StateVariableImpl {
            val name = name
                ?: throw IllegalStateException("name must be set.")
            val dataType = dataType
                ?: throw IllegalStateException("dataType must be set.")

            return StateVariableImpl(
                isSendEvents = sendEvents,
                isMulticast = multicast,
                name = name,
                dataType = dataType,
                allowedValueList = allowedValueList,
                defaultValue = defaultValue,
                minimum = minimum,
                maximum = maximum,
                step = step
            )
        }

        // 値が"no"でなければyesであると判定する。
        fun setSendEvents(sendEvents: String): Builder = apply {
            this.sendEvents = !sendEvents.equals("no", ignoreCase = true)
        }

        // 値が"yes"でなければnoであると判定する。Multicastの受信には非対応。
        fun setMulticast(multicast: String): Builder = apply {
            this.multicast = multicast.equals("yes", ignoreCase = true)
        }

        fun setName(name: String): Builder = apply {
            this.name = name
        }

        fun setDataType(dataType: String): Builder = apply {
            this.dataType = dataType
        }

        fun addAllowedValue(value: String): Builder = apply {
            allowedValueList.add(value)
        }

        fun setDefaultValue(defaultValue: String): Builder = apply {
            this.defaultValue = defaultValue
        }

        fun setMinimum(minimum: String): Builder = apply {
            this.minimum = minimum
        }

        fun setMaximum(maximum: String): Builder = apply {
            this.maximum = maximum
        }

        fun setStep(step: String): Builder = apply {
            this.step = step
        }
    }
}
