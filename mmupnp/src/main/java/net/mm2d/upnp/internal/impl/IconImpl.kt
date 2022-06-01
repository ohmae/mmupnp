/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.upnp.Http
import net.mm2d.upnp.Icon
import net.mm2d.upnp.SingleHttpClient
import java.io.IOException

/**
 * Implements for [Icon].
 *
 * @author [大前良介(OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class IconImpl(
    override val mimeType: String,
    override val height: Int,
    override val width: Int,
    override val depth: Int,
    override val url: String
) : Icon {
    override var binary: ByteArray? = null
        private set

    @Throws(IOException::class)
    fun loadBinary(client: SingleHttpClient, baseUrl: String, scopeId: Int) {
        val url = Http.makeAbsoluteUrl(baseUrl, url, scopeId)
        binary = client.downloadBinary(url)
    }

    internal class Builder {
        private var mimeType: String? = null
        private var height: Int = 0
        private var width: Int = 0
        private var depth: Int = 0
        private var url: String? = null

        @Throws(IllegalStateException::class)
        fun build(): IconImpl {
            val mimeType = mimeType
                ?: throw IllegalStateException("mimetype must be set.")
            check(width > 0) { "width must be > 0. actually $width" }
            check(height > 0) { "height must be > 0. actually $height" }
            check(depth > 0) { "depth must be > 0. actually $depth" }
            val url = url
                ?: throw IllegalStateException("url must be set.")

            return IconImpl(
                mimeType = mimeType,
                height = height,
                width = width,
                depth = depth,
                url = url
            )
        }

        fun setMimeType(mimeType: String): Builder = apply {
            this.mimeType = mimeType
        }

        fun setHeight(height: String): Builder = apply {
            this.height = height.toIntOrNull() ?: 0
        }

        fun setWidth(width: String): Builder = apply {
            this.width = width.toIntOrNull() ?: 0
        }

        fun setDepth(depth: String): Builder = apply {
            this.depth = depth.toIntOrNull() ?: 0
        }

        fun setUrl(url: String): Builder = apply {
            this.url = url
        }
    }
}
