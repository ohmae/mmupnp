/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.impl

import net.mm2d.upnp.common.internal.property.ArgumentProperty
import net.mm2d.upnp.cp.Argument

/**
 * Implements for [Argument].
 *
 * @author [大前良介(OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class ArgumentImpl(
    property: ArgumentProperty,
    override val relatedStateVariable: StateVariableImpl
) : Argument {
    override val name: String = property.name
    override val isInputDirection: Boolean = property.isInputDirection

    companion object {
        fun create(property: ArgumentProperty, stateVariableList: List<StateVariableImpl>): ArgumentImpl {
            val relatedStateVariable = stateVariableList.find {
                it.name == property.relatedStateVariable.name
            } ?: error("impossible")
            return ArgumentImpl(property, relatedStateVariable)
        }
    }
}
