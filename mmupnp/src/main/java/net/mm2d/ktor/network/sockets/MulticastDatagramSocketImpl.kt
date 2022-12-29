@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package net.mm2d.ktor.network.sockets

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.CoroutineScope
import java.nio.channels.MembershipKey

internal class MulticastDatagramSocketImpl(
    private val delegate: DatagramSocketImpl,
    private val membershipKey: MembershipKey?,
) : BoundDatagramSocket by delegate,
    ConnectedDatagramSocket,
    AConnectedSocket by delegate,
    CoroutineScope by delegate,
    Selectable by delegate {

    override fun dispose() {
        delegate.dispose()
    }

    override fun close() {
        membershipKey?.drop()
        delegate.close()
    }
}
