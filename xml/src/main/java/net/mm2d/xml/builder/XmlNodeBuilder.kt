/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 *  http://opensource.org/licenses/MIT
 */

package net.mm2d.xml.builder

import net.mm2d.xml.node.XmlNode

interface XmlNodeBuilder {
    var parent: XmlElementBuilder?
    fun build(removeIgnorableWhitespace: Boolean = true): XmlNode
}

