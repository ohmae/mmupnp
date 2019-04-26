/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

import net.mm2d.upnp.Adapter.taskExecutor
import net.mm2d.upnp.internal.impl.ControlPointImpl
import net.mm2d.upnp.internal.impl.DiFactory
import java.net.NetworkInterface

/**
 * Factory to create an instance of ControlPoint.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
object ControlPointFactory {
    /**
     * Create an instance of ControlPoint.
     *
     * @param protocol protocol stack.
     * If not specified, the default protocol stack is used.
     * @param interfaces network interface.
     * If not specified, it is automatically selected from the protocol stack.
     * @param callbackExecutor callback executor.
     * All callbacks are executed from the callback thread.
     * This argument is used to specify the callback thread.
     * If not specified, single Thread Executor is used.
     * @param callbackHandler callback handler.
     * This is used as a TaskExecutor.
     * All callbacks are executed from the callback thread.
     * This argument is used to specify the callback thread.
     * If not specified, single Thread Executor is used.
     * @param notifySegmentCheckEnabled Set whether to check the segment of SSDP Notify packet or not.
     * @return Instance of ControlPoint.
     * @throws IllegalStateException There is no interface available.
     */
    @JvmStatic
    fun create(
        protocol: Protocol = Protocol.DEFAULT,
        interfaces: Iterable<NetworkInterface>? = null,
        callbackExecutor: TaskExecutor? = null,
        callbackHandler: ((Runnable) -> Boolean)? = null,
        notifySegmentCheckEnabled: Boolean = false
    ): ControlPoint {
        val executor = callbackExecutor
            ?: callbackHandler?.let { taskExecutor(it) }
        return ControlPointImpl(
            protocol,
            getDefaultInterfacesIfEmpty(protocol, interfaces),
            notifySegmentCheckEnabled,
            DiFactory(protocol, executor)
        )
    }

    private fun getDefaultInterfacesIfEmpty(
        protocol: Protocol,
        interfaces: Iterable<NetworkInterface>?
    ): Iterable<NetworkInterface> {
        return if (interfaces?.none() != false) {
            protocol.getAvailableInterfaces()
        } else interfaces
    }

    /**
     * Return Builder instance.
     */
    @JvmStatic
    fun builder(): ControlPointBuilder = ControlPointBuilder()

    /**
     * Builder for ControlPoint.
     *
     * Mainly assuming to be use from Java.
     */
    class ControlPointBuilder internal constructor() {
        private var protocol: Protocol = Protocol.DEFAULT
        private var interfaces: Iterable<NetworkInterface>? = null
        private var callbackExecutor: TaskExecutor? = null
        private var notifySegmentCheckEnabled: Boolean = false

        /**
         * Set protocol stack.
         *
         * If not specified, the default protocol stack is used.
         *
         * @param proto Protocol stack
         * @return builder
         */
        fun setProtocol(proto: Protocol): ControlPointBuilder {
            protocol = proto
            return this
        }

        /**
         * Set network interface.
         *
         * If not specified, it is automatically selected from the protocol stack.
         *
         * @param ifs Network interface
         * @return builder
         */
        fun setInterfaces(ifs: Iterable<NetworkInterface>?): ControlPointBuilder {
            interfaces = ifs
            return this
        }

        /**
         * Set callback executor.
         *
         * All callbacks are executed from the callback thread.
         * This argument is used to specify the callback thread.
         * If not specified, single Thread Executor is used.
         *
         * @param executor callback executor
         * @return builder
         */
        fun setCallbackExecutor(executor: TaskExecutor): ControlPointBuilder {
            callbackExecutor = executor
            return this
        }

        /**
         * Set callback handler.
         *
         * This is used as a TaskExecutor.
         * All callbacks are executed from the callback thread.
         * This argument is used to specify the callback thread.
         * If not specified, single Thread Executor is used.
         *
         * @param handler callback executor
         * @return builder
         */
        fun setCallbackHandler(handler: (Runnable) -> Boolean): ControlPointBuilder {
            callbackExecutor = taskExecutor(handler)
            return this
        }

        /**
         * Set whether to check the segment of SSDP Notify packet or not.
         *
         * @param enabled true, check the segment. false, otherwise
         * @return builder
         */
        fun setNotifySegmentCheckEnabled(enabled: Boolean): ControlPointBuilder {
            notifySegmentCheckEnabled = enabled
            return this
        }

        /**
         * Build an instance of ControlPoint.
         *
         * @return instance of ControlPoint
         */
        fun build(): ControlPoint = ControlPointImpl(
            protocol,
            getDefaultInterfacesIfEmpty(protocol, interfaces),
            notifySegmentCheckEnabled,
            DiFactory(protocol, callbackExecutor)
        )
    }
}
