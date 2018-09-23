/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.log.Log;
import net.mm2d.util.TextUtils;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

/**
 * ControlPointの実装。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
class ControlPointImpl implements ControlPoint {
    @Nonnull
    private final Protocol mProtocol;
    @Nonnull
    private SsdpMessageFilter mSsdpMessageFilter = SsdpMessageFilter.DEFAULT;
    @Nonnull
    private IconFilter mIconFilter = IconFilter.NONE;
    @Nonnull
    private final DiscoveryListenerList mDiscoveryListenerList;
    @Nonnull
    private final NotifyEventListenerList mNotifyEventListenerList;
    @Nonnull
    private final SsdpSearchServerList mSearchServerList;
    @Nonnull
    private final SsdpNotifyReceiverList mNotifyReceiverList;
    @Nonnull
    private final Map<String, DeviceImpl.Builder> mLoadingDeviceMap;
    @Nonnull
    private final Set<String> mEmbeddedUdnSet = new HashSet<>();
    @Nonnull
    private final ThreadPool mThreadPool;
    @Nonnull
    private final AtomicBoolean mInitialized = new AtomicBoolean();
    @Nonnull
    private final AtomicBoolean mStarted = new AtomicBoolean();
    @Nonnull
    private final DeviceHolder mDeviceHolder;
    @Nonnull
    private final SubscribeManager mSubscribeManager;
    @Nonnull
    private final List<DeviceImpl.Builder> mLoadingPinnedDevices = Collections.synchronizedList(new ArrayList<>());

    ControlPointImpl(
            @Nonnull final Protocol protocol,
            @Nonnull final Collection<NetworkInterface> interfaces,
            final boolean notifySegmentCheckEnabled,
            @Nonnull final DiFactory factory) {
        if (interfaces.isEmpty()) {
            throw new IllegalStateException("no valid network interface.");
        }
        mProtocol = protocol;
        mThreadPool = new ThreadPool();
        mLoadingDeviceMap = factory.createLoadingDeviceMap();
        mDiscoveryListenerList = new DiscoveryListenerList();
        mNotifyEventListenerList = new NotifyEventListenerList();

        mSearchServerList = factory.createSsdpSearchServerList(interfaces, message ->
                mThreadPool.executeInParallel(() -> onReceiveSsdpMessage(message)));
        mNotifyReceiverList = factory.createSsdpNotifyReceiverList(interfaces, message ->
                mThreadPool.executeInParallel(() -> onReceiveSsdpMessage(message)));
        mNotifyReceiverList.setSegmentCheckEnabled(notifySegmentCheckEnabled);
        mDeviceHolder = factory.createDeviceHolder(this::lostDevice);
        mSubscribeManager = factory.createSubscribeManager(mThreadPool, mNotifyEventListenerList);
    }

    // VisibleForTesting
    @SuppressWarnings("WeakerAccess")
    @Nonnull
    HttpClient createHttpClient() {
        return new HttpClient(true);
    }

    // VisibleForTesting
    @SuppressWarnings("WeakerAccess")
    boolean needToUpdateSsdpMessage(
            @Nonnull final SsdpMessage oldMessage,
            @Nonnull final SsdpMessage newMessage) {
        final InetAddress newAddress = newMessage.getLocalAddress();
        if (mProtocol == Protocol.IP_V4_ONLY) {
            return newAddress instanceof Inet4Address;
        }
        if (mProtocol == Protocol.IP_V6_ONLY) {
            return newAddress instanceof Inet6Address;
        }
        final InetAddress oldAddress = oldMessage.getLocalAddress();
        if (oldAddress instanceof Inet4Address) {
            if (oldAddress.isLinkLocalAddress()) {
                return true;
            }
            return !(newAddress instanceof Inet6Address);
        } else {
            if (newAddress instanceof Inet6Address) {
                return true;
            }
            return newAddress != null && !newAddress.isLinkLocalAddress();
        }
    }

    // VisibleForTesting
    @SuppressWarnings("WeakerAccess")
    void onReceiveSsdpMessage(@Nonnull final SsdpMessage message) {
        if (mSsdpMessageFilter.accept(message)) {
            onAcceptSsdpMessage(message);
        }
    }

    // VisibleForTesting
    @SuppressWarnings("WeakerAccess")
    void onAcceptSsdpMessage(@Nonnull final SsdpMessage message) {
        synchronized (mDeviceHolder) {
            final String uuid = message.getUuid();
            final Device device = mDeviceHolder.get(uuid);
            if (device == null) {
                if (mEmbeddedUdnSet.contains(uuid)) {
                    return;
                }
                onReceiveNewSsdp(message);
                return;
            }
            if (TextUtils.equals(message.getNts(), SsdpMessage.SSDP_BYEBYE)) {
                if (!isPinnedDevice(device)) {
                    lostDevice(device);
                }
            } else {
                if (needToUpdateSsdpMessage(device.getSsdpMessage(), message)) {
                    device.updateSsdpMessage(message);
                }
            }
        }
    }

    private void onReceiveNewSsdp(@Nonnull final SsdpMessage message) {
        final String uuid = message.getUuid();
        if (TextUtils.equals(message.getNts(), SsdpMessage.SSDP_BYEBYE)) {
            mLoadingDeviceMap.remove(uuid);
            return;
        }
        final DeviceImpl.Builder deviceBuilder = mLoadingDeviceMap.get(uuid);
        if (deviceBuilder != null) {
            if (needToUpdateSsdpMessage(deviceBuilder.getSsdpMessage(), message)) {
                deviceBuilder.updateSsdpMessage(message);
            }
            return;
        }
        final DeviceImpl.Builder builder = new DeviceImpl.Builder(this, mSubscribeManager, message);
        mLoadingDeviceMap.put(uuid, builder);
        if (!mThreadPool.executeInParallel(() -> loadDevice(builder))) {
            mLoadingDeviceMap.remove(uuid);
        }
    }

    private void loadDevice(@Nonnull final DeviceImpl.Builder builder) {
        final HttpClient client = createHttpClient();
        final String uuid = builder.getUuid();
        try {
            DeviceParser.loadDescription(client, builder);
            final Device device = builder.build();
            device.loadIconBinary(client, mIconFilter);
            synchronized (mDeviceHolder) {
                if (mLoadingDeviceMap.remove(uuid) != null) {
                    discoverDevice(device);
                }
            }
        } catch (final IOException | SAXException | ParserConfigurationException | RuntimeException e) {
            Log.d(e.getClass().getSimpleName() + " occurred on loadDevice\n" + builder.toDumpString());
            Log.w(e);
            synchronized (mDeviceHolder) {
                mLoadingDeviceMap.remove(uuid);
            }
        } finally {
            client.close();
        }
    }

    @Override
    public void initialize() {
        if (mInitialized.getAndSet(true)) {
            return;
        }
        mDeviceHolder.start();
        mSubscribeManager.initialize();
    }

    @Override
    public void terminate() {
        if (mStarted.get()) {
            stop();
        }
        if (!mInitialized.getAndSet(false)) {
            return;
        }
        mThreadPool.terminate();
        mSubscribeManager.terminate();
        mDeviceHolder.shutdownRequest();
    }

    @Override
    public void start() {
        if (!mInitialized.get()) {
            initialize();
        }
        if (mStarted.getAndSet(true)) {
            return;
        }
        mSubscribeManager.start();
        mSearchServerList.openAndStart();
        mNotifyReceiverList.openAndStart();
    }

    @Override
    public void stop() {
        if (!mStarted.getAndSet(false)) {
            return;
        }
        mSubscribeManager.stop();
        mSearchServerList.stop();
        mNotifyReceiverList.stop();
        mSearchServerList.close();
        mNotifyReceiverList.close();
        final List<Device> list = getDeviceList();
        for (final Device device : list) {
            lostDevice(device);
        }
        mDeviceHolder.clear();
    }

    @Override
    public void clearDeviceList() {
        synchronized (mDeviceHolder) {
            final List<Device> list = getDeviceList();
            for (final Device device : list) {
                lostDevice(device);
            }
        }
    }

    @Override
    public void search() {
        search(null);
    }

    @Override
    public void search(@Nullable final String st) {
        if (!mStarted.get()) {
            throw new IllegalStateException("ControlPoint is not started.");
        }
        mSearchServerList.search(st);
    }

    @Override
    public void setSsdpMessageFilter(@Nullable final SsdpMessageFilter filter) {
        mSsdpMessageFilter = filter != null ? filter : SsdpMessageFilter.DEFAULT;
    }

    @Override
    public void setIconFilter(@Nullable final IconFilter filter) {
        mIconFilter = filter != null ? filter : IconFilter.NONE;
    }

    @Override
    public void addDiscoveryListener(@Nonnull final DiscoveryListener listener) {
        mDiscoveryListenerList.add(listener);
    }

    @Override
    public void removeDiscoveryListener(@Nonnull final DiscoveryListener listener) {
        mDiscoveryListenerList.remove(listener);
    }

    @Override
    public void addNotifyEventListener(@Nonnull final NotifyEventListener listener) {
        mNotifyEventListenerList.add(listener);
    }

    @Override
    public void removeNotifyEventListener(@Nonnull final NotifyEventListener listener) {
        mNotifyEventListenerList.remove(listener);
    }

    // VisibleForTesting
    @SuppressWarnings("WeakerAccess")
    void discoverDevice(@Nonnull final Device device) {
        if (isPinnedDevice(mDeviceHolder.get(device.getUdn()))) {
            return;
        }
        mEmbeddedUdnSet.addAll(collectEmbeddedUdn(device));
        mDeviceHolder.add(device);
        mThreadPool.executeInSequential(() ->
                mDiscoveryListenerList.onDiscover(device));
    }

    @SuppressWarnings("WeakerAccess")
    void lostDevice(@Nonnull final Device device) {
        mEmbeddedUdnSet.removeAll(collectEmbeddedUdn(device));
        synchronized (mDeviceHolder) {
            final List<Service> list = device.getServiceList();
            for (final Service s : list) {
                mSubscribeManager.unregister(s);
            }
            mDeviceHolder.remove(device);
        }
        mThreadPool.executeInSequential(() ->
                mDiscoveryListenerList.onLost(device));
    }

    // VisibleForTesting
    @SuppressWarnings("WeakerAccess")
    static Set<String> collectEmbeddedUdn(@Nonnull final Device device) {
        final List<Device> deviceList = device.getDeviceList();
        if (deviceList.isEmpty()) {
            return Collections.emptySet();
        }
        final Set<String> outSet = new HashSet<>();
        for (final Device d : deviceList) {
            collectEmbeddedUdn(d, outSet);
        }
        return outSet;
    }

    private static void collectEmbeddedUdn(
            @Nonnull final Device device,
            @Nonnull final Set<String> outSet) {
        outSet.add(device.getUdn());
        for (final Device d : device.getDeviceList()) {
            collectEmbeddedUdn(d, outSet);
        }
    }

    @Override
    public int getDeviceListSize() {
        return mDeviceHolder.size();
    }

    @Override
    @Nonnull
    public List<Device> getDeviceList() {
        return mDeviceHolder.getDeviceList();
    }

    @Override
    @Nullable
    public Device getDevice(@Nonnull final String udn) {
        return mDeviceHolder.get(udn);
    }

    @Override
    public void addPinnedDevice(@Nonnull final String location) {
        for (final Device device : getDeviceList()) {
            if (TextUtils.equals(device.getLocation(), location) && isPinnedDevice(device)) {
                return;
            }
        }
        final DeviceImpl.Builder builder = new DeviceImpl.Builder(
                this, mSubscribeManager, new PinnedSsdpMessage(location));
        mLoadingPinnedDevices.add(builder);
        mThreadPool.executeInParallel(() -> loadPinnedDevice(builder));
    }

    private void loadPinnedDevice(@Nonnull final DeviceImpl.Builder builder) {
        final HttpClient client = createHttpClient();
        try {
            DeviceParser.loadDescription(client, builder);
            final Device device = builder.build();
            device.loadIconBinary(client, mIconFilter);
            synchronized (mDeviceHolder) {
                final String udn = device.getUdn();
                if (!mLoadingPinnedDevices.remove(builder)) {
                    return;
                }
                mLoadingDeviceMap.remove(udn);
                final Device lostDevice = mDeviceHolder.remove(udn);
                if (lostDevice != null) {
                    lostDevice(lostDevice);
                }
                discoverDevice(device);
            }
        } catch (final IOException | IllegalStateException | SAXException | ParserConfigurationException e) {
            Log.w(null, "fail to load:" + builder.getLocation(), e);
        } finally {
            client.close();
        }
    }

    @Override
    public void removePinnedDevice(@Nonnull final String location) {
        synchronized (mLoadingPinnedDevices) {
            for (final ListIterator<DeviceImpl.Builder> i = mLoadingPinnedDevices.listIterator(); i.hasNext(); ) {
                final DeviceImpl.Builder builder = i.next();
                if (TextUtils.equals(builder.getLocation(), location)) {
                    i.remove();
                    return;
                }
            }
        }
        for (final Device device : getDeviceList()) {
            if (TextUtils.equals(device.getLocation(), location)) {
                lostDevice(device);
                return;
            }
        }
    }

    private static boolean isPinnedDevice(@Nullable final Device device) {
        return device != null && device.isPinned();
    }
}
