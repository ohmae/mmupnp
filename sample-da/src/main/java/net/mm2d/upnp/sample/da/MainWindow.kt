/*
 * Copyright (c) 2020 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.sample.da

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mm2d.log.DefaultSender
import net.mm2d.log.Logger
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JFrame
import javax.swing.UIManager
import javax.swing.WindowConstants

class MainWindow private constructor() : JFrame() {
    init {
        title = "UPnP"
        setSize(800, 800)
        setLocationRelativeTo(null)
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        isVisible = true
    }
    companion object {
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
