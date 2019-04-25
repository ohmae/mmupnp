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
        fun setSendEvents(sendEvents: String): Builder {
            this.sendEvents = !sendEvents.equals("no", ignoreCase = true)
            return this
        }

        // 値が"yes"でなければnoであると判定する。Multicastの受信には非対応。
        fun setMulticast(multicast: String): Builder {
            this.multicast = multicast.equals("yes", ignoreCase = true)
            return this
        }

        fun setName(name: String): Builder {
            this.name = name
            return this
        }

        fun setDataType(dataType: String): Builder {
            this.dataType = dataType
            return this
        }

        fun addAllowedValue(value: String): Builder {
            allowedValueList.add(value)
            return this
        }

        fun setDefaultValue(defaultValue: String): Builder {
            this.defaultValue = defaultValue
            return this
        }

        fun setMinimum(minimum: String): Builder {
            this.minimum = minimum
            return this
        }

        fun setMaximum(maximum: String): Builder {
            this.maximum = maximum
            return this
        }

        fun setStep(step: String): Builder {
            this.step = step
            return this
        }
    }
}
