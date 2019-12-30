/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.internal.server

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import net.mm2d.upnp.common.internal.server.TcpServerDelegate.ClientTask
import net.mm2d.upnp.common.internal.thread.TaskExecutors
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.net.Socket

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class TcpServerDelegateTest {
    @Test
    fun ClientTask_run() {
        val socket: Socket = mockk(relaxed = true)
        every { socket.getInputStream() } returns mockk(relaxed = true)
        every { socket.getOutputStream() } returns mockk(relaxed = true)
        val taskExecutors = TaskExecutors()
        val delegate = spyk(TcpServerDelegate(taskExecutors, ""))
        val clientTask = ClientTask(taskExecutors, delegate, socket) { _, _ -> }

        clientTask.run()

        verify(exactly = 1) { delegate.notifyClientFinished(clientTask) }
    }
}
