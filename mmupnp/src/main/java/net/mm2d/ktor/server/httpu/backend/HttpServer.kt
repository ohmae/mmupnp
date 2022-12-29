package net.mm2d.ktor.server.httpu.backend

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.server.engine.*
import io.ktor.server.engine.internal.*
import io.ktor.util.*
import io.ktor.util.logging.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.mm2d.ktor.server.httpu.HttpRequestHandler
import net.mm2d.ktor.server.httpu.HttpServer
import net.mm2d.ktor.server.httpu.HttpServerSettings
import net.mm2d.ktor.server.httpu.internal.WeakTimeoutQueue

fun CoroutineScope.httpServer(
    settings: HttpServerSettings,
    handler: HttpRequestHandler
): HttpServer {
    val socket = CompletableDeferred<BoundDatagramSocket>()

    val serverLatch: CompletableJob = Job()

    val serverJob = launch(
        context = CoroutineName("server-root-${settings.port}"),
        start = CoroutineStart.UNDISPATCHED
    ) {
        serverLatch.join()
    }

    val selector = SelectorManager(coroutineContext)
    val timeout = WeakTimeoutQueue(
        settings.connectionIdleTimeoutSeconds * 1000L
    )

    val logger = KtorSimpleLogger(
        HttpServer::class.simpleName ?: HttpServer::class.qualifiedName ?: HttpServer::class.toString()
    )

    val acceptJob = launch(serverJob + CoroutineName("accept-${settings.port}")) {
        aSocket(selector).udp().bind(InetSocketAddress(settings.host, settings.port)).use { server ->
            socket.complete(server)

            val exceptionHandler = coroutineContext[CoroutineExceptionHandler]
                ?: DefaultUncaughtExceptionHandler(logger)

            val connectionScope = CoroutineScope(
                coroutineContext +
                    SupervisorJob(serverJob) +
                    exceptionHandler +
                    CoroutineName("request")
            )

            try {
                while (true) {
                    server.receive()
                    val client: Socket = server.accept()

                    val connection = ServerIncomingConnection(
                        client.openReadChannel(),
                        client.openWriteChannel(),
                        client.remoteAddress.toNetworkAddress(),
                        client.localAddress.toNetworkAddress()
                    )

                    val clientJob = connectionScope.startServerConnectionPipeline(
                        connection,
                        timeout,
                        handler
                    )

                    clientJob.invokeOnCompletion {
                        client.close()
                    }
                }
            } catch (closed: ClosedChannelException) {
                coroutineContext.cancel()
            } finally {
                server.close()
                server.awaitClosed()
                connectionScope.coroutineContext.cancel()
            }
        }
    }

    acceptJob.invokeOnCompletion { cause ->
        cause?.let { socket.completeExceptionally(it) }
        serverLatch.complete()
        timeout.process()
    }

    @OptIn(InternalCoroutinesApi::class) // TODO it's attach child?
    serverJob.invokeOnCompletion(onCancelling = true) {
        timeout.cancel()
    }
    serverJob.invokeOnCompletion {
        selector.close()
    }

    return HttpServer(serverJob, acceptJob, socket)
}
