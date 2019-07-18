/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.util

import net.mm2d.log.toSimpleStackTraceString

internal fun Throwable.toSimpleTrace() = toSimpleStackTraceString(setOf("net.mm2d.upnp."))
