/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.log

/**
 * Logger interface.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object Logger {
    /**
     * Log level VERBOSE
     */
    const val VERBOSE = 2
    /**
     * Log level DEBUG
     */
    const val DEBUG = 3
    /**
     * Log level INFO
     */
    const val INFO = 4
    /**
     * Log level WARN
     */
    const val WARN = 5
    /**
     * Log level ERROR
     */
    const val ERROR = 6
    /**
     * Log level ASSERT
     */
    const val ASSERT = 7

    private var logLevel = Integer.MAX_VALUE

    private var logSender: Sender? = null

    /**
     * Set log Sender.
     *
     * @param sender Sender, If it is null, nothing is done
     * @see Sender
     */
    @JvmStatic
    fun setSender(sender: Sender?) {
        logSender = sender
    }

    /**
     * Set the log level.
     *
     * Output a log that is equal to or larger than the set value.
     * Log levels are defined in ascending order
     * [VERBOSE], [DEBUG], [INFO], [WARN], [ERROR], [ASSERT],
     * If set [ERROR], output log of [ERROR] and [ASSERT] level.
     *
     * Default value is [Integer.MAX_VALUE], This means that nothing to output.
     *
     * @param level log level
     * @see .VERBOSE
     * @see .DEBUG
     * @see .INFO
     * @see .WARN
     * @see .ERROR
     * @see .ASSERT
     */
    @JvmStatic
    fun setLogLevel(level: Int) {
        logLevel = level
    }

    private fun send(level: Int, throwable: Throwable?, message: String?, vararg args: Any?) {
        if (level < logLevel) {
            return
        }
        val msg: String = when {
            message == null -> "null"
            args.isEmpty() -> message
            else -> message.format(*args)
        }
        logSender?.invoke(level, msg, throwable)
    }

    private fun send(level: Int, throwable: Throwable?, supplier: () -> String?) {
        if (level < logLevel) {
            return
        }
        logSender?.invoke(level, supplier() ?: "null", throwable)
    }

    /**
     * Send log at [VERBOSE] level
     *
     * @param message log message
     */
    @JvmStatic
    fun v(message: String?, vararg args: Any?) {
        send(VERBOSE, null, message, *args)
    }

    /**
     * Send log at [VERBOSE] level
     *
     * @param supplier log message supplier
     */
    @JvmStatic
    fun v(supplier: () -> String?) {
        send(VERBOSE, null, supplier)
    }

    /**
     * Send log at [VERBOSE] level
     *
     * @param throwable Throwable
     */
    @JvmStatic
    fun v(throwable: Throwable?) {
        send(VERBOSE, throwable, "")
    }

    /**
     * Send log at [VERBOSE] level
     *
     * @param throwable Throwable
     * @param message   log message
     */
    @JvmStatic
    fun v(throwable: Throwable?, message: String?, vararg args: Any?) {
        send(VERBOSE, throwable, message, *args)
    }

    /**
     * Send log at [VERBOSE] level
     *
     * @param throwable Throwable
     * @param supplier  log message supplier
     */
    @JvmStatic
    fun v(throwable: Throwable?, supplier: () -> String?) {
        send(VERBOSE, throwable, supplier)
    }

    /**
     * Send log at [DEBUG] level
     *
     * @param message log message
     */
    @JvmStatic
    fun d(message: String?, vararg args: Any?) {
        send(DEBUG, null, message, *args)
    }

    /**
     * Send log at [DEBUG] level
     *
     * @param supplier log message supplier
     */
    @JvmStatic
    fun d(supplier: () -> String?) {
        send(DEBUG, null, supplier)
    }

    /**
     * Send log at [DEBUG] level
     *
     * @param throwable Throwable
     */
    @JvmStatic
    fun d(throwable: Throwable?) {
        send(DEBUG, throwable, "")
    }

    /**
     * Send log at [DEBUG] level
     *
     * @param throwable Throwable
     * @param message   log message
     */
    @JvmStatic
    fun d(throwable: Throwable?, message: String?, vararg args: Any?) {
        send(DEBUG, throwable, message, *args)
    }

    /**
     * Send log at [DEBUG] level
     *
     * @param throwable Throwable
     * @param supplier  log message supplier
     */
    @JvmStatic
    fun d(throwable: Throwable?, supplier: () -> String?) {
        send(DEBUG, throwable, supplier)
    }

    /**
     * Send log at [INFO] level
     *
     * @param message log message
     */
    @JvmStatic
    fun i(message: String?, vararg args: Any?) {
        send(INFO, null, message, *args)
    }

    /**
     * Send log at [INFO] level
     *
     * @param supplier log message supplier
     */
    @JvmStatic
    fun i(supplier: () -> String?) {
        send(INFO, null, supplier)
    }

    /**
     * Send log at [INFO] level
     *
     * @param throwable Throwable
     */
    @JvmStatic
    fun i(throwable: Throwable?) {
        send(INFO, throwable, "")
    }

    /**
     * Send log at [INFO] level
     *
     * @param throwable Throwable
     * @param message   log message
     */
    @JvmStatic
    fun i(throwable: Throwable?, message: String?, vararg args: Any?) {
        send(INFO, throwable, message, *args)
    }

    /**
     * Send log at [INFO] level
     *
     * @param throwable Throwable
     * @param supplier  log message supplier
     */
    @JvmStatic
    fun i(throwable: Throwable?, supplier: () -> String?) {
        send(INFO, throwable, supplier)
    }

    /**
     * Send log at [WARN] level
     *
     * @param message log message
     */
    @JvmStatic
    fun w(message: String?, vararg args: Any?) {
        send(WARN, null, message, *args)
    }

    /**
     * Send log at [WARN] level
     *
     * @param supplier log message supplier
     */
    @JvmStatic
    fun w(supplier: () -> String?) {
        send(WARN, null, supplier)
    }

    /**
     * Send log at [WARN] level
     *
     * @param throwable Throwable
     */
    @JvmStatic
    fun w(throwable: Throwable?) {
        send(WARN, throwable, "")
    }

    /**
     * Send log at [WARN] level
     *
     * @param throwable Throwable
     * @param message   log message
     */
    @JvmStatic
    fun w(throwable: Throwable?, message: String?, vararg args: Any?) {
        send(WARN, throwable, message, *args)
    }

    /**
     * Send log at [WARN] level
     *
     * @param throwable Throwable
     * @param supplier  log message supplier
     */
    @JvmStatic
    fun w(throwable: Throwable?, supplier: () -> String?) {
        send(WARN, throwable, supplier)
    }

    /**
     * Send log at [ERROR] level
     *
     * @param message log message
     */
    @JvmStatic
    fun e(message: String?, vararg args: Any?) {
        send(ERROR, null, message, *args)
    }

    /**
     * Send log at [ERROR] level
     *
     * @param supplier log message supplier
     */
    @JvmStatic
    fun e(supplier: () -> String?) {
        send(ERROR, null, supplier)
    }

    /**
     * Send log at [ERROR] level
     *
     * @param throwable Throwable
     */
    @JvmStatic
    fun e(throwable: Throwable?) {
        send(ERROR, throwable, "")
    }

    /**
     * Send log at [ERROR] level
     *
     * @param message   log message
     * @param throwable Throwable
     */
    @JvmStatic
    fun e(throwable: Throwable?, message: String?, vararg args: Any?) {
        send(ERROR, throwable, message, *args)
    }

    /**
     * Send log at [ERROR] level
     *
     * @param supplier  log message supplier
     * @param throwable Throwable
     */
    @JvmStatic
    fun e(throwable: Throwable?, supplier: () -> String?) {
        send(ERROR, throwable, supplier)
    }
}
