/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da.internal.impl

import net.mm2d.upnp.common.internal.property.ServiceProperty
import net.mm2d.upnp.common.util.XmlUtils
import net.mm2d.upnp.common.util.append
import net.mm2d.upnp.common.util.toXml
import net.mm2d.upnp.da.DaAction
import net.mm2d.upnp.da.DaDevice
import net.mm2d.upnp.da.DaService
import net.mm2d.upnp.da.DaStateVariable
import org.w3c.dom.Element

class DaServiceImpl(
    property: ServiceProperty,
    override val actionList: List<DaActionImpl>,
    override val stateVariableList: List<DaStateVariableImpl>
) : DaService, XmlAppendable {
    init {
        check(stateVariableList.isNotEmpty()) { "service shall have >= 1 state variable" }
    }

    override lateinit var device: DaDevice
        internal set
    override val serviceType: String = property.serviceType
    override val serviceId: String = property.serviceId
    override val scpdUrl: String = property.scpdUrl
    override val controlUrl: String = property.controlUrl
    override val eventSubUrl: String = property.eventSubUrl
    override val description: String = property.description
    override fun findAction(name: String): DaAction? =
        actionList.find { it.name == name }

    override fun findStateVariable(name: String?): DaStateVariable? =
        stateVariableList.find { it.name == name }

    override fun appendTo(parent: Element) {
        parent.append("service") {
            append("serviceType", serviceType)
            append("serviceId", serviceId)
            append("SCPDURL", scpdUrl)
            append("controlURL", controlUrl)
            append("eventSubURL", eventSubUrl)
        }
    }

    fun createDescription(): String =
        XmlUtils.newDocument(false).also {
            it.append("scpd") {
                setAttribute("xmlns", "urn:schemas-upnp-org:service-1-0")
                append("specVersion") {
                    append("major", "1")
                    append("minor", "0")
                }
                append(actionList, "actionList")
                append(stateVariableList, "serviceStateTable")
            }
        }.toXml()

    companion object {
        fun create(
            property: ServiceProperty
        ): DaServiceImpl {
            val stateVariableList = property.stateVariableList
                .map { DaStateVariableImpl.create(it) }
            val actionList = property.actionList
                .map { DaActionImpl.create(it, stateVariableList) }
            return DaServiceImpl(property, actionList, stateVariableList)
        }
    }
}
