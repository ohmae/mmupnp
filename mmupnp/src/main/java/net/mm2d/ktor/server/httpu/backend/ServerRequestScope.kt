package net.mm2d.ktor.server.httpu.backend

import io.ktor.util.network.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

class ServerRequestScope internal constructor(
    override val coroutineContext: CoroutineContext,
    val input: ByteReadChannel,
    val output: ByteWriteChannel,
    val remoteAddress: NetworkAddress?,
    val localAddress: NetworkAddress?,
    val upgraded: CompletableDeferred<Boolean>?
) : CoroutineScope {
    fun withContext(coroutineContext: CoroutineContext): ServerRequestScope =
        ServerRequestScope(
            this.coroutineContext + coroutineContext,
            input,
            output,
            remoteAddress,
            localAddress,
            upgraded
        )
}
