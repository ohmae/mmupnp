/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample

import net.mm2d.upnp.StateVariable

class StateVariableNode(variable: StateVariable) : UpnpNode(variable) {
    init {
        setAllowsChildren(false)
    }

    override fun getUserObject(): StateVariable = super.getUserObject() as StateVariable
    override fun formatDescription(): String = Formatter.format(getUserObject())
    override fun toString(): String = getUserObject().name
}
