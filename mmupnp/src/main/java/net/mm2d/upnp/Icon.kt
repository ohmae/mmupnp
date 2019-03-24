/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

/**
 * Iconを表すインターフェース。
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface Icon {
    /**
     * MimeTypeの値を返す。
     *
     * Required. Icon's MIME type (see RFC 2045, 2046, and 2387). Single MIME image type.
     * At least one icon should be of type “image/png” (Portable Network Graphics, see IETF RFC 2083).
     *
     * @return MimeType
     */
    val mimeType: String

    /**
     * Heightの値を返す。
     *
     * Required. Vertical dimension of icon in pixels. Integer.
     *
     * @return Height
     */
    val height: Int

    /**
     * Widthの値を返す。
     *
     * Required. Horizontal dimension of icon in pixels. Integer.
     *
     * @return Width
     */
    val width: Int

    /**
     * Depthの値を返す。
     *
     * Required. Number of color bits per pixel. Integer.
     *
     * @return Depth
     */
    val depth: Int

    /**
     * URLの値を返す。
     *
     * Required. Pointer to icon image. (XML does not support direct embedding of binary data. See note below.)
     * Retrieved via HTTP. Shall be relative to the URL at which the device description is located in accordance with
     * clause 5 of RFC 3986. Specified by UPnP vendor. Single URL.
     *
     * @return URL
     */
    val url: String

    /**
     * バイナリデータを返す。
     *
     * 取扱注意：メモリ節約のためバイナリデータは外部と共有させる。
     *
     * @return バイナリデータ
     */
    val binary: ByteArray?
}
