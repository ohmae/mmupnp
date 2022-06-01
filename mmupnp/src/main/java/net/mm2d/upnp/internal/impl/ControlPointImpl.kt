/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import net.mm2d.upnp.Adapter.iconFilter
import net.mm2d.upnp.ControlPoint
import net.mm2d.upnp.ControlPoint.DiscoveryListener
import net.mm2d.upnp.ControlPoint.EventListener
import net.mm2d.upnp.ControlPoint.MulticastEventListener
import net.mm2d.upnp.ControlPoint.NotifyEventListener
import net.mm2d.upnp.Device
import net.mm2d.upnp.IconFilter
import net.mm2d.upnp.Protocol
import net.mm2d.upnp.Service
import net.mm2d.upnp.SingleHttpClient
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.internal.impl.DeviceImpl.Builder
import net.mm2d.upnp.internal.manager.DeviceHolder
import net.mm2d.upnp.internal.manager.SubscribeManager
import net.mm2d.upnp.internal.message.FakeSsdpMessage
import net.mm2d.upnp.internal.parser.DeviceParser
import net.mm2d.upnp.internal.server.DEFAULT_SSDP_MESSAGE_FILTER
import net.mm2d.upnp.internal.server.MulticastEventReceiverList
import net.mm2d.upnp.internal.server.SsdpNotifyServerList
import net.mm2d.upnp.internal.server.SsdpSearchServerList
import net.mm2d.upnp.internal.thread.TaskExecutors
import net.mm2d.upnp.internal.util.toSimpleTrace
import net.mm2d.upnp.log.Logger
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
    multicastEventingEnabled: Boolean,
    factory: DiFactory
) : ControlPoint {
    private var iconFilter: IconFilter = EMPTY_FILTER
    private val discoveryListenerSet: MutableSet<DiscoveryListener>

    @Suppress("DEPRECATION")
    private val notifyEventListenerSet: MutableSet<NotifyEventListener>
    private val eventListenerSet: MutableSet<EventListener>
    private val multicastEventListenerSet: MutableSet<MulticastEventListener>
    private val searchServerList: SsdpSearchServerList
    private val notifyServerList: SsdpNotifyServerList
    private val deviceMap: MutableMap<String, Device>
    private val loadingDeviceMap: MutableMap<String, Builder>
    private val initialized = AtomicBoolean()
    private val started = AtomicBoolean()
    private val deviceHolder: DeviceHolder
    private val loadingPinnedDevices: MutableList<Builder>
    private val multicastEventReceiverList: MulticastEventReceiverList?
    internal val subscribeManager: SubscribeManager
    internal val taskExecutors: TaskExecutors

    init {
        check(interfaces.any()) { "no valid network interface." }
        discoveryListenerSet = CopyOnWriteArraySet()
        notifyEventListenerSet = CopyOnWriteArraySet()
        eventListenerSet = CopyOnWriteArraySet()
        multicastEventListenerSet = CopyOnWriteArraySet()
        deviceMap = mutableMapOf()
        loadingPinnedDevices = Collections.synchronizedList(mutableListOf())
        taskExecutors = factory.createTaskExecutors()
        loadingDeviceMap = factory.createLoadingDeviceMap()
        searchServerList = factory.createSsdpSearchServerList(taskExecutors, interfaces) { message ->
            taskExecutors.io { onReceiveSsdpMessage(message) }
        }
        notifyServerList = factory.createSsdpNotifyServerList(taskExecutors, interfaces) { message ->
            taskExecutors.io { onReceiveSsdpMessage(message) }
        }
        notifyServerList.setSegmentCheckEnabled(notifySegmentCheckEnabled)
        deviceHolder = factory.createDeviceHolder(taskExecutors) { lostDevice(it) }
        subscribeManager = factory.createSubscribeManager(subscriptionEnabled, taskExecutors, ::onReceiveEvent)
        multicastEventReceiverList = if (multicastEventingEnabled) {
            factory.createMulticastEventReceiverList(taskExecutors, interfaces, ::onReceiveMulticastEvent)
        } else null
    }

    private fun createHttpClient(): SingleHttpClient = SingleHttpClient.create(true)

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

    private fun onReceiveEvent(service: Service, seq: Long, properties: List<Pair<String, String>>) {
        eventListenerSet.forEach {
            taskExecutors.callback { it.onEvent(service, seq, properties) }
        }
        if (notifyEventListenerSet.isEmpty()) return
        properties.forEach {
            notifyEvent(service, seq, it.first, it.second)
        }
    }

    private fun notifyEvent(service: Service, seq: Long, name: String?, value: String?) {
        val variable = service.findStateVariable(name)
        if (variable?.isSendEvents != true || value == null) {
            Logger.w { "illegal notify argument: $name $value" }
            return
        }
        notifyEventListenerSet.forEach {
            taskExecutors.callback { it.onNotifyEvent(service, seq, variable.name, value) }
        }
    }

    // VisibleForTesting
    internal fun onReceiveMulticastEvent(
        uuid: String,
        svcid: String,
        lvl: String,
        seq: Long,
        properties: List<Pair<String, String>>
    ) {
        val service = synchronized(deviceHolder) {
            deviceMap[uuid]?.findServiceById(svcid)
        } ?: return
        multicastEventListenerSet.forEach {
            taskExecutors.callback { it.onEvent(service, lvl, seq, properties) }
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
        multicastEventReceiverList?.start()
        subscribeManager.start()
        searchServerList.start()
        notifyServerList.start()
    }

    override fun stop() {
        if (!started.getAndSet(false)) {
            return
        }
        multicastEventReceiverList?.stop()
        subscribeManager.stop()
        searchServerList.stop()
        notifyServerList.stop()
        deviceList.forEach { lostDevice(it) }
        deviceHolder.clear()
    }

    override fun clearDeviceList() {
        synchronized(deviceHolder) {
            deviceList.forEach { lostDevice(it) }
        }
    }

    override fun search(st: String?) {
        check(started.get()) { "ControlPoint is not started." }
        searchServerList.search(st)
    }

    override fun setSsdpMessageFilter(predicate: ((SsdpMessage) -> Boolean)?) {
        val predicateNonNull = predicate ?: DEFAULT_SSDP_MESSAGE_FILTER
        searchServerList.setFilter(predicateNonNull)
        notifyServerList.setFilter(predicateNonNull)
    }

    override fun setIconFilter(filter: IconFilter?) {
        iconFilter = filter ?: EMPTY_FILTER
    }

    override fun addDiscoveryListener(listener: DiscoveryListener) {
        discoveryListenerSet.add(listener)
    }

    override fun removeDiscoveryListener(listener: DiscoveryListener) {
        discoveryListenerSet.remove(listener)
    }

    @Suppress("DEPRECATION")
    override fun addNotifyEventListener(listener: NotifyEventListener) {
        notifyEventListenerSet.add(listener)
    }

    @Suppress("DEPRECATION")
    override fun removeNotifyEventListener(listener: NotifyEventListener) {
        notifyEventListenerSet.remove(listener)
    }

    override fun addEventListener(listener: EventListener) {
        eventListenerSet.add(listener)
    }

    override fun removeEventListener(listener: EventListener) {
        eventListenerSet.remove(listener)
    }

    override fun addMulticastEventListener(listener: MulticastEventListener) {
        multicastEventListenerSet.add(listener)
    }

    override fun removeMulticastEventListener(listener: MulticastEventListener) {
        multicastEventListenerSet.remove(listener)
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
            discoveryListenerSet.forEach { it.onDiscover(device) }
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
            discoveryListenerSet.forEach { it.onLost(device) }
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
