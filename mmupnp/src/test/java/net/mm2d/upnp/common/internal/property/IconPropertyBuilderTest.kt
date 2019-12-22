/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.property

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class IconPropertyBuilderTest {
    @Test
    fun build() {
        val mimeType = "mimeType"
        val height = 200
        val width = 300
        val depth = 24
        val url = "http://192.168.0.1/"
        val icon = IconProperty.Builder().also {
            it.mimeType = mimeType
            it.width = width
            it.height = height
            it.depth = depth
            it.url = url
        }.build()

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
        IconProperty.Builder().also {
            it.width = width
            it.height = height
            it.depth = depth
            it.url = url
        }.build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_width異常() {
        val mimeType = "mimeType"
        val height = 200
        val depth = 24
        val url = "http://192.168.0.1/"
        IconProperty.Builder().also {
            it.mimeType = mimeType
            it.width = 0
            it.height = height
            it.depth = depth
            it.url = url
        }.build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_height異常() {
        val mimeType = "mimeType"
        val width = 300
        val depth = 24
        val url = "http://192.168.0.1/"
        IconProperty.Builder().also {
            it.mimeType = mimeType
            it.width = width
            it.height = 0
            it.depth = depth
            it.url = url
        }.build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_depth異常() {
        val mimeType = "mimeType"
        val height = 200
        val width = 300
        val url = "http://192.168.0.1/"
        IconProperty.Builder().also {
            it.mimeType = mimeType
            it.width = width
            it.height = height
            it.depth = 0
            it.url = url
        }.build()
    }

    @Test(expected = IllegalStateException::class)
    fun build_urlなし() {
        val mimeType = "mimeType"
        val height = 200
        val width = 300
        val depth = 24
        IconProperty.Builder().also {
            it.mimeType = mimeType
            it.width = width
            it.height = height
            it.depth = depth
        }.build()
    }
}
