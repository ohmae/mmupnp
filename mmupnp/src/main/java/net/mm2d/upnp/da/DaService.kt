/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da

interface DaService {
    val device: DaDevice
    val serviceType: String
    val serviceId: String
    val scpdUrl: String
    val controlUrl: String
    val eventSubUrl: String
    val description: String
    val actionList: List<DaAction>
    val stateVariableList: List<DaStateVariable>
    fun findAction(name: String): DaAction?
    fun findStateVariable(name: String?): DaStateVariable?
}
