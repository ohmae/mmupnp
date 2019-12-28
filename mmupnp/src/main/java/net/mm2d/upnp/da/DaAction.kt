/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da

/**
 * Interface of UPnP Action.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface DaAction {

    /**
     * Return the Service that is the owner of this Action.
     *
     * @return Service
     */
    val service: DaService

    /**
     * Return the Action name.
     *
     * @return Action name
     */
    val name: String

    /**
     * Return the Argument list.
     *
     * List is the immutable.
     * When the modification method is called, UnsupportedOperationException will be thrown.
     *
     * @return Argument list
     */
    val argumentList: List<DaArgument>

    /**
     * Find the Argument by name
     *
     * @param name Argument name
     * @return Argument
     */
    fun findArgument(name: String): DaArgument?

    companion object {
        /**
         * The key used to store the error response of `faultcode`.
         *
         * If it is a normal error response, "Client" with a namespace of SOAP is stored.
         */
        const val FAULT_CODE_KEY = "faultcode"
        /**
         * The key used to store the error response of `faultstring`.
         *
         * If it is a normal error response, "UPnPError" is stored.
         */
        const val FAULT_STRING_KEY = "faultstring"
        /**
         * The key used to store the error response of `detail/UPnPError/errorCode`.
         */
        const val ERROR_CODE_KEY = "UPnPError/errorCode"
        /**
         * The key used to store the error response of `detail/UPnPError/errorDescription`.
         */
        const val ERROR_DESCRIPTION_KEY = "UPnPError/errorDescription"
    }
}
