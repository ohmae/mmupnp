package net.mm2d.upnp.internal.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.mm2d.upnp.ControlPointConfig

fun ControlPointConfig.launchClient(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = scope().launch(clientDispatcher(), start, block)

fun ControlPointConfig.launchServer(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = scope().launch(serverDispatcher(), start, block)
