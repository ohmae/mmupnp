/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample.cp

import net.mm2d.upnp.cp.StateVariable

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class StateVariableNode(variable: StateVariable) : UpnpNode(variable) {
    init {
        setAllowsChildren(false)
    }

    override fun getUserObject(): StateVariable {
        return super.getUserObject() as StateVariable
    }

    override fun formatDescription(): String {
        return Formatter.format(getUserObject())
    }

    override fun toString(): String {
        return getUserObject().name
    }
}
