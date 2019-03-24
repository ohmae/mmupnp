/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample

import net.mm2d.upnp.Http
import net.mm2d.upnp.Service
import java.awt.Component
import java.awt.Desktop
import java.io.IOException
import java.net.URISyntaxException
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class ServiceNode(service: Service) : UpnpNode(service) {
    var isSubscribing: Boolean = false
        private set

    override fun getDetailXml(): String = formatXml(getUserObject().description)

    init {
        val actions = service.actionList
        for (action in actions) {
            add(ActionNode(action))
        }
        add(StateVariableListNode(service.stateVariableList))
    }

    override fun getUserObject(): Service {
        return super.getUserObject() as Service
    }

    override fun formatDescription(): String {
        return Formatter.format(getUserObject())
    }

    override fun toString(): String {
        return getUserObject().serviceType
    }

    override fun showContextMenu(frame: JFrame, invoker: Component, x: Int, y: Int) {
        val menu = JPopupMenu()
        menu.add(JMenuItem("Open Service Description").also {
            it.addActionListener { openServerDescription() }
        })
        if (isSubscribing) {
            menu.add(JMenuItem("Unsubscribe").also {
                it.addActionListener { unsubscribe() }
            })
        } else {
            menu.add(JMenuItem("Subscribe").also {
                it.addActionListener { subscribe() }
            })
        }
        menu.show(invoker, x, y)
    }

    private fun openServerDescription() {
        val service = getUserObject()
        val device = service.device
        try {
            val uri = Http.makeAbsoluteUrl(device.baseUrl, service.scpdUrl, device.scopeId).toURI()
            Desktop.getDesktop().browse(uri)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    private fun subscribe() {
        getUserObject().subscribe(true) {
            isSubscribing = it
        }
    }

    private fun unsubscribe() {
        getUserObject().unsubscribe {
            isSubscribing = !it
        }
    }
}
