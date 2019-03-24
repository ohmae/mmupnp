/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.upnp.StateVariable
import net.mm2d.upnp.internal.parser.DeviceParser
import net.mm2d.upnp.internal.parser.ServiceParser

/**
 * [StateVariable]の実装
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

    /**
     * ServiceDescriptionのパース時に使用するビルダー
     *
     * @see DeviceParser.loadDescription
     * @see ServiceParser.loadDescription
     */
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

        /**
         * [StateVariable]のインスタンスを作成する。
         *
         * @return [StateVariable]のインスタンス
         */
        fun build(): StateVariable {
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

        /**
         * SendEventsの値を登録する。
         *
         * 値が"no"でなければyesであると判定する。
         *
         * @param sendEvents SendEventsの値
         */
        fun setSendEvents(sendEvents: String): Builder {
            this.sendEvents = !sendEvents.equals("no", ignoreCase = true)
            return this
        }

        /**
         * Multicastの値を登録する
         *
         * 値が"yes"でなければnoであると判定する。
         * なお、Multicastの受信には非対応である。
         *
         * @param multicast Multicastの値
         */
        fun setMulticast(multicast: String): Builder {
            this.multicast = multicast.equals("yes", ignoreCase = true)
            return this
        }

        /**
         * StateVariable名を登録する。
         *
         * @param name StateVariable名
         */
        fun setName(name: String): Builder {
            this.name = name
            return this
        }

        /**
         * DataTypeを登録する。
         *
         * @param dataType DataType
         */
        fun setDataType(dataType: String): Builder {
            this.dataType = dataType
            return this
        }

        /**
         * AllowedValueを登録する。
         *
         * @param value AllowedValue
         */
        fun addAllowedValue(value: String): Builder {
            allowedValueList.add(value)
            return this
        }

        /**
         * DefaultValueを登録する。
         *
         * @param defaultValue DefaultValue
         */
        fun setDefaultValue(defaultValue: String): Builder {
            this.defaultValue = defaultValue
            return this
        }

        /**
         * Minimumを登録する。
         *
         * @param minimum Minimum
         * @return Builder
         */
        fun setMinimum(minimum: String): Builder {
            this.minimum = minimum
            return this
        }

        /**
         * Maximumを登録する。
         *
         * @param maximum Maximum
         */
        fun setMaximum(maximum: String): Builder {
            this.maximum = maximum
            return this
        }

        /**
         * Stepを登録する。
         *
         * @param step Step
         */
        fun setStep(step: String): Builder {
            this.step = step
            return this
        }
    }
}
