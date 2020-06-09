package net.mm2d.upnp.da.internal.server

import net.mm2d.upnp.common.internal.server.SsdpServer
import net.mm2d.upnp.common.internal.server.SsdpServerDelegate

internal class SsdpAdvertiser(
    private val delegate: SsdpServerDelegate
) : SsdpServer by delegate {
}
