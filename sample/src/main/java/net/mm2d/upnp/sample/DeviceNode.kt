/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample

import net.mm2d.upnp.Device
import java.awt.Component
import java.awt.Desktop
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

class DeviceNode(device: Device) : UpnpNode(device) {
    init {
        device.serviceList.forEach { add(ServiceNode(it)) }
        device.deviceList.forEach { add(DeviceNode(it)) }
    }

    override fun getDetailXml(): String = formatXml(getUserObject().description)
    override fun getUserObject(): Device = super.getUserObject() as Device
    override fun toString(): String = getUserObject().friendlyName + " [" + getUserObject().ipAddress + "]"
    override fun formatDescription(): String = Formatter.format(getUserObject())

    override fun showContextMenu(frame: JFrame, invoker: Component, x: Int, y: Int) {
        val menu = JPopupMenu()
        JMenuItem("Open Device Description").also {
            it.addActionListener {
                try {
                    Desktop.getDesktop().browse(URI(getUserObject().location))
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }
            }
            menu.add(it)
        }
        menu.show(invoker, x, y)
    }
}
