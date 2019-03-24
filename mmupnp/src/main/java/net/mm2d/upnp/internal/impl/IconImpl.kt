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
import net.mm2d.upnp.internal.parser.DeviceParser

import java.io.IOException

/**
 * [Icon]の実装。
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
    private var _binary: ByteArray? = null
    override val binary: ByteArray?
        get() = _binary

    /**
     * URLからバイナリデータを読み込む。
     *
     * @param client  通信に使用する[HttpClient]
     * @param baseUrl baseUrl
     * @param scopeId scopeId
     * @throws IOException 通信エラー
     */
    @Throws(IOException::class)
    fun loadBinary(client: HttpClient, baseUrl: String, scopeId: Int) {
        val url = Http.makeAbsoluteUrl(baseUrl, url, scopeId)
        _binary = client.downloadBinary(url)
    }

    /**
     * DeviceDescriptionのパース時に使用するビルダー
     *
     * @see DeviceParser.loadDescription
     */
    internal class Builder {
        private var mimeType: String? = null
        private var height: Int = 0
        private var width: Int = 0
        private var depth: Int = 0
        private var url: String? = null
        /**
         * [Icon]のインスタンスを作成する。
         *
         * @return [Icon]のインスタンス
         * @throws IllegalStateException 必須パラメータが設定されていない場合
         */
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

        /**
         * MimeTypeの値を登録する。
         *
         * @param mimeType MimeType
         */
        fun setMimeType(mimeType: String): Builder {
            this.mimeType = mimeType
            return this
        }

        /**
         * Heightの値を登録する
         *
         * @param height Height
         */
        fun setHeight(height: String): Builder {
            this.height = height.toIntOrNull() ?: 0
            return this
        }

        /**
         * Widthの値を登録する。
         *
         * @param width Width
         */
        fun setWidth(width: String): Builder {
            this.width = width.toIntOrNull() ?: 0
            return this
        }

        /**
         * Depthの値を登録する
         *
         * @param depth Depth
         */
        fun setDepth(depth: String): Builder {
            this.depth = depth.toIntOrNull() ?: 0
            return this
        }

        /**
         * URLの値を登録する。
         *
         * @param url URL
         */
        fun setUrl(url: String): Builder {
            this.url = url
            return this
        }
    }
}
