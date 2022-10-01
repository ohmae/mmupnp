/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import net.mm2d.upnp.ControlPointConfig
import net.mm2d.upnp.Http
import net.mm2d.upnp.Property
import net.mm2d.upnp.internal.impl.launchServer
import net.mm2d.upnp.internal.parser.parseEventXml

/**
 * Class to receive Event notified by event subscription.
 *
 * It only accepts requests as an HTTP server.
 * The listener parses HTTP messages.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class EventReceiver(
    private val config: ControlPointConfig,
    private val listener: suspend (sid: String, seq: Long, properties: List<Pair<String, String>>) -> Boolean
) {
    private var applicationEngine: ApplicationEngine? = null
    private val localPortFlow: MutableSharedFlow<Int> = MutableSharedFlow(replay = 1)
    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = config.launchServer {
            val engine = embeddedServer(CIO, port = 0, host = "0.0.0.0") {
                configureRouting()
            }
            applicationEngine = engine
            engine.start(false)
            localPortFlow.emit(engine.resolvedConnectors().first().port)
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        applicationEngine?.stop()
        applicationEngine = null
    }

    private fun Application.configureRouting() {
        routing {
            method(HttpMethod.parse("NOTIFY")) {
                handle {
                    val nt = call.request.header(Http.NT)
                    val nts = call.request.header(Http.NTS)
                    val sid = call.request.header(Http.SID)
                    if (nt.isNullOrEmpty() || nts.isNullOrEmpty()) {
                        call.respondBadRequest()
                    } else if (sid.isNullOrEmpty() || nt != Http.UPNP_EVENT || nts != Http.UPNP_PROPCHANGE) {
                        call.respondPreconditionFailed()
                    } else if (notifyEvent(sid, call.request)) {
                        call.respondOk()
                    } else {
                        call.respondPreconditionFailed()
                    }
                }
            }
        }
    }

    private suspend fun ApplicationCall.respondOk() {
        response.status(HttpStatusCode.OK)
        response.header(Http.SERVER, Property.SERVER_VALUE)
        respondBytes(byteArrayOf())
    }

    private suspend fun ApplicationCall.respondPreconditionFailed() {
        response.status(HttpStatusCode.PreconditionFailed)
        response.header(Http.SERVER, Property.SERVER_VALUE)
        respondBytes(byteArrayOf())
    }

    private suspend fun ApplicationCall.respondBadRequest() {
        response.status(HttpStatusCode.BadRequest)
        response.header(Http.SERVER, Property.SERVER_VALUE)
        respondBytes(byteArrayOf())
    }

    suspend fun getLocalPort(): Int = localPortFlow.first()

    // VisibleForTesting
    internal suspend fun notifyEvent(sid: String, request: ApplicationRequest): Boolean {
        val seq = request.header(Http.SEQ)?.toLongOrNull() ?: return false
        val properties = request.receiveChannel().readRemaining().readText().parseEventXml()
        println(properties)
        if (properties.isEmpty()) return false
        return listener(sid, seq, properties)
    }
}
