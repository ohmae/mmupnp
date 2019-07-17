package net.mm2d.upnp.internal.util

import net.mm2d.log.toSimpleStackTraceString

internal fun Throwable.toSimpleTrace() = toSimpleStackTraceString(setOf("net.mm2d.upnp."))
