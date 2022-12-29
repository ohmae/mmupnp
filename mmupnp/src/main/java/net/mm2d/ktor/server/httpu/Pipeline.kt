package net.mm2d.ktor.server.httpu

import io.ktor.http.cio.*
import kotlinx.coroutines.CoroutineName
import net.mm2d.ktor.server.httpu.backend.ServerRequestScope

typealias HttpRequestHandler = suspend ServerRequestScope.(request: Request) -> Unit

val HttpPipelineCoroutine: CoroutineName = CoroutineName("http-pipeline")
val HttpPipelineWriterCoroutine: CoroutineName = CoroutineName("http-pipeline-writer")
val RequestHandlerCoroutine: CoroutineName = CoroutineName("request-handler")
