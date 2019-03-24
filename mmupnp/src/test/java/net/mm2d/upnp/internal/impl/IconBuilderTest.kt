/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class IconBuilderTest {
    @Test
    fun build() {
        val mimeType = "mimeType"
        val height = 200
        val width = 300
        val depth = 24
        val url = "http://192.168.0.1/"
        val icon = IconImpl.Builder()
                .setMimeType(mimeType)
                .setWidth(width.toString())
                .setHeight(height.toString())
                .setDepth(depth.toString())
                .setUrl(url)
                .build()

        assertThat(icon.mimeType).isEqualTo(mimeType)
        assertThat(icon.width).isEqualTo(width)
        assertThat(icon.height).isEqualTo(height)
        assertThat(icon.depth).isEqualTo(depth)
        assertThat(icon.url).isEqualTo(url)
    }

    @Test(expected = IllegalStateException::class)
    fun build_mimeTypeなし() {
        val height = 200
        val width = 300
        val depth = 24
        val url = "http://192.168.0.1/"
        IconImpl.Builder()
                .setWidth(width.toString())
                .setHeight(height.toString())
                .setDepth(depth.toString())
                .setUrl(url)
                .build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_width異常() {
        val mimeType = "mimeType"
        val height = 200
        val depth = 24
        val url = "http://192.168.0.1/"
        IconImpl.Builder()
                .setMimeType(mimeType)
                .setWidth("width")
                .setHeight(height.toString())
                .setDepth(depth.toString())
                .setUrl(url)
                .build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_height異常() {
        val mimeType = "mimeType"
        val width = 300
        val depth = 24
        val url = "http://192.168.0.1/"
        IconImpl.Builder()
                .setMimeType(mimeType)
                .setWidth(width.toString())
                .setHeight("height")
                .setDepth(depth.toString())
                .setUrl(url)
                .build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_depth異常() {
        val mimeType = "mimeType"
        val height = 200
        val width = 300
        val url = "http://192.168.0.1/"
        IconImpl.Builder()
                .setMimeType(mimeType)
                .setWidth(width.toString())
                .setHeight(height.toString())
                .setDepth("depth")
                .setUrl(url)
                .build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_urlなし() {
        val mimeType = "mimeType"
        val height = 200
        val width = 300
        val depth = 24
        IconImpl.Builder()
                .setMimeType(mimeType)
                .setWidth(width.toString())
                .setHeight(height.toString())
                .setDepth(depth.toString())
                .build()
    }
}
