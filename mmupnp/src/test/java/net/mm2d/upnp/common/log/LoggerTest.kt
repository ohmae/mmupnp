/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.common.log

import io.mockk.clearMocks
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
class LoggerTest {
    @Test
    fun setLogLevel() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val message = "message"

        Logger.setLogLevel(Logger.ASSERT)
        Logger.v(message)
        Logger.d(message)
        Logger.i(message)
        Logger.w(message)
        Logger.e(message)

        verify(exactly = 0) { sender(any(), message, null) }
        clearMocks(sender)

        Logger.setLogLevel(Logger.ERROR)
        Logger.v(message)
        Logger.d(message)
        Logger.i(message)
        Logger.w(message)
        verify(exactly = 0) { sender(any(), message, null) }
        Logger.e(message)
        verify(exactly = 1) { sender(any(), message, null) }
        clearMocks(sender)

        Logger.setLogLevel(Logger.WARN)
        Logger.v(message)
        Logger.d(message)
        Logger.i(message)
        verify(exactly = 0) { sender(any(), message, null) }
        Logger.w(message)
        Logger.e(message)
        verify(exactly = 2) { sender(any(), message, null) }
        clearMocks(sender)

        Logger.setLogLevel(Logger.INFO)
        Logger.v(message)
        Logger.d(message)
        verify(exactly = 0) { sender(any(), message, null) }
        Logger.i(message)
        Logger.w(message)
        Logger.e(message)
        verify(exactly = 3) { sender(any(), message, null) }
        clearMocks(sender)

