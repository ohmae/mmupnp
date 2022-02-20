/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample

import net.mm2d.upnp.Argument

class ArgumentNode(argument: Argument) : UpnpNode(argument) {
    init {
        setAllowsChildren(false)
    }

    override fun getUserObject(): Argument = super.getUserObject() as Argument
    override fun formatDescription(): String = Formatter.format(getUserObject())
    override fun toString(): String = getUserObject().name
}
