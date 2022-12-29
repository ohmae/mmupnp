@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package net.mm2d.ktor.network.sockets

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import java.net.InetAddress
import java.net.StandardSocketOptions
import java.nio.channels.DatagramChannel

class MulticastSocketBuilder(
    private val selector: SelectorManager,
    override var options: SocketOptions.UDPSocketOptions
) : Configurable<MulticastSocketBuilder, SocketOptions.UDPSocketOptions> {
    fun bind(
        localAddress: SocketAddress? = null,
        multicastOptions: MulticastSocketOptions,
        configure: SocketOptions.UDPSocketOptions.() -> Unit = {}
    ): BoundDatagramSocket = bindAndJoinGroup(
        selector,
        localAddress,
        null,
        multicastOptions,
        options.udp().also { it.reuseAddress = true }.apply(configure),
    )

    fun bindAndJoinGroup(
        localAddress: SocketAddress? = null,
        group: InetAddress? = null,
        multicastOptions: MulticastSocketOptions,
        configure: SocketOptions.UDPSocketOptions.() -> Unit = {}
    ): BoundDatagramSocket = bindAndJoinGroup(
        selector,
        localAddress,
        group,
        multicastOptions,
        options.udp().also { it.reuseAddress = true }.apply(configure),
    )

    companion object
}

internal fun MulticastSocketBuilder.Companion.bindAndJoinGroup(
    selector: SelectorManager,
    localAddress: SocketAddress?,
    group: InetAddress?,
    multicastOptions: MulticastSocketOptions,
    udpOptions: SocketOptions.UDPSocketOptions
): BoundDatagramSocket = selector.buildOrClose({ openDatagramChannel() }) {
    assignOptions(multicastOptions)
    assignOptions(udpOptions)
    nonBlocking()
    if (java7NetworkApisAvailable) {
        bind(localAddress?.toJavaAddress())
    } else {
        socket().bind(localAddress?.toJavaAddress())
    }
    val key = group?.let {
        join(it, multicastOptions.networkInterface)
    }
    return MulticastDatagramSocketImpl(DatagramSocketImpl(this, selector), key)
}

internal fun DatagramChannel.assignOptions(options: MulticastSocketOptions) {
    if (options.networkInterface != null) {
        setOption(StandardSocketOptions.IP_MULTICAST_IF, options.networkInterface)
    }
    if (options.loop) {
        setOption(StandardSocketOptions.IP_MULTICAST_LOOP, true)
    }
    if (options.timeToLive >= 0) {
        setOption(StandardSocketOptions.IP_MULTICAST_TTL, options.timeToLive)
    }
}
