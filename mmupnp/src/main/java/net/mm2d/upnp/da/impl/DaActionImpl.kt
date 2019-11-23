/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da.impl

import net.mm2d.upnp.da.DaAction
import net.mm2d.upnp.da.DaArgument
import net.mm2d.upnp.da.DaService

class DaActionImpl(
    override val service: DaService,
    override val name: String,
    override val argumentList: List<DaArgument>
) : DaAction {
    override fun findArgument(name: String): DaArgument? = null

    class Builder {
        private var service: DaServiceImpl? = null
        private var name: String? = null
        private val argumentList: MutableList<DaArgumentImpl.Builder> = mutableListOf()

        @Throws(IllegalStateException::class)
        fun build(): DaActionImpl {
            val service = service
                ?: throw IllegalStateException("service must be set.")
            val name = name
                ?: throw IllegalStateException("name must be set.")
            return DaActionImpl(
                service = service,
                name = name,
                argumentList = argumentList
                    .map { it.build() }
            )
        }

        fun setService(service: DaServiceImpl): Builder = apply {
            this.service = service
        }

        fun setName(name: String): Builder = apply {
            this.name = name
        }

        fun addArgumentBuilder(argument: DaArgumentImpl.Builder): Builder = apply {
            argumentList.add(argument)
        }
    }
}
