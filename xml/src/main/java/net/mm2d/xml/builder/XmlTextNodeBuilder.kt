/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 *  http://opensource.org/licenses/MIT
 */

package net.mm2d.xml.builder

import net.mm2d.xml.node.XmlTextNode

class XmlTextNodeBuilder(
    value: String
) : XmlNodeBuilder {
    private val valueBuilder = StringBuilder(value)

    override var parent: XmlElementBuilder? = null

    val value: String
        get() = valueBuilder.toString()

    fun appendText(text: String) {
        valueBuilder.append(text)
    }

    override fun build(removeIgnorableWhitespace: Boolean): XmlTextNode = XmlTextNode(value)
}
