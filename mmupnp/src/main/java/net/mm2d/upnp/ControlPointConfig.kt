package net.mm2d.upnp

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher

interface ControlPointConfig {
    fun createHttpClient(): HttpClient
    fun ioDispatcher(): CoroutineDispatcher
}
