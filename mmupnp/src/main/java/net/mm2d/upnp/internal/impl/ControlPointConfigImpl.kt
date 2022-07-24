package net.mm2d.upnp.internal.impl

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import net.mm2d.upnp.ControlPointConfig

class ControlPointConfigImpl : ControlPointConfig {
    override fun createHttpClient(): HttpClient = HttpClient(CIO)

    override fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
