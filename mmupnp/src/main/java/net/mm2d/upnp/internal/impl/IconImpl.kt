/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.upnp.Http
import net.mm2d.upnp.HttpClient
import net.mm2d.upnp.Icon
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
    fun loadBinary(client: HttpClient, baseUrl: String, scopeId: Int) {
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
        fun build(): Icon {
            val mimeType = mimeType
                ?: throw IllegalStateException("mimetype must be set.")
            if (width <= 0)
                throw IllegalStateException("width must be > 0. actually $width")
            if (height <= 0)
                throw IllegalStateException("height must be > 0. actually $height")
            if (depth <= 0)
                throw IllegalStateException("depth must be > 0. actually $depth")
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

        fun setMimeType(mimeType: String): Builder {
            this.mimeType = mimeType
            return this
        }

        fun setHeight(height: String): Builder {
            this.height = height.toIntOrNull() ?: 0
            return this
        }

        fun setWidth(width: String): Builder {
            this.width = width.toIntOrNull() ?: 0
            return this
        }

        fun setDepth(depth: String): Builder {
            this.depth = depth.toIntOrNull() ?: 0
            return this
        }

        fun setUrl(url: String): Builder {
            this.url = url
            return this
        }
    }
}
