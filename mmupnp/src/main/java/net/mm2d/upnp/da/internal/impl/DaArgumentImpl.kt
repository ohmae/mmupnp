/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da.internal.impl

import net.mm2d.upnp.common.internal.property.ArgumentProperty
import net.mm2d.upnp.common.util.append
import net.mm2d.upnp.da.DaArgument
import net.mm2d.upnp.da.DaStateVariable
import org.w3c.dom.Element

class DaArgumentImpl(
    property: ArgumentProperty,
    override val relatedStateVariable: DaStateVariable
) : DaArgument, XmlAppendable {
    override val name: String = property.name
    override val isInputDirection: Boolean = property.isInputDirection

    override fun appendTo(parent: Element) {
        parent.append("argument") {
            append("name", name)
            append("direction", if (isInputDirection) "in" else "out")
            append("relatedStateVariable", relatedStateVariable.name)
        }
    }

    companion object {
        fun create(property: ArgumentProperty, stateVariableList: List<DaStateVariableImpl>): DaArgumentImpl {
            val relatedStateVariable = stateVariableList.find {
                it.name == property.relatedStateVariable.name
            } ?: error("impossible")
            return DaArgumentImpl(property, relatedStateVariable)
        }
    }
}
