/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample.cp

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mm2d.log.DefaultSender
import net.mm2d.log.Logger
import net.mm2d.upnp.common.Protocol
import net.mm2d.upnp.cp.Adapter.discoveryListener
import net.mm2d.upnp.cp.Adapter.eventListener
import net.mm2d.upnp.cp.Adapter.iconFilter
import net.mm2d.upnp.cp.ControlPoint
import net.mm2d.upnp.cp.ControlPointFactory
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.FlowLayout
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
    private val controlPoint: ControlPoint
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
        controlPoint = ControlPointFactory.create(protocol = Protocol.DUAL_STACK).also {
            it.setIconFilter(iconFilter { list -> list })
            it.initialize()
        }
        title = "UPnP"
        setSize(800, 800)
        setLocationRelativeTo(null)
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
        controlPoint.addEventListener(eventListener { service, seq, properties ->
            properties.forEach {
                eventArea.text = "${eventArea.text}${service.serviceType} : $seq : ${it.first} : ${it.second}\n"
            }
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

    private fun makeDumpButton(): JButton = JButton("Device Dump").also {
        it.addActionListener { dump() }
    }

    private fun makeLogLevelDialog(): JButton = JButton("Log").also {
        it.addActionListener {
            val dialog = JDialog()
            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
            for (i in Logger.VERBOSE..Logger.ERROR) {
                val label = when (i) {
                    Logger.VERBOSE -> "VERBOSE"
                    Logger.DEBUG -> "DEBUG"
                    Logger.INFO -> "INFO"
                    Logger.WARN -> "WARN"
                    Logger.ERROR -> "ERROR"
                    else -> ""
                }
                panel.add(JCheckBox(label).also { checkBox ->
                    checkBox.isSelected = enabledLogLevel[i]
                    checkBox.addChangeListener {
                        enabledLogLevel[i] = checkBox.isSelected
                    }
                })
            }
            dialog.add(panel)
            dialog.pack()
            dialog.setLocationRelativeTo(null)
            dialog.isModal = true
            dialog.isVisible = true
        }
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
        it.add(makeLogLevelDialog())
    }

    private fun makeTextArea(): JTextArea = JTextArea().also {
        it.tabSize = 2
        it.isEditable = false
    }

    private class MyTreeCellRenderer : DefaultTreeCellRenderer() {
        private val rootIcon: Icon
        private val deviceIcon: Icon
        private val serviceIcon: Icon
        private val variableListIcon: Icon
        private val variableIcon: Icon
        private val argumentIcon: Icon
        private val actionIcon: Icon

        init {
            val classLoader = MyTreeCellRenderer::class.java.classLoader
            rootIcon = ImageIcon(classLoader.getResource("root.png"))
            deviceIcon = ImageIcon(classLoader.getResource("device.png"))
            serviceIcon = ImageIcon(classLoader.getResource("service.png"))
            variableListIcon = ImageIcon(classLoader.getResource("folder.png"))
            variableIcon = ImageIcon(classLoader.getResource("variable.png"))
            argumentIcon = ImageIcon(classLoader.getResource("variable.png"))
            actionIcon = ImageIcon(classLoader.getResource("action.png"))
        }

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
                else -> icon = rootIcon
            }
            return this
        }
    }

    companion object {
        private const val DMS_PREFIX = "urn:schemas-upnp-org:device:MediaServer"
        private const val DMR_PREFIX = "urn:schemas-upnp-org:device:MediaRenderer"

        private val enabledLogLevel = Array(7) { true }
        private val FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

        @JvmStatic
        fun main(args: Array<String>) {
            setUpLogger()
            UIManager.getInstalledLookAndFeels()
                .find { it.className.contains("Nimbus") }
                ?.let { UIManager.setLookAndFeel(it.className) }
            MainWindow()
        }

        private fun setUpLogger() {
            Logger.setLogLevel(Logger.VERBOSE)
            Logger.setSender(DefaultSender.create { level, tag, message ->
                if (!enabledLogLevel[level]) return@create
                GlobalScope.launch(Dispatchers.Main) {
                    val prefix = "$dateString ${level.toLogLevelString()} [$tag] "
                    message.split("\n").dropLast(1).forEach { println(prefix + it) }
                }
            })
        }

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
