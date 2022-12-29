package net.mm2d.ktor.server.httpu.backend

import io.ktor.network.sockets.*
import io.ktor.util.network.*

internal val SocketAddress.port: Int
    get() {
        val inetAddress = this as? InetSocketAddress ?: error("Expected inet socket address")
        return inetAddress.port
    }

internal fun SocketAddress.toNetworkAddress(): NetworkAddress {
    return toJavaAddress() as? java.net.InetSocketAddress ?: error("Expected inet socket address")
}
