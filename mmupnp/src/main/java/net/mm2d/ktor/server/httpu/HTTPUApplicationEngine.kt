package net.mm2d.ktor.server.httpu

import io.ktor.http.cio.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.util.pipeline.*
import kotlinx.atomicfu.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.mm2d.ktor.server.httpu.backend.ServerRequestScope
import net.mm2d.ktor.server.httpu.backend.httpServer
import net.mm2d.ktor.server.httpu.backend.port
import net.mm2d.ktor.server.httpu.internal.IOBridge

class HTTPUApplicationEngine(
    environment: ApplicationEngineEnvironment,
    configure: Configuration.() -> Unit
) : BaseApplicationEngine(environment) {
    class Configuration : BaseApplicationEngine.Configuration() {
        var connectionIdleTimeoutSeconds: Int = 45
    }

    private val configuration: Configuration = Configuration().apply(configure)
    private val engineDispatcher = Dispatchers.IOBridge
    private val userDispatcher = Dispatchers.IOBridge
    private val startupJob: CompletableDeferred<Unit> = CompletableDeferred()
    private val stopRequest: CompletableJob = Job()
    private var serverJob: Job by atomic(Job())

    init {
        serverJob = initServerJob()
        serverJob.invokeOnCompletion { cause ->
            cause?.let { stopRequest.completeExceptionally(cause) }
            cause?.let { startupJob.completeExceptionally(cause) }
            environment.stop()
        }
    }

    override fun start(wait: Boolean): ApplicationEngine {
        serverJob.start()

        runBlocking {
            startupJob.await()

            if (wait) {
                serverJob.join()
            }
        }

        return this
    }

    override fun stop(gracePeriodMillis: Long, timeoutMillis: Long) {
        shutdownServer(gracePeriodMillis, timeoutMillis)
    }

    private fun shutdownServer(gracePeriodMillis: Long, timeoutMillis: Long) {
        stopRequest.complete()

        runBlocking {
            val result = withTimeoutOrNull(gracePeriodMillis) {
                serverJob.join()
                true
            }

            if (result == null) {
                // timeout
                serverJob.cancel()

                val forceShutdown = withTimeoutOrNull(timeoutMillis - gracePeriodMillis) {
                    serverJob.join()
                    false
                } ?: true

                if (forceShutdown) {
                    environment.stop()
                }
            }
        }
    }

    private fun CoroutineScope.startConnector(host: String, port: Int): HttpServer {
        val settings = HttpServerSettings(
            host = host,
            port = port,
            connectionIdleTimeoutSeconds = configuration.connectionIdleTimeoutSeconds.toLong()
        )

        return httpServer(settings) { request ->
            handleRequest(request)
        }
    }

    private suspend fun ServerRequestScope.handleRequest(request: Request) {
        withContext(userDispatcher) {
            val call = HTTPUApplicationCall(
                application,
                request,
                input,
                output,
                engineDispatcher,
                userDispatcher,
                upgraded,
                remoteAddress,
                localAddress,
            )

            try {
                pipeline.execute(call)
            } catch (error: Throwable) {
                handleFailure(call, error)
            } finally {
                call.release()
            }
        }
    }

    private fun initServerJob(): Job {
        val environment = environment
        val userDispatcher = userDispatcher
        val stopRequest = stopRequest
        val startupJob = startupJob
        val httpuConnectors = resolvedConnectors

        return CoroutineScope(
            environment.parentCoroutineContext + engineDispatcher
        ).launch(start = CoroutineStart.LAZY) {
            val connectors = ArrayList<HttpServer>(environment.connectors.size)

            try {
                environment.connectors.forEach { connectorSpec ->
                    if (connectorSpec.type == ConnectorType.HTTPS) {
                        throw UnsupportedOperationException(
                            "CIO Engine does not currently support HTTPS. Please " +
                                "consider using a different engine if you require HTTPS"
                        )
                    }
                }

                withContext(userDispatcher) {
                    environment.start()
                }

                val connectorsAndServers = environment.connectors.map { connectorSpec ->
                    connectorSpec to startConnector(connectorSpec.host, connectorSpec.port)
                }
                connectors.addAll(connectorsAndServers.map { it.second })

                val resolvedConnectors = connectorsAndServers
                    .map { (connector, server) -> connector to server.serverSocket.await() }
                    .map { (connector, socket) -> connector.withPort(socket.localAddress.port) }
                httpuConnectors.complete(resolvedConnectors)
            } catch (cause: Throwable) {
                connectors.forEach { it.rootServerJob.cancel() }
                stopRequest.completeExceptionally(cause)
                startupJob.completeExceptionally(cause)
                throw cause
            }

            startupJob.complete(Unit)
            stopRequest.join()

            // stopping
            connectors.forEach { it.acceptJob.cancel() }

            withContext(userDispatcher) {
                environment.monitor.raise(ApplicationStopPreparing, environment)
            }
        }
    }
}
