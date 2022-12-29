@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package net.mm2d.ktor.network.sockets

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import java.net.NetworkInterface

fun aMulticastSocket(selector: SelectorManager): SocketBuilder = SocketBuilder(selector, SocketOptions.create())

@Suppress("PublicApiImplicitType", "unused")
class SocketBuilder internal constructor(
    private val selector: SelectorManager,
    override var options: SocketOptions
) : Configurable<SocketBuilder, SocketOptions> {
    fun multicast(): MulticastSocketBuilder = MulticastSocketBuilder(selector, options.peer().udp())
}

class MulticastSocketOptions(
    val networkInterface: NetworkInterface? = null,
    val loop: Boolean = false,
    val timeToLive: Int = -1,
)
