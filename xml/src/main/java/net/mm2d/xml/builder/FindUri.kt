package net.mm2d.xml.builder

internal fun findUri(prefix: String, element: XmlElementBuilder): String =
    element.prefixMap[prefix]
        ?: element.parent?.let { findUri(prefix, it) }
        ?: ""
