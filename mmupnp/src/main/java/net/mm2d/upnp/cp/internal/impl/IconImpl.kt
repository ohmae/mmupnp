/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.cp.internal.impl

import net.mm2d.upnp.common.Http
import net.mm2d.upnp.common.HttpClient
import net.mm2d.upnp.common.internal.property.IconProperty
import net.mm2d.upnp.cp.Icon
import java.io.IOException

/**
 * Implements for [Icon].
 *
 * @author [大前良介(OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class IconImpl(
    property: IconProperty
) : Icon {
    override val mimeType: String = property.mimeType
    override val height: Int = property.height
    override val width: Int = property.width
    override val depth: Int = property.depth
    override val url: String = property.url
    override var binary: ByteArray? = null
        private set

    @Throws(IOException::class)
    fun loadBinary(client: HttpClient, baseUrl: String, scopeId: Int) {
        val url = Http.makeAbsoluteUrl(baseUrl, url, scopeId)
        binary = client.downloadBinary(url)
    }
}
