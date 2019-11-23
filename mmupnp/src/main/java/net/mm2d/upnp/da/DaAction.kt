/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da

interface DaAction {
    val service: DaService
    val name: String
    val argumentList: List<DaArgument>
    fun findArgument(name: String): DaArgument?

    companion object {
        const val FAULT_CODE_KEY = "faultcode"
        const val FAULT_STRING_KEY = "faultstring"
        const val ERROR_CODE_KEY = "UPnPError/errorCode"
        const val ERROR_DESCRIPTION_KEY = "UPnPError/errorDescription"
    }
}
