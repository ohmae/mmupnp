/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.log

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Provide sender factory method and control method.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object Senders {
    /**
     * Set whether to append the caller's code position to the log.
     *
     * If set true, append the caller's code position to the log.
     * This will act as a link to the source code of caller on the IntelliJ console.
     *
     * Default value is false
     *
     * @param append if true, enabled.
     * @see DefaultSender
     */
    @JvmStatic
    fun appendCaller(append: Boolean) {
        DefaultSender.appendCaller(append)
    }

    /**
     * Set whether to append caller's thread information to the log.
     *
     * Default value is false
     *
     * @param append if true, enabled.
     * @see DefaultSender
     */
    @JvmStatic
    fun appendThread(append: Boolean) {
        DefaultSender.appendThread(append)
    }

    /**
     * Default [Sender] that using System.out.println
     *
     * @return Sender
     */
    @JvmStatic
    fun create(): Sender = DefaultSender.create { level, tag, message ->
        val prefix = "$dateString ${level.toLogLevelString()} [$tag] "
        message.split("\n").forEach {
            println(prefix + it)
        }
    }

    private val FORMAT = object : ThreadLocal<DateFormat>() {
        override fun initialValue(): DateFormat {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        }
    }

    private val dateString: String
        get() = FORMAT.get().format(Date(System.currentTimeMillis()))

    private fun Int.toLogLevelString(): String = when (this) {
        Logger.VERBOSE -> "V"
        Logger.DEBUG -> "D"
        Logger.INFO -> "I"
        Logger.WARN -> "W"
        Logger.ERROR -> "E"
        else -> " "
    }
}
