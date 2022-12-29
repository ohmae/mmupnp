package net.mm2d.ktor.server.httpu

import io.ktor.http.cio.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.util.network.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CompletableDeferred
import kotlin.coroutines.CoroutineContext

class HTTPUApplicationCall(
    application: Application,
    _request: Request,
    input: ByteReadChannel,
    output: ByteWriteChannel,
    engineDispatcher: CoroutineContext,
    appDispatcher: CoroutineContext,
    upgraded: CompletableDeferred<Boolean>?,
    remoteAddress: NetworkAddress?,
    localAddress: NetworkAddress?,
) : BaseApplicationCall(application) {
    override val request = HTTPUApplicationRequest(this, remoteAddress, localAddress, input, _request)
    override val response = HTTPUApplicationResponse(this, output, input, engineDispatcher, appDispatcher, upgraded)

    internal fun release() {
        request.release()
    }

    init {
        putResponseAttribute()
    }
}
