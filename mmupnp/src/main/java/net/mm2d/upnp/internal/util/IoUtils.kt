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
 * IO関係のユーティリティメソッドを提供する。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */

/**
 * Nullチェック、Exceptionキャッチ付きでclose処理を行う。
 *
 * nullの場合は何も行わない、
 * closeで[IOException]が発生した場合はログ出力をする。
 *
 * @receiver close処理をする[Closeable]
 */
internal fun Closeable?.closeQuietly() {
    try {
        this?.close()
    } catch (ignored: IOException) {
    }
}

/**
 * Nullチェック、Exceptionキャッチ付きでclose処理を行う。
 *
 * nullの場合は何も行わない、
 * closeで[IOException]が発生した場合はログ出力をする。
 *
 * @receiver close処理をする[Socket]
 */
internal fun Socket?.closeQuietly() {
    try {
        this?.close()
    } catch (ignored: IOException) {
    }
}

/**
 * Nullチェック、Exceptionキャッチ付きでclose処理を行う。
 *
 * nullの場合は何も行わない、
 * closeで[IOException]が発生した場合はログ出力をする。
 *
 * @receiver close処理をする[DatagramSocket]
 */
internal fun DatagramSocket?.closeQuietly() {
    this?.close()
}

/**
 * Nullチェック、Exceptionキャッチ付きでclose処理を行う。
 *
 * nullの場合は何も行わない、
 * closeで[IOException]が発生した場合はログ出力をする。
 *
 * @receiver close処理をする[ServerSocket]
 */
internal fun ServerSocket?.closeQuietly() {
    try {
        this?.close()
    } catch (ignored: IOException) {
    }
}
