package net.mm2d.ktor.server.httpu.backend

import io.ktor.http.*
import io.ktor.http.cio.*
import io.ktor.http.cio.internals.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import net.mm2d.ktor.server.httpu.HttpRequestHandler
import net.mm2d.ktor.server.httpu.HttpPipelineCoroutine
import net.mm2d.ktor.server.httpu.HttpPipelineWriterCoroutine
import net.mm2d.ktor.server.httpu.RequestHandlerCoroutine
import net.mm2d.ktor.server.httpu.internal.WeakTimeoutQueue

fun CoroutineScope.startServerConnectionPipeline(
    connection: ServerIncomingConnection,
    timeout: WeakTimeoutQueue,
    handler: HttpRequestHandler
): Job = launch(HttpPipelineCoroutine) {
    val actorChannel = Channel<ByteReadChannel>(capacity = 3)

    launch(
        context = HttpPipelineWriterCoroutine,
        start = CoroutineStart.UNDISPATCHED
    ) {
        try {
            pipelineWriterLoop(actorChannel, timeout, connection)
        } catch (cause: Throwable) {
            connection.output.close(cause)
        } finally {
            connection.output.close()
        }
    }

    val requestContext = RequestHandlerCoroutine + Dispatchers.Unconfined

    try {
        while (true) { // parse requests loop
            val request = try {
                parseRequest(connection.input) ?: break
            } catch (cause: TooLongLineException) {
                respondBadRequest(actorChannel)
                break // end pipeline loop
            } catch (io: IOException) {
                throw io
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (parseFailed: Throwable) { // try to write 400 Bad Request
                respondBadRequest(actorChannel)
                break // end pipeline loop
            }

            val response = ByteChannel()

            val transferEncoding = request.headers["Transfer-Encoding"]
            val upgrade = request.headers["Upgrade"]
            val contentType = request.headers["Content-Type"]
            val http11 = request.version == "HTTP/1.1"

            val connectionOptions: ConnectionOptions?
            val contentLength: Long
            val expectedHttpBody: Boolean
            val expectedHttpUpgrade: Boolean

            try {
                actorChannel.send(response)
            } catch (cause: Throwable) {
                request.release()
                throw cause
            }

            try {
                val contentLengthIndex = request.headers.find("Content-Length")
                connectionOptions = ConnectionOptions.parse(request.headers["Connection"])

                if (contentLengthIndex != -1) {
                    contentLength = request.headers.valueAt(contentLengthIndex).parseDecLong()
                    if (request.headers.find("Content-Length", contentLengthIndex + 1) != -1) {
                        throw ParserException("Duplicate Content-Length header")
                    }
                } else {
                    contentLength = -1
                }
                expectedHttpBody = expectHttpBody(
                    request.method,
                    contentLength,
                    transferEncoding,
                    connectionOptions,
                    contentType
                )
                expectedHttpUpgrade = !expectedHttpBody && expectHttpUpgrade(request.method, upgrade, connectionOptions)
            } catch (cause: Throwable) {
                request.release()
                response.writePacket(BadRequestPacket.copy())
                response.close()
                break
            }

            val requestBody = if (expectedHttpBody || expectedHttpUpgrade) {
                ByteChannel(true)
            } else {
                ByteReadChannel.Empty
            }

            val upgraded = if (expectedHttpUpgrade) CompletableDeferred<Boolean>() else null

            launch(requestContext, start = CoroutineStart.UNDISPATCHED) {
                val handlerScope = ServerRequestScope(
                    coroutineContext,
                    requestBody,
                    response,
                    connection.remoteAddress,
                    connection.localAddress,
                    upgraded
                )

                try {
                    handler(handlerScope, request)
                } catch (cause: Throwable) {
                    response.close(cause)
                    upgraded?.completeExceptionally(cause)
                } finally {
                    response.close()
                    upgraded?.complete(false)
                }
            }

            if (upgraded != null) {
                if (upgraded.await()) { // suspend pipeline until we know if upgrade performed?
                    actorChannel.close()
                    connection.input.copyAndClose(requestBody as ByteChannel)
                    break
                } else if (!expectedHttpBody && requestBody is ByteChannel) { // not upgraded, for example 404
                    requestBody.close()
                }
            }

            if (expectedHttpBody && requestBody is ByteWriteChannel) {
                try {
                    parseHttpBody(
                        contentLength,
                        transferEncoding,
                        connectionOptions,
                        connection.input,
                        requestBody
                    )
                } catch (cause: Throwable) {
                    requestBody.close(ChannelReadException("Failed to read request body", cause))
                    response.writePacket(BadRequestPacket.copy())
                    response.close()
                    break
                } finally {
                    requestBody.close()
                }
            }

            if (isLastHttpRequest(http11, connectionOptions)) break
        }
    } catch (cause: IOException) { // already handled
        coroutineContext.cancel()
    } finally {
        actorChannel.close()
    }
}

private suspend fun respondBadRequest(actorChannel: Channel<ByteReadChannel>) {
    val bc = ByteChannel()
    if (actorChannel.trySend(bc).isSuccess) {
        bc.writePacket(BadRequestPacket.copy())
        bc.close()
    }
    actorChannel.close()
}

private suspend fun pipelineWriterLoop(
    channel: ReceiveChannel<ByteReadChannel>,
    timeout: WeakTimeoutQueue,
    connection: ServerIncomingConnection
) {
    val receiveChildOrNull = suspendLambda<CoroutineScope, ByteReadChannel?> {
        channel.receiveCatching().getOrNull()
    }

    while (true) {
        val child = timeout.withTimeout(receiveChildOrNull) ?: break
        try {
            child.joinTo(connection.output, false)
            connection.output.flush()
        } catch (cause: Throwable) {
            if (child is ByteWriteChannel) {
                child.close(cause)
            }
        }
    }
}

private val BadRequestPacket = RequestResponseBuilder().apply {
    responseLine("HTTP/1.0", HttpStatusCode.BadRequest.value, "Bad Request")
    headerLine("Connection", "close")
    emptyLine()
}.build()

internal fun isLastHttpRequest(http11: Boolean, connectionOptions: ConnectionOptions?): Boolean {
    return when {
        connectionOptions == null -> !http11
        connectionOptions.keepAlive -> false
        connectionOptions.close -> true
        else -> false
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun <S, R> suspendLambda(noinline block: suspend S.() -> R): suspend S.() -> R = block
