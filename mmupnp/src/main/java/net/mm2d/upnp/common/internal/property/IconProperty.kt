/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.property

/**
 * Interface of UPnP icon.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class IconProperty(
    /**
     * Return the value of MimeType
     *
     * Required. Icon's MIME type (see RFC 2045, 2046, and 2387). Single MIME image type.
     * At least one icon should be of type “image/png” (Portable Network Graphics, see IETF RFC 2083).
     *
     * @return MimeType
     */
    val mimeType: String,

    /**
     * Return the value of Height.
     *
     * Required. Vertical dimension of icon in pixels. Integer.
     *
     * @return Height
     */
    val height: Int,

    /**
     * Return the value of Width.
     *
     * Required. Horizontal dimension of icon in pixels. Integer.
     *
     * @return Width
     */
    val width: Int,

    /**
     * Return the value of Depth.
     *
     * Required. Number of color bits per pixel. Integer.
     *
     * @return Depth
     */
    val depth: Int,

    /**
     * Return the value of URL.
     *
     * Required. Pointer to icon image. (XML does not support direct embedding of binary data. See note below.)
     * Retrieved via HTTP. Shall be relative to the URL at which the device description is located in accordance with
     * clause 5 of RFC 3986. Specified by UPnP vendor. Single URL.
     *
     * @return URL
     */
    val url: String
) {
    class Builder {
        var mimeType: String? = null
        var height: Int = 0
        var width: Int = 0
        var depth: Int = 0
        var url: String? = null

        fun build(): IconProperty {
            val mimeType = checkNotNull(mimeType) { "mimetype must be set." }
            check(width > 0) { "width must be > 0. actually $width" }
            check(height > 0) { "height must be > 0. actually $height" }
            check(depth > 0) { "depth must be > 0. actually $depth" }
            val url = checkNotNull(url) { "url must be set." }

            return IconProperty(
                mimeType = mimeType,
                height = height,
                width = width,
                depth = depth,
                url = url
            )
        }
    }
}
