/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.util

import java.io.Closeable
import java.io.IOException
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket

/**
 * Provide IO related utility methods.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */

/**
 * Perform close processing with null check and exception catch.
 *
 * Does not do anything if it is null,
 * it outputs log when [IOException] occurs in close.
 *
 * @receiver close target of [Closeable]
 */
internal fun Closeable?.closeQuietly() {
    try {
        this?.close()
    } catch (ignored: IOException) {
    }
}

/**
 * Perform close processing with null check and exception catch.
 *
 * Does not do anything if it is null,
 * it outputs log when [IOException] occurs in close.
 *
 * @receiver close target of [Socket]
 */
internal fun Socket?.closeQuietly() {
    try {
        this?.close()
    } catch (ignored: IOException) {
    }
}

/**
 * Perform close processing with null check and exception catch.
 *
 * Does not do anything if it is null,
 * it outputs log when [IOException] occurs in close.
 *
 * @receiver close target of [DatagramSocket]
 */
internal fun DatagramSocket?.closeQuietly() {
    this?.close()
}

/**
 * Perform close processing with null check and exception catch.
 *
 * Does not do anything if it is null,
 * it outputs log when [IOException] occurs in close.
 *
 * @receiver close target of [ServerSocket]
 */
internal fun ServerSocket?.closeQuietly() {
    try {
        this?.close()
    } catch (ignored: IOException) {
    }
}
