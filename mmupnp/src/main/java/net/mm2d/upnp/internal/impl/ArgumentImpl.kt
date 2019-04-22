/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.upnp.Argument
import net.mm2d.upnp.StateVariable

/**
 * Implements for [Argument].
 *
 * @author [大前良介(OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class ArgumentImpl(
    override val name: String,
    override val isInputDirection: Boolean,
    override val relatedStateVariable: StateVariable
) : Argument {
    internal class Builder {
        private var name: String? = null
        private var inputDirection: Boolean = false
        private var relatedStateVariableName: String? = null
        private var relatedStateVariable: StateVariable? = null

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

        fun setName(name: String): Builder {
            this.name = name
            return this
        }

        fun setDirection(direction: String): Builder {
            inputDirection = "in".equals(direction, ignoreCase = true)
            return this
        }

        fun setRelatedStateVariableName(name: String): Builder {
            relatedStateVariableName = name
            return this
        }

        fun getRelatedStateVariableName(): String? {
            return relatedStateVariableName
        }

        fun setRelatedStateVariable(variable: StateVariable): Builder {
            relatedStateVariable = variable
            return this
        }
    }
}
