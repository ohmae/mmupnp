/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 *  http://opensource.org/licenses/MIT
 */

package net.mm2d.xml.node

internal fun escapeXmlString(s: String, forQuote: Boolean = false): String = buildString {
    var i = 0
    while (i < s.length) {
        val c = s[i]
        when {
            c == '<' -> append("&lt;")
            c == '>' -> append("&gt;")
            c == '&' -> append("&amp;")
            c == '"' -> if (forQuote) append("&quot;") else append(c)
            c == '\'' -> if (forQuote) append("&apos;") else append(c)
            c.code in 0xd800..0xdc00 -> {
                // UTF-16 surrogate
                if (i + 1 >= s.length)
                    throw IllegalArgumentException("Invalid UTF-16 surrogate: %x".format(c.code))
                val n = s[++i]
                if (n.code !in 0xdc00..0xe000)
                    throw IllegalArgumentException("Invalid UTF-16 surrogate: %x %x".format(c.code, n.code))
                append("&#x")
                append(Integer.toHexString((c.code - 0xd800 shl 10) + n.code - 0xdc00 + 0x00010000))
                append(";")
            }
            else -> append(c)
        }
        i++
    }
}
