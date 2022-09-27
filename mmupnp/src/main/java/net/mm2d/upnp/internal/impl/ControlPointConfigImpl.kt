package net.mm2d.upnp.internal.impl

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import net.mm2d.upnp.ControlPointConfig

class ControlPointConfigImpl : ControlPointConfig {
    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)
    override fun initialize() {
    }

    override fun terminate() {
        scope.cancel()
    }

    override fun scope(): CoroutineScope = scope

    override fun clientDispatcher(): CoroutineDispatcher = Dispatchers.IO

    override fun serverDispatcher(): CoroutineDispatcher = Dispatchers.IO

    override fun createHttpClient(): HttpClient = HttpClient(OkHttp)
}
