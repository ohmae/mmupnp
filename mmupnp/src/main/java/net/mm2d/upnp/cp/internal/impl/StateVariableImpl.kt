/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.impl

import net.mm2d.upnp.common.internal.property.StateVariableProperty
import net.mm2d.upnp.cp.StateVariable

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
    override val allowedValueList: List<String> = property.allowedValueList
    override val defaultValue: String? = property.defaultValue
    override val minimum: String? = property.minimum
    override val maximum: String? = property.maximum
    override val step: String? = property.step
}
