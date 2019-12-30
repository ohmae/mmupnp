/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da.internal.impl

import net.mm2d.upnp.common.internal.property.ActionProperty
import net.mm2d.upnp.common.util.append
import net.mm2d.upnp.da.DaAction
import net.mm2d.upnp.da.DaArgument
import net.mm2d.upnp.da.DaService
import org.w3c.dom.Element

class DaActionImpl(
    property: ActionProperty,
    override val argumentList: List<DaArgumentImpl>
) : DaAction, DescriptionAppendable {
    override val name: String = property.name
    override lateinit var service: DaService
        internal set

    override fun findArgument(name: String): DaArgument? =
        argumentList.find { it.name == name }

    override fun appendDescriptionTo(parent: Element) {
        parent.append("action").apply {
            append("name", name)
            if (argumentList.isEmpty()) return@apply
            append("argumentList").apply {
                argumentList.forEach {
                    it.appendDescriptionTo(this)
                }
            }
        }
    }

    companion object {
        fun create(
            property: ActionProperty,
            stateVariableList: List<DaStateVariableImpl>
        ): DaActionImpl = DaActionImpl(
            property,
            property.argumentList.map { DaArgumentImpl.create(it, stateVariableList) }
        )
    }
}
