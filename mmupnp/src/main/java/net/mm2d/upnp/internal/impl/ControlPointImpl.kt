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
import net.mm2d.upnp.internal.server.SsdpNotifyReceiverList
import net.mm2d.upnp.internal.server.SsdpSearchServerList
import net.mm2d.upnp.internal.thread.TaskExecutors
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
    factory: DiFactory
) : ControlPoint {
    private var ssdpMessageFilter: (SsdpMessage) -> Boolean = { true }
    private var iconFilter: IconFilter = EMPTY_FILTER
    private val discoveryListenerList: MutableSet<DiscoveryListener>
    private val notifyEventListenerList: MutableSet<NotifyEventListener>
    private val searchServerList: SsdpSearchServerList
    private val notifyReceiverList: SsdpNotifyReceiverList
    private val loadingDeviceMap: MutableMap<String, Builder>
    private val embeddedUdnSet: MutableSet<String>
    private val initialized = AtomicBoolean()
    private val started = AtomicBoolean()
    private val deviceHolder: DeviceHolder
    private val subscribeManager: SubscribeManager
    private val loadingPinnedDevices: MutableList<Builder>
    internal val taskExecutors: TaskExecutors

    init {
        if (interfaces.none()) {
            throw IllegalStateException("no valid network interface.")
        }
        discoveryListenerList = CopyOnWriteArraySet()
        notifyEventListenerList = CopyOnWriteArraySet()
        embeddedUdnSet = mutableSetOf()
        loadingPinnedDevices = Collections.synchronizedList<Builder>(mutableListOf())
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
        subscribeManager = factory.createSubscribeManager(taskExecutors, notifyEventListenerList)
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
        if (ssdpMessageFilter(message)) {
            onAcceptSsdpMessage(message)
        }
    }

    // VisibleForTesting
    internal fun onAcceptSsdpMessage(message: SsdpMessage) {
        synchronized(deviceHolder) {
            val uuid = message.uuid
            val device = deviceHolder[uuid]
            if (device == null) {
                if (embeddedUdnSet.contains(uuid)) {
                    return
                }
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
        loadDevice(uuid, Builder(this, subscribeManager, message))
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
            Logger.w(e)
            Logger.i { "${e.javaClass.simpleName} occurred on loadDevice\n${builder.toDumpString()}" }
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

    override fun setSsdpMessageFilter(filter: ((SsdpMessage) -> Boolean)?) {
        ssdpMessageFilter = filter ?: { true }
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
        embeddedUdnSet.addAll(collectEmbeddedUdn(device))
        deviceHolder.add(device)
        taskExecutors.callback {
            discoveryListenerList.forEach { it.onDiscover(device) }
        }
    }

    // VisibleForTesting
    internal fun lostDevice(device: Device) {
        Logger.d { "lostDevice:[${device.friendlyName}](${device.ipAddress})" }
        embeddedUdnSet.removeAll(collectEmbeddedUdn(device))
        synchronized(deviceHolder) {
            device.serviceList.forEach { subscribeManager.unregister(it) }
            deviceHolder.remove(device)
        }
        taskExecutors.callback {
            discoveryListenerList.forEach { it.onLost(device) }
        }
    }

    override fun getDevice(udn: String): Device? = deviceHolder[udn]

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
        loadDevice(uuid, Builder(this, subscribeManager, message))
    }

    override fun tryAddPinnedDevice(location: String) {
        if (deviceList.any { it.location == location && isPinnedDevice(it) }) {
            Logger.i { "already added: $location" }
            return
        }
        val builder = Builder(this, subscribeManager, FakeSsdpMessage(location))
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
        internal fun collectEmbeddedUdn(device: Device): Set<String> {
            if (device.deviceList.isEmpty()) {
                return emptySet()
            }
            val outSet = mutableSetOf<String>()
            device.deviceList.forEach { collectEmbeddedUdn(it, outSet) }
            return outSet
        }

        private fun collectEmbeddedUdn(device: Device, outSet: MutableSet<String>) {
            outSet.add(device.udn)
            device.deviceList.forEach { collectEmbeddedUdn(it, outSet) }
        }

        private fun isPinnedDevice(device: Device?): Boolean {
            return device?.isPinned == true
        }
    }
}
