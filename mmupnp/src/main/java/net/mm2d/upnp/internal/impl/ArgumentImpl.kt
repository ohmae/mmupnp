/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.upnp.Argument
import net.mm2d.upnp.StateVariable
import net.mm2d.upnp.internal.parser.DeviceParser
import net.mm2d.upnp.internal.parser.ServiceParser

/**
 * [Argument]の実装
 *
 * @author [大前良介(OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class ArgumentImpl(
    override val name: String,
    override val isInputDirection: Boolean,
    override val relatedStateVariable: StateVariable
) : Argument {

    /**
     * ServiceDescriptionのパース時に使用するビルダー
     *
     * @see DeviceParser.loadDescription
     * @see ServiceParser.loadDescription
     * @see ActionImpl.Builder
     */
    internal class Builder {
        private var name: String? = null
        private var inputDirection: Boolean = false
        private var relatedStateVariableName: String? = null
        private var relatedStateVariable: StateVariable? = null

        /**
         * [Argument]のインスタンスを作成する。
         *
         * @return [Argument]のインスタンス
         * @throws IllegalStateException 必須パラメータが設定されていない場合
         */
        @Throws(IllegalStateException::class)
        fun build(): Argument {
            val name = name
                ?: throw IllegalStateException("name must be set.")
            val relatedStateVariable = relatedStateVariable
                ?: throw IllegalStateException("related state variable must be set.")
            return ArgumentImpl(
                name = name,
                isInputDirection = inputDirection,
                relatedStateVariable = relatedStateVariable
            )
        }

        /**
         * Argument名を登録する。
         *
         * @param name Argument名
         */
        fun setName(name: String): Builder {
            this.name = name
            return this
        }

        /**
         * Directionの値を登録する
         *
         * "in"の場合のみinput、それ以外をoutputと判定する。
         *
         * @param direction Directionの値
         */
        fun setDirection(direction: String): Builder {
            inputDirection = "in".equals(direction, ignoreCase = true)
            return this
        }

        /**
         * RelatedStateVariableの値を登録する。
         *
         * @param name RelatedStateVariableの値
         */
        fun setRelatedStateVariableName(name: String): Builder {
            relatedStateVariableName = name
            return this
        }

        /**
         * RelatedStateVariableの値を返す。
         *
         * @return RelatedStateVariableの値
         */
        fun getRelatedStateVariableName(): String? {
            return relatedStateVariableName
        }

        /**
         * RelatedStateVariableので指定されたStateVariableのインスタンスを登録する。
         *
         * @param variable StateVariableのインスタンス
         */
        fun setRelatedStateVariable(variable: StateVariable): Builder {
            relatedStateVariable = variable
            return this
        }

    }
}
