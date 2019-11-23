/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da.impl

import net.mm2d.upnp.da.DaArgument
import net.mm2d.upnp.da.DaStateVariable

class DaArgumentImpl(
    override val name: String,
    override val isInputDirection: Boolean,
    override val relatedStateVariable: DaStateVariable
) : DaArgument {

    class Builder {
        private var name: String? = null
        private var inputDirection: Boolean = false
        private var relatedStateVariableName: String? = null
        private var relatedStateVariable: DaStateVariable? = null

        @Throws(IllegalStateException::class)
        fun build(): DaArgument {
            val name = name
                ?: throw IllegalStateException("name must be set.")
            val relatedStateVariable = relatedStateVariable
                ?: throw IllegalStateException("related state variable must be set.")
            return DaArgumentImpl(
                name = name,
                isInputDirection = inputDirection,
                relatedStateVariable = relatedStateVariable
            )
        }

        fun setName(name: String): Builder = apply {
            this.name = name
        }

        fun setDirection(direction: String): Builder = apply {
            inputDirection = "in".equals(direction, ignoreCase = true)
        }

        fun setRelatedStateVariableName(name: String): Builder = apply {
            relatedStateVariableName = name
        }

        fun getRelatedStateVariableName(): String? {
            return relatedStateVariableName
        }

        fun setRelatedStateVariable(variable: DaStateVariable): Builder = apply {
            relatedStateVariable = variable
        }
    }
}
