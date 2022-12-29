package net.mm2d.ktor.server.httpu.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

val Dispatchers.IOBridge: CoroutineDispatcher
    get() = IO
