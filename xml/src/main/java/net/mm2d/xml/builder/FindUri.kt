/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 *  http://opensource.org/licenses/MIT
 */

package net.mm2d.xml.builder

internal fun findUri(prefix: String, element: XmlElementBuilder): String =
    element.prefixMap[prefix]
        ?: element.parent?.let { findUri(prefix, it) }
        ?: ""
