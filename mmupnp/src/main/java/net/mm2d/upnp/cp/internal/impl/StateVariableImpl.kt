/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.impl

import net.mm2d.upnp.common.internal.property.StateVariableProperty
import net.mm2d.upnp.cp.StateVariable
import net.mm2d.upnp.cp.StateVariable.AllowedValueRange

/**
 * Implements for [StateVariable].
 *
 * @author [大前良介(OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class StateVariableImpl(
    property: StateVariableProperty
) : StateVariable {
    override val isSendEvents: Boolean = property.isSendEvents
    override val isMulticast: Boolean = property.isMulticast
    override val name: String = property.name
    override val dataType: String = property.dataType
    override val defaultValue: String? = property.defaultValue
    override val allowedValueList: List<String> = property.allowedValueList
    override val allowedValueRange: AllowedValueRange? = property.allowedValueRange?.let {
        AllowedValueRange(
            minimum = it.minimum,
            maximum = it.maximum,
            step = it.step
        )
    }
}
