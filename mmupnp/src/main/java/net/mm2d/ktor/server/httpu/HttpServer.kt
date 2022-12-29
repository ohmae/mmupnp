package net.mm2d.ktor.server.httpu

import io.ktor.network.sockets.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

class HttpServer (
    val rootServerJob: Job,
    val acceptJob: Job,
    val serverSocket: Deferred<ServerSocket>
)

data class HttpServerSettings(
    val host: String = "0.0.0.0",
    val port: Int = 8080,
    val connectionIdleTimeoutSeconds: Long = 45
)
