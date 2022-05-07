/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample

import net.mm2d.xml.parser.XmlParser
import java.awt.Component
import javax.swing.JFrame
import javax.swing.tree.DefaultMutableTreeNode

open class UpnpNode(
    userObject: Any,
    allowsChildren: Boolean = true
) : DefaultMutableTreeNode(userObject, allowsChildren) {
    open fun getDetailXml(): String {
        return ""
    }

    open fun formatDescription(): String {
        return ""
    }

    protected fun formatXml(xml: String): String =
        XmlParser.parse(xml)?.buildString(indent = true, withDeclaration = true) ?: ""

    open fun showContextMenu(frame: JFrame, invoker: Component, x: Int, y: Int) {
    }
}
