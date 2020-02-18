/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample.cp

import net.mm2d.upnp.cp.StateVariable

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class StateVariableListNode(variables: List<StateVariable>) : UpnpNode("StateVariable") {
    init {
        variables.forEach {
            add(StateVariableNode(it))
        }
    }
}
