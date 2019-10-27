/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.message

import net.mm2d.upnp.common.Http
import net.mm2d.upnp.common.HttpMessage
import java.util.*

internal const val DEFAULT_MAX_AGE = 1800
internal fun HttpMessage.parseCacheControl(): Int {
    val age = getHeader(Http.CACHE_CONTROL)?.toLowerCase(Locale.US)
    if (age?.startsWith("max-age") != true) {
        return DEFAULT_MAX_AGE
    }
    return age.substringAfter('=', "").toIntOrNull() ?: DEFAULT_MAX_AGE
}

internal fun HttpMessage.parseUsn(): Pair<String, String> {
    val usn = getHeader(Http.USN)
    if (usn.isNullOrEmpty() || !usn.startsWith("uuid")) {
        return "" to ""
    }
    val pos = usn.indexOf("::")
    return if (pos < 0) usn to ""
    else usn.substring(0, pos) to usn.substring(pos + 2)
}
