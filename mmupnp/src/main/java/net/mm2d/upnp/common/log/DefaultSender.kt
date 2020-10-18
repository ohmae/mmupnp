/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.log

/**
 * Default implementation of [Sender].
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object DefaultSender {
    private const val MAX_TAG_LENGTH = 23
    private var sAppendCaller: Boolean = false
    private var sAppendThread: Boolean = false

    /**
     * Set whether to append the caller's code position to the log.
     *
     * If set true, append the caller's code position to the log.
     * This will act as a link to the source code of caller on the IntelliJ console.
     *
     * Default value is false
     *
     * @param append if true, enabled.
     */
    @JvmStatic
    fun appendCaller(append: Boolean) {
        sAppendCaller = append
    }

    /**
     * Set whether to append caller's thread information to the log.
     *
     * Default value is false
     *
     * @param append if true, enabled.
     */
    @JvmStatic
    fun appendThread(append: Boolean) {
        sAppendThread = append
    }

    @JvmStatic
    fun create(printer: Printer): Sender = { level, message, throwable ->
        val trace = Throwable().stackTrace
        // $1 -> DefaultSender -> Logger#send -> Logger#v/d/i/w/e -> ログコール場所
        val element = if (trace.size > 4) trace[4] else null
        val tag = element?.makeTag() ?: "tag"
        val messages = mutableListOf<String>()
        if (sAppendThread) {
            messages.add(makeThreadInfo())
        }
        if (sAppendCaller && element != null) {
            messages.add("$element : ")
        }
        messages.add(makeMessage(message, throwable))
        printer.invoke(level, tag, messages.joinToString(separator = ""))
    }

    private fun StackTraceElement.makeTag(): String = className
        .substringAfterLast('.')
        .substringBefore('$')
        .let { if (it.length > MAX_TAG_LENGTH) it.substring(0, MAX_TAG_LENGTH) else it }
}