        Logger.setLogLevel(Logger.DEBUG)
        Logger.v(message)
        verify(exactly = 0) { sender(any(), message, null) }
        Logger.d(message)
        Logger.i(message)
        Logger.w(message)
        Logger.e(message)
        verify(exactly = 4) { sender(any(), message, null) }
        clearMocks(sender)

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.v(message)
        Logger.d(message)
        Logger.i(message)
        Logger.w(message)
        Logger.e(message)
        verify(exactly = 5) { sender(any(), message, null) }
        clearMocks(sender)
    }

    @Test
    fun v() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val message = "message"

        Logger.setLogLevel(Logger.ASSERT)
        Logger.v(message)

        verify(exactly = 0) { sender(Logger.VERBOSE, message, null) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.v(message)

        verify(exactly = 1) { sender(Logger.VERBOSE, message, null) }
    }

    @Test
    fun v1() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)

        val message = "message"
        val supplier = spyk({ message })

        Logger.setLogLevel(Logger.ASSERT)
        Logger.v(supplier)

        verify(exactly = 0) { supplier() }
        verify(exactly = 0) { sender(Logger.VERBOSE, message, null) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.v(supplier)

        verify(exactly = 1) { supplier() }
        verify(exactly = 1) { sender(Logger.VERBOSE, message, null) }
    }

    @Test
    fun v2() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val throwable = Throwable()

        Logger.setLogLevel(Logger.ASSERT)
        Logger.v(throwable)

        verify(exactly = 0) { sender(Logger.VERBOSE, "", throwable) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.v(throwable)

        verify(exactly = 1) { sender(Logger.VERBOSE, "", throwable) }
    }

    @Test
    fun v3() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val message = "message"
        val throwable = Throwable()

        Logger.setLogLevel(Logger.ASSERT)
        Logger.v(throwable, message)

        verify(exactly = 0) { sender(Logger.VERBOSE, message, throwable) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.v(throwable, message)

        verify(exactly = 1) { sender(Logger.VERBOSE, message, throwable) }
    }

    @Test
    fun v4() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val message = "message"
        val throwable = Throwable()
        val supplier = spyk({ message })

        Logger.setLogLevel(Logger.ASSERT)
        Logger.v(throwable, supplier)

        verify(exactly = 0) { supplier() }
        verify(exactly = 0) { sender(Logger.VERBOSE, message, throwable) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.v(throwable, supplier)

        verify(exactly = 1) { supplier() }
        verify(exactly = 1) { sender(Logger.VERBOSE, message, throwable) }
    }

    @Test
    fun d() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val message = "message"

        Logger.setLogLevel(Logger.ASSERT)
        Logger.d(message)

        verify(exactly = 0) { sender(Logger.DEBUG, message, null) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.d(message)

        verify(exactly = 1) { sender(Logger.DEBUG, message, null) }
    }

    @Test
    fun d1() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)

        val message = "message"
        val supplier = spyk({ message })

        Logger.setLogLevel(Logger.ASSERT)
        Logger.d(supplier)

        verify(exactly = 0) { supplier() }
        verify(exactly = 0) { sender(Logger.DEBUG, message, null) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.d(supplier)

        verify(exactly = 1) { supplier() }
        verify(exactly = 1) { sender(Logger.DEBUG, message, null) }
    }

    @Test
    fun d2() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val throwable = Throwable()

        Logger.setLogLevel(Logger.ASSERT)
        Logger.d(throwable)

        verify(exactly = 0) { sender(Logger.DEBUG, "", throwable) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.d(throwable)

        verify(exactly = 1) { sender(Logger.DEBUG, "", throwable) }
    }

    @Test
    fun d3() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val message = "message"
        val throwable = Throwable()

        Logger.setLogLevel(Logger.ASSERT)
        Logger.d(throwable, message)

        verify(exactly = 0) { sender(Logger.DEBUG, message, throwable) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.d(throwable, message)

        verify(exactly = 1) { sender(Logger.DEBUG, message, throwable) }
    }

    @Test
    fun d4() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val message = "message"
        val throwable = Throwable()
        val supplier = spyk({ message })

        Logger.setLogLevel(Logger.ASSERT)
        Logger.d(throwable, supplier)

        verify(exactly = 0) { supplier() }
        verify(exactly = 0) { sender(Logger.DEBUG, message, throwable) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.d(throwable, supplier)

        verify(exactly = 1) { supplier() }
        verify(exactly = 1) { sender(Logger.DEBUG, message, throwable) }
    }

    @Test
    fun i() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val message = "message"

        Logger.setLogLevel(Logger.ASSERT)
        Logger.i(message)

        verify(exactly = 0) { sender(Logger.INFO, message, null) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.i(message)

        verify(exactly = 1) { sender(Logger.INFO, message, null) }
    }

    @Test
    fun i1() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)

        val message = "message"
        val supplier = spyk({ message })

        Logger.setLogLevel(Logger.ASSERT)
        Logger.i(supplier)

        verify(exactly = 0) { supplier() }
        verify(exactly = 0) { sender(Logger.INFO, message, null) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.i(supplier)

        verify(exactly = 1) { supplier() }
        verify(exactly = 1) { sender(Logger.INFO, message, null) }
    }

    @Test
    fun i2() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val throwable = Throwable()

        Logger.setLogLevel(Logger.ASSERT)
        Logger.i(throwable)

        verify(exactly = 0) { sender(Logger.INFO, "", throwable) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.i(throwable)

        verify(exactly = 1) { sender(Logger.INFO, "", throwable) }
    }

    @Test
    fun i3() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val message = "message"
        val throwable = Throwable()

        Logger.setLogLevel(Logger.ASSERT)
        Logger.i(throwable, message)

        verify(exactly = 0) { sender(Logger.INFO, message, throwable) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.i(throwable, message)

        verify(exactly = 1) { sender(Logger.INFO, message, throwable) }
    }

    @Test
    fun i4() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val message = "message"
        val throwable = Throwable()
        val supplier = spyk({ message })

        Logger.setLogLevel(Logger.ASSERT)
        Logger.i(throwable, supplier)

        verify(exactly = 0) { supplier() }
        verify(exactly = 0) { sender(Logger.INFO, message, throwable) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.i(throwable, supplier)

        verify(exactly = 1) { supplier() }
        verify(exactly = 1) { sender(Logger.INFO, message, throwable) }
    }

    @Test
    fun w() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val message = "message"

        Logger.setLogLevel(Logger.ASSERT)
        Logger.w(message)

        verify(exactly = 0) { sender(Logger.WARN, message, null) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.w(message)

        verify(exactly = 1) { sender(Logger.WARN, message, null) }
    }

    @Test
    fun w1() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)

        val message = "message"
        val supplier = spyk({ message })

        Logger.setLogLevel(Logger.ASSERT)
        Logger.w(supplier)

        verify(exactly = 0) { supplier() }
        verify(exactly = 0) { sender(Logger.WARN, message, null) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.w(supplier)

        verify(exactly = 1) { supplier() }
        verify(exactly = 1) { sender(Logger.WARN, message, null) }
    }

    @Test
    fun w2() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val throwable = Throwable()

        Logger.setLogLevel(Logger.ASSERT)
        Logger.w(throwable)

        verify(exactly = 0) { sender(Logger.WARN, "", throwable) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.w(throwable)

        verify(exactly = 1) { sender(Logger.WARN, "", throwable) }
    }

    @Test
    fun w3() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val message = "message"
        val throwable = Throwable()

        Logger.setLogLevel(Logger.ASSERT)
        Logger.w(throwable, message)

        verify(exactly = 0) { sender(Logger.WARN, message, throwable) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.w(throwable, message)

        verify(exactly = 1) { sender(Logger.WARN, message, throwable) }
    }

    @Test
    fun w4() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val message = "message"
        val throwable = Throwable()
        val supplier = spyk({ message })

        Logger.setLogLevel(Logger.ASSERT)
        Logger.w(throwable, supplier)

        verify(exactly = 0) { supplier() }
        verify(exactly = 0) { sender(Logger.WARN, message, throwable) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.w(throwable, supplier)

        verify(exactly = 1) { supplier() }
        verify(exactly = 1) { sender(Logger.WARN, message, throwable) }
    }

    @Test
    fun e() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val message = "message"

        Logger.setLogLevel(Logger.ASSERT)
        Logger.e(message)

        verify(exactly = 0) { sender(Logger.ERROR, message, null) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.e(message)

        verify(exactly = 1) { sender(Logger.ERROR, message, null) }
    }

    @Test
    fun e1() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)

        val message = "message"
        val supplier = spyk({ message })

        Logger.setLogLevel(Logger.ASSERT)
        Logger.e(supplier)

        verify(exactly = 0) { supplier() }
        verify(exactly = 0) { sender(Logger.ERROR, message, null) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.e(supplier)

        verify(exactly = 1) { supplier() }
        verify(exactly = 1) { sender(Logger.ERROR, message, null) }
    }

    @Test
    fun e2() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val throwable = Throwable()

        Logger.setLogLevel(Logger.ASSERT)
        Logger.e(throwable)

        verify(exactly = 0) { sender(Logger.ERROR, "", throwable) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.e(throwable)

        verify(exactly = 1) { sender(Logger.ERROR, "", throwable) }
    }

    @Test
    fun e3() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val message = "message"
        val throwable = Throwable()

        Logger.setLogLevel(Logger.ASSERT)
        Logger.e(throwable, message)

        verify(exactly = 0) { sender(Logger.ERROR, message, throwable) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.e(throwable, message)

        verify(exactly = 1) { sender(Logger.ERROR, message, throwable) }
    }

    @Test
    fun e4() {
        val sender: (Int, String, Throwable?) -> Unit = spyk({ _, _, _ -> })
        Logger.setSender(sender)
        val message = "message"
        val throwable = Throwable()
        val supplier = spyk({ message })

        Logger.setLogLevel(Logger.ASSERT)
        Logger.e(throwable, supplier)

        verify(exactly = 0) { supplier() }
        verify(exactly = 0) { sender(Logger.ERROR, message, throwable) }

        Logger.setLogLevel(Logger.VERBOSE)
        Logger.e(throwable, supplier)

        verify(exactly = 1) { supplier() }
        verify(exactly = 1) { sender(Logger.ERROR, message, throwable) }
    }
}
