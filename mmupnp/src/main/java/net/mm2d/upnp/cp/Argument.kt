/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp

/**
 * Interface of UPnP Argument.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface Argument {
    /**
     * Return the Argument name
     *
     * @return Argument name
     */
    val name: String

    /**
     * Return direction is input or not.
     *
     * @return true: input, false: output
     */
    val isInputDirection: Boolean

    /**
     * Return an instance of StateVariable that is specified in RelatedStateVariable.
     *
     * @return instance of StateVariable
     */
    val relatedStateVariable: StateVariable
}
