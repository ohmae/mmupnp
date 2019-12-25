/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.property

import net.mm2d.log.Logger

/**
 * Property of UPnP Argument.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class ArgumentProperty(
    /**
     * Return the Argument name
     *
     * @return Argument name
     */
    val name: String,

    /**
     * Return direction is input or not.
     *
     * @return true: input, false: output
     */
    val isInputDirection: Boolean,

    /**
     * Return an instance of StateVariable that is specified in RelatedStateVariable.
     *
     * @return instance of StateVariable
     */
    val relatedStateVariable: StateVariableProperty
) {
    class Builder {
        var name: String? = null
        var isInputDirection: Boolean = false
        var relatedStateVariableName: String? = null
        fun build(stateVariableList: List<StateVariableProperty>): ArgumentProperty {
            val name = checkNotNull(name) {
                "name must be set."
            }
            val relatedStateVariableName = checkNotNull(relatedStateVariableName) {
                "relatedStateVariableName must be set."
            }
            val relatedStateVariable = stateVariableList.find { it.name == relatedStateVariableName }
                ?: repairInvalidFormatAndGet(relatedStateVariableName, stateVariableList)
                ?: error("$relatedStateVariableName not found in serviceStateTable.")
            return ArgumentProperty(
                name = name,
                isInputDirection = isInputDirection,
                relatedStateVariable = relatedStateVariable
            )
        }

        // Implement the remedies because there is a device that has the wrong format of XML
        // That indented in the text content.
        // e.g. AN-WLTU1
        @Throws(IllegalStateException::class)
        private fun repairInvalidFormatAndGet(
            name: String,
            stateVariableList: List<StateVariableProperty>
        ): StateVariableProperty {
            val trimmedName = name.trim()
            val trimmedVariable = stateVariableList.find { it.name == trimmedName }
                ?: error("There is no StateVariable [$name]")
            relatedStateVariableName = trimmedName
            Logger.i { "Invalid description. relatedStateVariable name has unnecessary blanks [$name]" }
            return trimmedVariable
        }
    }
}
