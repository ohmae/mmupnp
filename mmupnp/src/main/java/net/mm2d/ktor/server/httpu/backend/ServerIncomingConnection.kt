package net.mm2d.ktor.server.httpu.backend

import io.ktor.util.network.*
import io.ktor.utils.io.*

class ServerIncomingConnection(
    val input: ByteReadChannel,
    val output: ByteWriteChannel,
    val remoteAddress: NetworkAddress?,
    val localAddress: NetworkAddress?
)
