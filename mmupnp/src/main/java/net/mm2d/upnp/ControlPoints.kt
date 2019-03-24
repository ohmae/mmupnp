/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import net.mm2d.upnp.empty.*

/**
 * EmptyObjectを提供する。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object ControlPoints {
    /**
     * ControlPointのEmpty実装を返す。
     *
     * @return ControlPointのEmpty実装
     */
    fun emptyControlPoint(): ControlPoint = EmptyControlPoint

    /**
     * DeviceのEmpty実装を返す。
     *
     * @return DeviceのEmpty実装
     */
    fun emptyDevice(): Device = EmptyDevice

    /**
     * ServiceのEmpty実装を返す。
     *
     * @return ServiceのEmpty実装
     */
    fun emptyService(): Service = EmptyService

    /**
     * ActionのEmpty実装を返す。
     *
     * @return ActionのEmpty実装
     */
    fun emptyAction(): Action = EmptyAction

    /**
     * SsdpMessageのEmpty実装を返す。
     *
     * @return SsdpMessageのEmpty実装
     */
    fun emptySsdpMessage(): SsdpMessage = EmptySsdpMessage
}
