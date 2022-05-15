/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty

import net.mm2d.upnp.Action
import net.mm2d.upnp.Argument
import net.mm2d.upnp.ControlPoints
import net.mm2d.upnp.Service
import java.io.IOException

/**
 * Empty implementation of [Action].
 */
object EmptyAction : Action {
    override val service: Service = ControlPoints.emptyService()
    override val name: String = ""
    override val argumentList: List<Argument> = emptyList()
    override fun findArgument(name: String): Argument? = null

    override suspend fun invoke(
        argumentValues: Map<String, String?>,
        returnErrorResponse: Boolean
    ): Map<String, String> {
        throw IOException("empty object")
    }

    override suspend fun invokeCustom(
        argumentValues: Map<String, String?>,
        customNamespace: Map<String, String>,
        customArguments: Map<String, String>,
        returnErrorResponse: Boolean
    ): Map<String, String> {
        throw IOException("empty object")
    }
}
