/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da.internal.impl

import net.mm2d.upnp.common.internal.property.StateVariableProperty
import net.mm2d.upnp.common.util.append
import net.mm2d.upnp.common.util.appendIfNotNull
import net.mm2d.upnp.da.DaStateVariable
import net.mm2d.upnp.da.DaStateVariable.AllowedValueRange
import org.w3c.dom.Element

class DaStateVariableImpl(
    property: StateVariableProperty
) : DaStateVariable, XmlAppendable {
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

    override fun appendTo(parent: Element) {
        parent.append("stateVariable") {
            setAttribute("sendEvents", if (isSendEvents) "yes" else "no")
            if (isMulticast) {
                setAttribute("multicast", "yes")
            }
            append("name", name)
            append("dataType", dataType)
            appendIfNotNull("defaultValue", defaultValue)
            append(allowedValueList, "allowedValueList", "allowedValue")
            allowedValueRange?.appendTo(this)
        }
    }

    private fun AllowedValueRange.appendTo(parent: Element) {
        parent.append("allowedValueRange") {
            append("minimum", minimum)
            append("maximum", maximum)
            step?.let { append("step", it) }
        }
    }

    companion object {
        fun create(property: StateVariableProperty): DaStateVariableImpl =
            DaStateVariableImpl(property)
    }
}
