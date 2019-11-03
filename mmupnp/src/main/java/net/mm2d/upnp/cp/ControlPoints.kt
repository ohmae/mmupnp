/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp

import net.mm2d.upnp.common.SsdpMessage
import net.mm2d.upnp.cp.empty.*

/**
 * Provide EmptyObject.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object ControlPoints {
    /**
     * Returns the ControlPoint's Empty Object.
     *
     * @return ControlPoint's Empty Object
     */
    fun emptyControlPoint(): ControlPoint = EmptyControlPoint

    /**
     * Returns the Device's Empty Object.
     *
     * @return Device's Empty Object
     */
    fun emptyDevice(): Device = EmptyDevice

    /**
     * Returns the Service's Empty Object.
     *
     * @return Service's Empty Object
     */
    fun emptyService(): Service = EmptyService

    /**
     * Returns the Action's Empty Object.
     *
     * @return Action's Empty Object
     */
    fun emptyAction(): Action = EmptyAction

    /**
     * Returns the SsdpMessage's Empty Object.
     *
     * @return SsdpMessage's Empty Object
     */
    fun emptySsdpMessage(): SsdpMessage = EmptySsdpMessage
}
