/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample.cp

import net.mm2d.upnp.cp.Action
import java.awt.Component
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class ActionNode(action: Action) : UpnpNode(action) {
    init {
        action.argumentList.forEach {
            add(ArgumentNode(it))
        }
    }

    override fun getUserObject(): Action {
        return super.getUserObject() as Action
    }

    override fun formatDescription(): String {
        return Formatter.format(getUserObject())
    }

    override fun toString(): String {
        return getUserObject().name
    }

    override fun showContextMenu(frame: JFrame, invoker: Component, x: Int, y: Int) {
        JPopupMenu().also {
            it.add(JMenuItem("Invoke Action").also { item ->
                item.addActionListener { ActionWindow(getUserObject()).show(frame.x + x, frame.y + y) }
            })
        }.show(invoker, x, y)
    }
}
