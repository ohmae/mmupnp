package net.mm2d.upnp

import io.ktor.client.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

interface ControlPointConfig {
    fun initialize()
    fun terminate()
    fun scope(): CoroutineScope
    fun clientDispatcher(): CoroutineDispatcher
    fun serverDispatcher(): CoroutineDispatcher
    fun createHttpClient(): HttpClient
}
