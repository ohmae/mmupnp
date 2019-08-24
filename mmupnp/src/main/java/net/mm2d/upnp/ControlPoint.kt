/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

/**
 * Interface of UPnP ControlPoint.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface ControlPoint {
    /**
     * Listener to notify discovery event.
     *
     * Notified in callback thread
     */
    interface DiscoveryListener {
        /**
         * Called on device discovery.
         *
         * @param device Discovered device
         * @see Device
         */
        fun onDiscover(device: Device)

        /**
         * Called on device lost.
         *
         * Caused by expiration, SSDP byebye reception, ControlPoint stop,
         *
         * @param device Lost device
         * @see Device
         */
        fun onLost(device: Device)
    }

    /**
     * Listener to notify event of "NotifyEvent".
     *
     * Notified in callback thread
     */
    @Deprecated("use EventListener instead")
    interface NotifyEventListener {
        /**
         * Called on receive NotifyEvent
         *
         * @param service Target Service
         * @param seq Sequence number
         * @param variable variable name
         * @param value value
         * @see Service
         */
        fun onNotifyEvent(service: Service, seq: Long, variable: String, value: String)
    }

    /**
     * Listener to notify on receive Event.
     *
     * Notified in callback thread
     */
    interface EventListener {
        /**
         * Called on receive NotifyEvent
         *
         * @param service Target Service
         * @param seq Sequence number
         * @param properties property name / value pair list
         * @see Service
         */
        fun onEvent(service: Service, seq: Long, properties: List<Pair<String, String>>)
    }

    /**
     * Listener to notify on receive multicast event.
     *
     * Notified in callback thread
     */
    interface MulticastEventListener {
        /**
         * Called on receive Multicast Events
         *
         * @param service Target Service
         * @param lvl Multicast event levels.
         * Format is `<domain>:/<level>`
         * `upnp:/emergency`, `upnp:/fault`, `upnp:/warning`, `upnp:/info`, `upnp:/debug`, `upnp:/general`
         * @param seq Sequence number
         * @param properties property name / value pair list
         * @see Service
         */
        fun onEvent(service: Service, lvl: String, seq: Long, properties: List<Pair<String, String>>)
    }

    /**
     * Number of discovered devices
     */
    val deviceListSize: Int

    /**
     * List of discovered devices.
     *
     * @see Device
     */
    val deviceList: List<Device>

    /**
     * Do initialize.
     *
     * Must be call before use.
     *
     * Once initialized, reinitialization is impossible.
     * If reinitialization is required, such as changing the interface, start over from creating an instance.
     * Also, be sure to call [terminate] when ending.
     *
     * @see terminate
     */
    fun initialize()

    /**
     * Do terminate.
     *
     * If it is in operation, stop processing is performed.
     * The instance can not be used after terminate.
     *
     * @see stop
     * @see initialize
     */
    fun terminate()

    /**
     * Start the control point.
     *
     * It is not possible to execute network related processing before calling this method.
     * Nothing is done if it is already started.
     * Even after it has been started once, it can be started again if it is after stop.
     *
     * @see initialize
     */
    fun start()

    /**
     * Stop the control point.
     *
     * Nothing happens if it is not started or already stopped.
     * The discovered Device is treated as Lost.
     * After stopping, even if you holds an instance of discovered Device, it does not operate normally.
     *
     * @see start
     */
    fun stop()

    /**
     * Clear the discovered device list.
     *
     * Devices held at the time of the call are notified as lost.
     */
    fun clearDeviceList()

    /**
     * Send Search packet.
     *
     * When st is null, it works as "ssdp:all".
     *
     * @param st ST field of Search packet
     */
    fun search(st: String? = null)

    /**
     * Set a filter to judge whether to accept or ignore SsdpMessage.
     *
     * @param predicate filter: return true to accept, false to ignore. if null, accept all.
     */
    fun setSsdpMessageFilter(predicate: ((SsdpMessage) -> Boolean)?)

    /**
     * Set a filter to select Icon to download.
     *
     * @param filter the filter to be set, null will not download anything.
     * @see IconFilter
     */
    fun setIconFilter(filter: IconFilter?)

    /**
     * Add a listener for device discovery.
     *
     * Listener notification is performed in the callback thread.
     *
     * @param listener Listener to add
     * @see DiscoveryListener
     * @see ControlPointFactory.create
     */
    fun addDiscoveryListener(listener: DiscoveryListener)

    /**
     * Remove a listener for device discovery.
     *
     * @param listener Listener to remove
     * @see DiscoveryListener
     */
    fun removeDiscoveryListener(listener: DiscoveryListener)

    /**
     * Add a NotifyEvent listener.
     *
     * Listener notification is performed in the callback thread.
     *
     * @param listener Listener to add
     * @see NotifyEventListener
     * @see ControlPointFactory.create
     */
    @Deprecated("use addEventListener instead")
    fun addNotifyEventListener(listener: NotifyEventListener)

    /**
     * Remove a NotifyEvent listener.
     *
     * @param listener Listener to remove
     * @see NotifyEventListener
     */
    @Deprecated("use removeEventListener instead")
    fun removeNotifyEventListener(listener: NotifyEventListener)

    /**
     * Add a EventListener.
     *
     * Listener notification is performed in the callback thread.
     *
     * @param listener Listener to add
     * @see EventListener
     */
    fun addEventListener(listener: EventListener)

    /**
     * Remove a EventListener.
     *
     * @param listener Listener to remove
     * @see EventListener
     */
    fun removeEventListener(listener: EventListener)

    /**
     * Add a EventListener.
     *
     * Listener notification is performed in the callback thread.
     *
     * @param listener Listener to add
     * @see MulticastEventListener
     */
    fun addMulticastEventListener(listener: MulticastEventListener)

    /**
     * Remove a EventListener.
     *
     * @param listener Listener to remove
     * @see MulticastEventListener
     */
    fun removeMulticastEventListener(listener: MulticastEventListener)

    /**
     * Find Device by UDN.
     *
     * If not found null will be returned.
     *
     * @param udn UDN
     * @return Device
     * @see Device
     */
    fun getDevice(udn: String): Device?

    /**
     * Try to add a device.
     *
     * This is used to read the cached information based on it.
     *
     * @param uuid UDN
     * @param location location
     */
    fun tryAddDevice(uuid: String, location: String)

    /**
     * Try to add the pinned device
     *
     * After devices can be added, they will not be deleted by time lapse or ByeBye.
     *
     * @param location URL of location. It needs to be an accurate value.
     */
    fun tryAddPinnedDevice(location: String)

    /**
     * Remove the pinned device
     *
     * Even if there is a device with the same location, it will not be deleted if it is not a pinned device.
     *
     * @param location locationのURL
     */
    fun removePinnedDevice(location: String)
}
