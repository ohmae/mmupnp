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
    private IconFilter mIconFilter = IconFilter.NONE;
    @Nonnull
    private final DiscoveryListenerList mDiscoveryListenerList;
    @Nonnull
    private final NotifyEventListenerList mNotifyEventListenerList;
    @Nonnull
    private final SsdpSearchServerList mSearchList;
    @Nonnull
    private final SsdpNotifyReceiverList mNotifyList;
    @Nonnull
    private final Map<String, DeviceImpl.Builder> mLoadingDeviceMap;
    @Nonnull
    private final Set<String> mAllUdnSet = new HashSet<>();
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

    private class DeviceLoader implements Runnable {
        @Nonnull
        private final DeviceImpl.Builder mDeviceBuilder;

        DeviceLoader(@Nonnull final DeviceImpl.Builder builder) {
            mDeviceBuilder = builder;
        }

        @Override
        public void run() {
            final HttpClient client = createHttpClient();
            final String uuid = mDeviceBuilder.getUuid();
            try {
                DeviceParser.loadDescription(client, mDeviceBuilder);
                final Device device = mDeviceBuilder.build();
                device.loadIconBinary(client, mIconFilter);
                synchronized (mDeviceHolder) {
                    if (mLoadingDeviceMap.remove(uuid) != null) {
                        discoverDevice(device);
                    }
                }
            } catch (final IOException | IllegalStateException | SAXException | ParserConfigurationException e) {
                Log.w(e);
                synchronized (mDeviceHolder) {
                    mLoadingDeviceMap.remove(uuid);
                }
            } finally {
                client.close();
            }
        }
    }

    private class PinnedDeviceLoader implements Runnable {
        @Nonnull
        private final DeviceImpl.Builder mDeviceBuilder;

        PinnedDeviceLoader(@Nonnull final DeviceImpl.Builder builder) {
            mDeviceBuilder = builder;
        }

        @Override
        public void run() {
            final HttpClient client = createHttpClient();
            try {
                DeviceParser.loadDescription(client, mDeviceBuilder);
                final Device device = mDeviceBuilder.build();
                device.loadIconBinary(client, mIconFilter);
                synchronized (mDeviceHolder) {
                    final String udn = device.getUdn();
                    if (!mLoadingPinnedDevices.remove(mDeviceBuilder)) {
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
                Log.w(null, "fail to load:" + mDeviceBuilder.getLocation(), e);
            } finally {
                client.close();
            }
        }
    }

    ControlPointImpl(
            @Nonnull final Protocol protocol,
            @Nonnull final Collection<NetworkInterface> interfaces,
            @Nonnull final DiFactory factory) {
        if (interfaces.isEmpty()) {
            throw new IllegalStateException("no valid network interface.");
        }
        mProtocol = protocol;
        mThreadPool = new ThreadPool();
        mLoadingDeviceMap = factory.createLoadingDeviceMap();
        mDiscoveryListenerList = new DiscoveryListenerList();
        mNotifyEventListenerList = new NotifyEventListenerList();

        mSearchList = factory.createSsdpSearchServerList(interfaces, message ->
                mThreadPool.executeInParallel(() -> onReceiveSsdp(message)));
        mNotifyList = factory.createSsdpNotifyReceiverList(interfaces, message ->
                mThreadPool.executeInParallel(() -> onReceiveSsdp(message)));
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
    void onReceiveSsdp(@Nonnull final SsdpMessage message) {
        synchronized (mDeviceHolder) {
            final String uuid = message.getUuid();
            final Device device = mDeviceHolder.get(uuid);
            if (device == null) {
                if (mAllUdnSet.contains(uuid)) {
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
        if (!mThreadPool.executeInParallel(new DeviceLoader(builder))) {
            mLoadingDeviceMap.remove(uuid);
        }
    }

    /**
     * 初期化を行う。
     *
     * <p>利用前にかならず実行する。
     * 一度初期化を行うと再初期化は不可能。
     * インターフェースの変更など、再初期化が必要な場合はインスタンスの生成からやり直すこと。
     * また、終了する際は必ず{@link #terminate()}をコールすること。
     *
     * @see #initialize()
     */
    @Override
    public void initialize() {
        if (mInitialized.getAndSet(true)) {
            return;
        }
        mDeviceHolder.start();
        mSubscribeManager.initialize();
    }

    /**
     * 終了処理を行う。
     *
     * <p>動作中の場合、停止処理を行う。
     * 一度終了処理を行ったあとは再初期化は不可能。
     * インスタンス参照を破棄すること。
     *
     * @see #stop()
     * @see #initialize()
     */
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

    /**
     * 処理を開始する。
     *
     * <p>本メソッドのコール前はネットワークに関連する処理を実行することはできない。
     * 既に開始状態の場合は何も行われない。
     * 一度開始したあとであっても、停止処理後であれば再度開始可能。
     *
     * @see #initialize()
     */
    @Override
    public void start() {
        if (!mInitialized.get()) {
            initialize();
        }
        if (mStarted.getAndSet(true)) {
            return;
        }
        mSubscribeManager.start();
        mSearchList.openAndStart();
        mNotifyList.openAndStart();
    }

    /**
     * 処理を停止する。
     *
     * <p>開始していない状態、既に停止済みの状態の場合なにも行われない。
     * 停止に伴い発見済みDeviceはLost扱いとなる。
     * 停止後は発見済みDeviceのインスタンスを保持していても正常に動作しない。
     *
     * @see #start()
     */
    @Override
    public void stop() {
        if (!mStarted.getAndSet(false)) {
            return;
        }
        mSubscribeManager.stop();
        mSearchList.stop();
        mNotifyList.stop();
        mSearchList.close();
        mNotifyList.close();
        final List<Device> list = getDeviceList();
        for (final Device device : list) {
            lostDevice(device);
        }
        mDeviceHolder.clear();
    }

    /**
     * 保持している発見済みのデバイスリストをクリアする。
     *
     * <p>コール時点で保持されているデバイスはlost扱いとして通知される。
     */
    @Override
    public void clearDeviceList() {
        synchronized (mDeviceHolder) {
            final List<Device> list = getDeviceList();
            for (final Device device : list) {
                lostDevice(device);
            }
        }
    }

    /**
     * Searchパケットを送出する。
     *
     * <p>{@link #search(String)}を引数nullでコールするのと等価。
     */
    @Override
    public void search() {
        search(null);
    }

    /**
     * Searchパケットを送出する。
     *
     * <p>stがnullの場合、"ssdp:all"として動作する。
     *
     * @param st SearchパケットのSTフィールド
     */
    @Override
    public void search(@Nullable final String st) {
        if (!mStarted.get()) {
            throw new IllegalStateException("ControlPoint is not started.");
        }
        mSearchList.search(st);
    }

    /**
     * ダウンロードするIconを選択するフィルタを設定する。
     *
     * @param filter 設定するフィルタ、nullは指定できない。
     * @see IconFilter#NONE
     * @see IconFilter#ALL
     */
    @Override
    public void setIconFilter(@Nonnull final IconFilter filter) {
        mIconFilter = filter;
    }

    /**
     * 機器発見のリスナーを登録する。
     *
     * @param listener リスナー
     * @see DiscoveryListener
     */
    @Override
    public void addDiscoveryListener(@Nonnull final DiscoveryListener listener) {
        mDiscoveryListenerList.add(listener);
    }

    /**
     * 機器発見リスナーを削除する。
     *
     * @param listener リスナー
     * @see DiscoveryListener
     */
    @Override
    public void removeDiscoveryListener(@Nonnull final DiscoveryListener listener) {
        mDiscoveryListenerList.remove(listener);
    }

    /**
     * NotifyEvent受信リスナーを登録する。
     *
     * @param listener リスナー
     * @see NotifyEventListener
     */
    @Override
    public void addNotifyEventListener(@Nonnull final NotifyEventListener listener) {
        mNotifyEventListenerList.add(listener);
    }

    /**
     * NotifyEvent受信リスナーを削除する。
     *
     * @param listener リスナー
     * @see NotifyEventListener
     */
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
        mAllUdnSet.addAll(device.getAllUdnSet());
        mDeviceHolder.add(device);
        mThreadPool.executeInSequential(() ->
                mDiscoveryListenerList.onDiscover(device));
    }

    /**
     * デバイスの喪失を行う。
     *
     * <p>DeviceInspectorからコールするためにパッケージデフォルトとする
     * 他からはコールしないこと。
     *
     * @param device 喪失してデバイス
     * @see Device
     * @see DeviceHolder
     */
    @SuppressWarnings("WeakerAccess")
    void lostDevice(@Nonnull final Device device) {
        mAllUdnSet.removeAll(device.getAllUdnSet());
        synchronized (mDeviceHolder) {
            final List<Service> list = device.getServiceList();
            for (final Service s : list) {
                mSubscribeManager.unregisterSubscribeService(s);
            }
            mDeviceHolder.remove(device);
        }
        mThreadPool.executeInSequential(() ->
                mDiscoveryListenerList.onLost(device));
    }

    /**
     * 発見したデバイスの数を返す。
     *
     * @return デバイスの数
     */
    @Override
    public int getDeviceListSize() {
        return mDeviceHolder.size();
    }

    /**
     * 発見したデバイスのリストを返す。
     *
     * <p>内部で保持するリストのコピーが返される。
     *
     * @return デバイスのリスト
     * @see Device
     */
    @Override
    @Nonnull
    public List<Device> getDeviceList() {
        return mDeviceHolder.getDeviceList();
    }

    /**
     * 指定UDNのデバイスを返す。
     *
     * <p>見つからない場合nullが返る。
     *
     * @param udn UDN
     * @return 指定UDNのデバイス
     * @see Device
     */
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
        mThreadPool.executeInParallel(new PinnedDeviceLoader(builder));
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
        return device != null && device.getSsdpMessage() instanceof PinnedSsdpMessage;
    }
}
