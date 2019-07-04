/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mm2d.log.DefaultSender
import net.mm2d.log.Logger
import net.mm2d.upnp.Adapter.discoveryListener
import net.mm2d.upnp.Adapter.iconFilter
import net.mm2d.upnp.Adapter.notifyEventListener
import net.mm2d.upnp.ControlPoint
import net.mm2d.upnp.ControlPointFactory
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class MainWindow private constructor() : JFrame() {
    private val controlPoint: ControlPoint = ControlPointFactory.create().also {
        it.setIconFilter(iconFilter { list -> list })
        it.initialize()
    }
    private val rootNode: UpnpNode = UpnpNode("Device").also {
        it.allowsChildren = true
    }
    private val tree: JTree = JTree(rootNode, true).also {
        it.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        it.cellRenderer = MyTreeCellRenderer()
    }
    private val detail1: JTextArea = makeTextArea()
    private val detail2: JTextArea = makeTextArea()
    private val eventArea: JTextArea = makeTextArea()

    init {
        title = "UPnP"
        setSize(800, 800)
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        contentPane.add(makeControlPanel(), BorderLayout.NORTH)

        val detail = JSplitPane(JSplitPane.VERTICAL_SPLIT, JScrollPane(detail1), JScrollPane(detail2)).also {
            it.dividerLocation = 250
        }
        val main = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, JScrollPane(tree), detail).also {
            it.dividerLocation = 300
        }
        val content = JSplitPane(JSplitPane.VERTICAL_SPLIT, main, JScrollPane(eventArea)).also {
            it.dividerLocation = 600
        }
        contentPane.add(content, BorderLayout.CENTER)
        isVisible = true
        setUpControlPoint()
        setUpTree()
    }

    private fun setUpControlPoint() {
        controlPoint.addDiscoveryListener(discoveryListener({ update() }, { update() }))
        controlPoint.addNotifyEventListener(notifyEventListener { service, seq, variable, value ->
            eventArea.text = "${eventArea.text}${service.serviceType} : $seq : $variable : $value\n"
        })
    }

    private fun update() {
        rootNode.removeAllChildren()
        controlPoint.deviceList.forEach {
            rootNode.add(DeviceNode(it))
        }
        val model = tree.model as DefaultTreeModel
        model.reload()
    }

    private fun setUpTree() {
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (!SwingUtilities.isRightMouseButton(e)) {
                    return
                }
                val x = e.x
                val y = e.y
                val row = tree.getRowForLocation(x, y)
                if (row < 0) {
                    return
                }
                tree.setSelectionRow(row)
                val node = tree.lastSelectedPathComponent as? UpnpNode ?: return
                node.showContextMenu(this@MainWindow, tree, x, y)
            }
        })
        tree.addTreeSelectionListener {
            val node = tree.lastSelectedPathComponent as? UpnpNode ?: return@addTreeSelectionListener
            detail1.text = node.formatDescription()
            detail2.text = node.getDetailXml()
        }
    }

    private fun makeStartButton(): JButton = JButton("START").also {
        it.addActionListener {
            controlPoint.start()
            controlPoint.search()
        }
    }

    private fun makeStopButton(): JButton = JButton("STOP").also {
        it.addActionListener { controlPoint.stop() }
    }

    private fun makeClearButton(): JButton = JButton("CLEAR").also {
        it.addActionListener { controlPoint.clearDeviceList() }
    }

    private fun makeSearchButton(): JButton = JButton("M-SEARCH").also {
        it.addActionListener { controlPoint.search() }
    }

    private fun makeDumpButton(): JButton = JButton("DMS/DMR Dump").also {
        it.addActionListener { dump() }
    }

    private fun selectSaveDirectory(): File? {
        val chooser = JFileChooser().also {
            it.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            it.dialogTitle = "select directory"
        }
        val selected = chooser.showSaveDialog(this)
        return if (selected == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile
        } else null
    }

    private fun dump() {
        val dir = selectSaveDirectory() ?: return
        val json = controlPoint.deviceList
            .filter { it.deviceType.startsWith(DMS_PREFIX) || it.deviceType.startsWith(DMR_PREFIX) }
            .map { Server(it.location, it.friendlyName) }
            .let { Gson().toJson(it) }
        FileOutputStream(File(dir, "locations.json")).use {
            it.write(json.toByteArray())
        }
    }

    private data class Server(
        val location: String,
        val friendlyName: String
    )

    private fun makeControlPanel(): JPanel = JPanel().also {
        it.layout = FlowLayout()
        it.add(makeStartButton())
        it.add(makeStopButton())
        it.add(makeClearButton())
        it.add(makeSearchButton())
        it.add(makeDumpButton())
    }

    private fun makeTextArea(): JTextArea = JTextArea().also {
        it.tabSize = 2
        it.isEditable = false
        it.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
    }

    private class MyTreeCellRenderer internal constructor() : DefaultTreeCellRenderer() {
        private val deviceIcon: Icon = UIManager.getIcon("FileView.computerIcon")
        private val serviceIcon: Icon = UIManager.getIcon("FileView.directoryIcon")
        private val variableListIcon: Icon = UIManager.getIcon("FileView.hardDriveIcon")
        private val variableIcon: Icon = UIManager.getIcon("FileView.fileIcon")
        private val argumentIcon: Icon = UIManager.getIcon("FileView.fileIcon")
        private val actionIcon: Icon = UIManager.getIcon("FileView.floppyDriveIcon")

        override fun getTreeCellRendererComponent(
            tree: JTree,
            value: Any,
            sel: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ): Component {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
            when (value) {
                is DeviceNode -> icon = deviceIcon
                is ServiceNode -> {
                    icon = serviceIcon
                    if (value.isSubscribing) {
                        foreground = Color.BLUE
                    }
                }
                is StateVariableListNode -> icon = variableListIcon
                is StateVariableNode -> icon = variableIcon
                is ArgumentNode -> icon = argumentIcon
                is ActionNode -> icon = actionIcon
            }
            return this
        }
    }

    companion object {
        private const val DMS_PREFIX = "urn:schemas-upnp-org:device:MediaServer"
        private const val DMR_PREFIX = "urn:schemas-upnp-org:device:MediaRenderer"

        @JvmStatic
        fun main(args: Array<String>) {
            setUpLogger()
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: InstantiationException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: UnsupportedLookAndFeelException) {
                e.printStackTrace()
            }
            MainWindow()
        }

        private fun setUpLogger() {
            Logger.setLogLevel(Logger.VERBOSE)
            Logger.setSender(DefaultSender.create { level, tag, message ->
                GlobalScope.launch(Dispatchers.Main) {
                    val prefix = "$dateString ${level.toLogLevelString()} [$tag] "
                    message.split("\n").forEach { println(prefix + it) }
                }
            })
        }

        private val FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

        private val dateString: String
            get() = FORMAT.format(Date(System.currentTimeMillis()))

        private fun Int.toLogLevelString(): String = when (this) {
            Logger.VERBOSE -> "V"
            Logger.DEBUG -> "D"
            Logger.INFO -> "I"
            Logger.WARN -> "W"
            Logger.ERROR -> "E"
            else -> " "
        }
    }
}
