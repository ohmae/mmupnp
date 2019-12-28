/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da.internal.impl

import net.mm2d.upnp.common.internal.property.StateVariableProperty
import net.mm2d.upnp.common.util.appendNewElement
import net.mm2d.upnp.da.DaStateVariable
import net.mm2d.upnp.da.DaStateVariable.AllowedValueRange
import org.w3c.dom.Element

class DaStateVariableImpl(
    property: StateVariableProperty
) : DaStateVariable, DescriptionAppendable {
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

    override fun appendDescriptionTo(parent: Element) {
        parent.appendNewElement("stateVariable").apply {
            setAttribute("sendEvents", if (isSendEvents) "yes" else "no")
            if (isMulticast) {
                setAttribute("multicast", "yes")
            }
            appendNewElement("name", name)
            appendNewElement("dataType", dataType)
            if (defaultValue != null) {
                appendNewElement("defaultValue", defaultValue)
            }
            if (allowedValueList.isNotEmpty()) {
                appendNewElement("allowedValueList").apply {
                    allowedValueList.forEach {
                        appendNewElement("allowedValue", it)
                    }
                }
            }
            allowedValueRange?.let { allowedValueRange ->
                appendNewElement("allowedValueRange").apply {
                    appendNewElement("minimum", allowedValueRange.minimum)
                    appendNewElement("maximum", allowedValueRange.maximum)
                    allowedValueRange.step?.let { step ->
                        appendNewElement("step", step)
                    }
                }
            }
        }
    }

    companion object {
        fun create(property: StateVariableProperty): DaStateVariableImpl =
            DaStateVariableImpl(property)
    }
}
