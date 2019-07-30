/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.log.Logger
import net.mm2d.upnp.*
import net.mm2d.upnp.Adapter.iconFilter
import net.mm2d.upnp.ControlPoint.DiscoveryListener
import net.mm2d.upnp.ControlPoint.NotifyEventListener
import net.mm2d.upnp.internal.impl.DeviceImpl.Builder
import net.mm2d.upnp.internal.manager.DeviceHolder
import net.mm2d.upnp.internal.manager.SubscribeManager
import net.mm2d.upnp.internal.message.FakeSsdpMessage
import net.mm2d.upnp.internal.parser.DeviceParser
import net.mm2d.upnp.internal.server.DEFAULT_SSDP_MESSAGE_FILTER
import net.mm2d.upnp.internal.server.SsdpNotifyReceiverList
import net.mm2d.upnp.internal.server.SsdpSearchServerList
import net.mm2d.upnp.internal.thread.TaskExecutors
import net.mm2d.upnp.internal.util.toSimpleTrace
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Implements for [ControlPoint].
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class ControlPointImpl(
    private val protocol: Protocol,
    interfaces: Iterable<NetworkInterface>,
    notifySegmentCheckEnabled: Boolean,
    subscriptionEnabled: Boolean,
    factory: DiFactory
) : ControlPoint {
    private var iconFilter: IconFilter = EMPTY_FILTER
    private val discoveryListenerList: MutableSet<DiscoveryListener>
    private val notifyEventListenerList: MutableSet<NotifyEventListener>
    private val searchServerList: SsdpSearchServerList
    private val notifyReceiverList: SsdpNotifyReceiverList
    private val deviceMap: MutableMap<String, Device>
    private val loadingDeviceMap: MutableMap<String, Builder>
    private val initialized = AtomicBoolean()
    private val started = AtomicBoolean()
    private val deviceHolder: DeviceHolder
    private val loadingPinnedDevices: MutableList<Builder>
    internal val subscribeManager: SubscribeManager
    internal val taskExecutors: TaskExecutors

    init {
        if (interfaces.none()) {
            throw IllegalStateException("no valid network interface.")
        }
        discoveryListenerList = CopyOnWriteArraySet()
        notifyEventListenerList = CopyOnWriteArraySet()
        deviceMap = mutableMapOf()
        loadingPinnedDevices = Collections.synchronizedList(mutableListOf())
        taskExecutors = factory.createTaskExecutors()
        loadingDeviceMap = factory.createLoadingDeviceMap()
        searchServerList = factory.createSsdpSearchServerList(taskExecutors, interfaces) { message ->
            taskExecutors.io { onReceiveSsdpMessage(message) }
        }
        notifyReceiverList = factory.createSsdpNotifyReceiverList(taskExecutors, interfaces) { message ->
            taskExecutors.io { onReceiveSsdpMessage(message) }
        }
        notifyReceiverList.setSegmentCheckEnabled(notifySegmentCheckEnabled)
        deviceHolder = factory.createDeviceHolder(taskExecutors) { lostDevice(it) }
        subscribeManager = factory.createSubscribeManager(subscriptionEnabled, taskExecutors, notifyEventListenerList)
    }

    // VisibleForTesting
    internal fun createHttpClient(): HttpClient = HttpClient(true)

    // VisibleForTesting
    internal fun needToUpdateSsdpMessage(oldMessage: SsdpMessage, newMessage: SsdpMessage): Boolean {
        val newAddress = newMessage.localAddress
        if (protocol === Protocol.IP_V4_ONLY) {
            return newAddress is Inet4Address
        }
        if (protocol === Protocol.IP_V6_ONLY) {
            return newAddress is Inet6Address
        }
        val oldAddress = oldMessage.localAddress
        return if (oldAddress is Inet4Address) {
            if (oldAddress.isLinkLocalAddress) true else newAddress is Inet4Address
        } else {
            if (newAddress is Inet6Address) true else newAddress?.isLinkLocalAddress == false
        }
    }

    // VisibleForTesting
    internal fun onReceiveSsdpMessage(message: SsdpMessage) {
        synchronized(deviceHolder) {
            val uuid = message.uuid
            val device = deviceMap[uuid]
            if (device == null) {
                onReceiveNewSsdp(message)
                return
            }
            if (message.nts == SsdpMessage.SSDP_BYEBYE) {
                if (!isPinnedDevice(device)) {
                    lostDevice(device)
                }
            } else {
                if (needToUpdateSsdpMessage(device.ssdpMessage, message)) {
                    device.updateSsdpMessage(message)
                }
            }
        }
    }

    private fun onReceiveNewSsdp(message: SsdpMessage) {
        val uuid = message.uuid
        if (message.nts == SsdpMessage.SSDP_BYEBYE) {
            loadingDeviceMap.remove(uuid)
            return
        }
        loadingDeviceMap[uuid]?.let {
            if (needToUpdateSsdpMessage(it.getSsdpMessage(), message)) {
                it.updateSsdpMessage(message)
            }
            return
        }
        loadDevice(uuid, Builder(this, message))
    }

    // VisibleForTesting
    internal fun loadDevice(uuid: String, builder: Builder) {
        loadingDeviceMap[uuid] = builder
        if (!taskExecutors.io { loadDevice(builder) }) {
            loadingDeviceMap.remove(uuid)
        }
    }

    private fun loadDevice(builder: Builder) {
        val client = createHttpClient()
        val uuid = builder.getUuid()
        try {
            DeviceParser.loadDescription(client, builder)
            val device = builder.build()
            device.loadIconBinary(client, iconFilter)
            synchronized(deviceHolder) {
                if (loadingDeviceMap.remove(uuid) != null) {
                    discoverDevice(device)
                }
            }
        } catch (e: Exception) {
            Logger.w { "loadDevice: " + e.toSimpleTrace() }
            Logger.i(e) { builder.toDumpString() }
            synchronized(deviceHolder) {
                loadingDeviceMap.remove(uuid)
            }
        } finally {
            client.close()
        }
    }

    override val deviceListSize: Int
        get() = deviceHolder.size

    override val deviceList: List<Device>
        get() = deviceHolder.deviceList

    override fun initialize() {
        if (initialized.getAndSet(true)) {
            return
        }
        deviceHolder.start()
        subscribeManager.initialize()
    }

    override fun terminate() {
        if (started.get()) {
            stop()
        }
        if (!initialized.getAndSet(false)) {
            return
        }
        taskExecutors.terminate()
        subscribeManager.terminate()
        deviceHolder.stop()
    }

    override fun start() {
        if (!initialized.get()) {
            initialize()
        }
        if (started.getAndSet(true)) {
            return
        }
        subscribeManager.start()
        searchServerList.start()
        notifyReceiverList.start()
    }

    override fun stop() {
        if (!started.getAndSet(false)) {
            return
        }
        subscribeManager.stop()
        searchServerList.stop()
        notifyReceiverList.stop()
        deviceList.forEach { lostDevice(it) }
        deviceHolder.clear()
    }

    override fun clearDeviceList() {
        synchronized(deviceHolder) {
            deviceList.forEach { lostDevice(it) }
        }
    }

    override fun search(st: String?) {
        if (!started.get()) {
            throw IllegalStateException("ControlPoint is not started.")
        }
        searchServerList.search(st)
    }

    override fun setSsdpMessageFilter(predicate: ((SsdpMessage) -> Boolean)?) {
        val predicateNonNull = predicate ?: DEFAULT_SSDP_MESSAGE_FILTER
        searchServerList.setFilter(predicateNonNull)
        notifyReceiverList.setFilter(predicateNonNull)
    }

    override fun setIconFilter(filter: IconFilter?) {
        iconFilter = filter ?: EMPTY_FILTER
    }

    override fun addDiscoveryListener(listener: DiscoveryListener) {
        discoveryListenerList.add(listener)
    }

    override fun removeDiscoveryListener(listener: DiscoveryListener) {
        discoveryListenerList.remove(listener)
    }

    override fun addNotifyEventListener(listener: NotifyEventListener) {
        notifyEventListenerList.add(listener)
    }

    override fun removeNotifyEventListener(listener: NotifyEventListener) {
        notifyEventListenerList.remove(listener)
    }

    // VisibleForTesting
    internal fun discoverDevice(device: Device) {
        Logger.d { "discoverDevice:[${device.friendlyName}](${device.ipAddress})" }
        if (isPinnedDevice(deviceHolder[device.udn])) {
            return
        }
        deviceHolder.add(device)
        collectUdn(device).forEach { deviceMap[it] = device }
        taskExecutors.callback {
            discoveryListenerList.forEach { it.onDiscover(device) }
        }
    }

    // VisibleForTesting
    internal fun lostDevice(device: Device) {
        Logger.d { "lostDevice:[${device.friendlyName}](${device.ipAddress})" }
        synchronized(deviceHolder) {
            device.serviceList.forEach { subscribeManager.unregister(it) }
            collectUdn(device).forEach { deviceMap.remove(it) }
            deviceHolder.remove(device)
        }
        taskExecutors.callback {
            discoveryListenerList.forEach { it.onLost(device) }
        }
    }

    override fun getDevice(udn: String): Device? = deviceMap[udn]

    override fun tryAddDevice(uuid: String, location: String) {
        if (deviceList.any { it.location == location }) {
            Logger.i { "already added: $location" }
            return
        }
        if (loadingDeviceMap.values.any { it.getLocation() == location }) {
            Logger.i { "already loading: $location" }
            return
        }
        val message = FakeSsdpMessage(location, uuid, false)
        loadDevice(uuid, Builder(this, message))
    }

    override fun tryAddPinnedDevice(location: String) {
        if (deviceList.any { it.location == location && isPinnedDevice(it) }) {
            Logger.i { "already added: $location" }
            return
        }
        val builder = Builder(this, FakeSsdpMessage(location))
        loadingPinnedDevices.add(builder)
        taskExecutors.io { loadPinnedDevice(builder) }
    }

    private fun loadPinnedDevice(builder: Builder) {
        val client = createHttpClient()
        try {
            DeviceParser.loadDescription(client, builder)
            val device = builder.build()
            device.loadIconBinary(client, iconFilter)
            synchronized(deviceHolder) {
                if (!loadingPinnedDevices.remove(builder)) {
                    return
                }
                val udn = device.udn
                loadingDeviceMap.remove(udn)
                deviceHolder.remove(udn)?.let { lostDevice(it) }
                discoverDevice(device)
            }
        } catch (e: Exception) {
            Logger.w(e) { "fail to load:\n${builder.getLocation()}" }
        } finally {
            client.close()
        }
    }

    override fun removePinnedDevice(location: String) {
        synchronized(loadingPinnedDevices) {
            val i = loadingPinnedDevices.listIterator()
            while (i.hasNext()) {
                if (i.next().getLocation() == location) {
                    i.remove()
                    return
                }
            }
        }
        deviceList.find { it.location == location }
            ?.let { lostDevice(it) }
    }

    companion object {
        private val EMPTY_FILTER = iconFilter { emptyList() }

        // VisibleForTesting
        internal fun collectUdn(device: Device): Set<String> = mutableSetOf<String>().also {
            collectUdn(device, it)
        }

        private fun collectUdn(device: Device, outSet: MutableSet<String>) {
            outSet.add(device.udn)
            device.deviceList.forEach { collectUdn(it, outSet) }
        }

        private fun isPinnedDevice(device: Device?): Boolean {
            return device?.isPinned == true
        }
    }
}
