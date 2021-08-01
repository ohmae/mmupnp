/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 *  http://opensource.org/licenses/MIT
 */

package net.mm2d.xml.node

interface XmlNode {
    var parent: XmlElement?
    fun toXmlString(indent: Boolean, sb: StringBuilder, depth: Int)
}
