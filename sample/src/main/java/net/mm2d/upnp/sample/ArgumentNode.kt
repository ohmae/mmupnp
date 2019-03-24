/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample

import net.mm2d.upnp.Argument

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class ArgumentNode(argument: Argument) : UpnpNode(argument) {
    init {
        setAllowsChildren(false)
    }

    override fun getUserObject(): Argument {
        return super.getUserObject() as Argument
    }

    override fun formatDescription(): String {
        return Formatter.format(getUserObject())
    }

    override fun toString(): String {
        return getUserObject().name
    }
}
