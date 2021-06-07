/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.log

import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

/**
 * Log sender interface.
 */
typealias Sender = (level: Int, message: String, throwable: Throwable?) -> Unit

/**
 * Delegate the log output method.
 */
typealias Printer = (level: Int, tag: String, message: String) -> Unit

/**
 * Make thread info string
 *
 * Utility method for Sender.
 *
 * @return Thread info string.
 */
fun makeThreadInfo(): String = Thread.currentThread().let { thread ->
    "[${thread.name},${thread.priority}" + (thread.threadGroup?.let { ",${it.name}] " } ?: "] ")
}

/**
 * Make message by join message and stack trace.
 *
 * Utility method for Sender.
 *
 * @param message Log message
 * @param throwable Throwable
 * @return message + stacktrace
 */
fun makeMessage(message: String, throwable: Throwable?): String = if (message.isEmpty()) {
    throwable?.toStackTraceString() ?: ""
} else {
    throwable?.let { "$message\n" + it.toStackTraceString() } ?: message
}

/**
 * Throwable to stack trace string.
 *
 * Utility method for Sender.
 *
 * @receiver throwable
 * @return Stacktrace string
 */
fun Throwable.toStackTraceString(): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw, false)
    printStackTrace(pw)
    pw.flush()
    return sw.toString()
}

private const val CAUSE_CAPTION = "Caused by: "

/**
 * Throwable to simple stack trace string.
 *
 * Utility method for Sender.
 *
 * @receiver throwable
 * @param classPrefix print target class prefix set
 * @return Stacktrace string
 */
fun Throwable.toSimpleStackTraceString(classPrefix: Set<String> = emptySet()): String {
    val dejaVu: MutableSet<Throwable> =
        Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    dejaVu.add(this)
    val sw = StringWriter()
    PrintWriter(sw, false).let {
        it.println(this)
        it.printSimpleStackTrace(classPrefix, stackTrace)
        it.printEnclosedSimpleStackTrace(classPrefix, cause, CAUSE_CAPTION, dejaVu)
    }
    return sw.toString()
}

private fun PrintWriter.printSimpleStackTrace(
    classPrefix: Set<String>,
    stackTrace: Array<StackTraceElement>
) {
    if (stackTrace.isEmpty()) return
    val index = stackTrace.indexOfFirst { element ->
        classPrefix.any { element.className.startsWith(it) }
    }.coerceAtLeast(0)
    if (index > 0) print("$index more ... ") else print("\t")
    print("at ${stackTrace[index]}")
    val remain = stackTrace.size - 1 - index
    if (remain > 0) println(" ... $remain more") else println()
}

private fun PrintWriter.printEnclosedSimpleStackTrace(
    classPrefix: Set<String>,
    throwable: Throwable?,
    caption: String,
    dejaVu: MutableSet<Throwable>
) {
    throwable ?: return
    if (dejaVu.contains(throwable)) {
        println("\t[CIRCULAR REFERENCE:$throwable]")
        return
    }
    dejaVu.add(throwable)
    println(caption + throwable)
    printSimpleStackTrace(classPrefix, throwable.stackTrace)
    printEnclosedSimpleStackTrace(classPrefix, throwable.cause, CAUSE_CAPTION, dejaVu)
}
