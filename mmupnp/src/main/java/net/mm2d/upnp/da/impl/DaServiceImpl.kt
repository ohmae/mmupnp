/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da.impl

import net.mm2d.upnp.da.DaAction
import net.mm2d.upnp.da.DaDevice
import net.mm2d.upnp.da.DaService
import net.mm2d.upnp.da.DaStateVariable

class DaServiceImpl(
    override val device: DaDevice,
    override val serviceType: String,
    override val serviceId: String,
    override val scpdUrl: String,
    override val controlUrl: String,
    override val eventSubUrl: String,
    override val description: String,
    override val actionList: List<DaAction>,
    override val stateVariableList: List<DaStateVariable>
) : DaService {
    override fun findAction(name: String): DaAction? = null
    override fun findStateVariable(name: String?): DaStateVariable? = null

    class Builder {
        private var device: DaDeviceImpl? = null
        private var serviceType: String? = null
        private var serviceId: String? = null
        private var scpdUrl: String? = null
        private var controlUrl: String? = null
        private var eventSubUrl: String? = null
        private var description: String? = null
        private val actionBuilderList = mutableListOf<DaActionImpl.Builder>()
        private val stateVariables = mutableListOf<DaStateVariable>()

        @Throws(IllegalStateException::class)
        fun build(): DaServiceImpl {
            val device = device
                ?: throw IllegalStateException("device must be set.")
            val serviceType = serviceType
                ?: throw IllegalStateException("serviceType must be set.")
            val serviceId = serviceId
                ?: throw IllegalStateException("serviceId must be set.")
            val scpdUrl = scpdUrl
                ?: throw IllegalStateException("SCPDURL must be set.")
            val controlUrl = controlUrl
                ?: throw IllegalStateException("controlURL must be set.")
            val eventSubUrl = eventSubUrl
                ?: throw IllegalStateException("eventSubURL must be set.")
            val description = description ?: ""
            return DaServiceImpl(
                device = device,
                serviceType = serviceType,
                serviceId = serviceId,
                scpdUrl = scpdUrl,
                controlUrl = controlUrl,
                eventSubUrl = eventSubUrl,
                description = description,
                actionList = actionBuilderList.map { it.build() },
                stateVariableList = stateVariables
            )
        }

        fun setDevice(device: DaDeviceImpl): Builder = apply {
            this.device = device
        }

        fun setServiceType(serviceType: String): Builder = apply {
            this.serviceType = serviceType
        }

        fun setServiceId(serviceId: String): Builder = apply {
            this.serviceId = serviceId
        }

        fun setScpdUrl(scpdUrl: String): Builder = apply {
            this.scpdUrl = scpdUrl
        }

        fun getScpdUrl(): String? = scpdUrl

        fun setControlUrl(controlUrl: String): Builder = apply {
            this.controlUrl = controlUrl
        }

        fun setEventSubUrl(eventSubUrl: String): Builder = apply {
            this.eventSubUrl = eventSubUrl
        }

        fun setDescription(description: String): Builder = apply {
            this.description = description
        }

        fun addActionBuilder(builder: DaActionImpl.Builder): Builder = apply {
            actionBuilderList.add(builder)
        }

        fun addStateVariable(builder: DaStateVariable): Builder = apply {
            stateVariables.add(builder)
        }

        fun toDumpString(): String = "ServiceBuilder:\n" +
            "serviceType:$serviceType\n" +
            "serviceId:$serviceId\n" +
            "SCPDURL:$scpdUrl\n" +
            "eventSubURL:$eventSubUrl\n" +
            "controlURL:$controlUrl\n" +
            "DESCRIPTION:$description"
    }
}
