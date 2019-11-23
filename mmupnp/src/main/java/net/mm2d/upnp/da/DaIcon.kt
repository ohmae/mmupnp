/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da

interface DaIcon {
    val mimeType: String
    val height: Int
    val width: Int
    val depth: Int
    val url: String
    val binary: ByteArray?
}
