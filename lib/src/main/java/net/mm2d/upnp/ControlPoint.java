/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.log.Log;
import net.mm2d.upnp.DeviceHolder.ExpireListener;
import net.mm2d.upnp.SsdpNotifyReceiver.NotifyListener;
import net.mm2d.upnp.SsdpSearchServer.ResponseListener;
import net.mm2d.util.TextUtils;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

/**
 * UPnP ControlPointのクラス。
 *
 * <p>このライブラリを利用する上でアプリからインスタンス化する必要がある唯一のクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class ControlPoint {
    /**
     * 機器発見イベント通知用リスナー。
     *
     * <p>
     * {@link #onDiscover(Device)}
     * {@link #onLost(Device)}
     * 及び、
     * {@link NotifyEventListener#onNotifyEvent(Service, long, String, String)}
     * は、いずれも同一のスレッドからコールされる。
     *
     * @see NotifyEventListener
     */
    public interface DiscoveryListener {
        /**
         * 機器発見時にコールされる。
         *
         * @param device 発見したDevice
         * @see Device
         */
        void onDiscover(@Nonnull Device device);

        /**
         * 機器喪失時にコールされる。
         *
         * <p>有効期限切れ、SSDP byebye受信、ControlPointの停止によって発生する
         *
         * @param device 喪失したDevice
         * @see Device
         */
        void onLost(@Nonnull Device device);
    }

    /**
     * NotifyEvent通知を受け取るリスナー。
     *
     * <p>
     * {@link #onNotifyEvent(Service, long, String, String)}
     * 及び、
     * {@link DiscoveryListener#onDiscover(Device)}
     * {@link DiscoveryListener#onLost(Device)}
     * は、いずれも同一のスレッドからコールされる。
     *
     * @see DiscoveryListener
     */
    public interface NotifyEventListener {
        /**
         * NotifyEvent受信時にコールされる。
         *
         * @param service  対応するService
         * @param seq      シーケンス番号
         * @param variable 変数名
         * @param value    値
         * @see Service
         */
        void onNotifyEvent(
                @Nonnull Service service,
                long seq,
                @Nonnull String variable,
                @Nonnull String value);
    }

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
    private final Set<String> mEmbeddedDeviceUdnSet = new HashSet<>();
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

    /**
     * インスタンス初期化
     *
     * <p>引数のインターフェースを利用するように初期化される。
     * 使用するインターフェースは自動的に選定される。
     *
     * @throws IllegalStateException 使用可能なインターフェースがない。
     * @deprecated Use {@link ControlPointFactory#create()} instead.
     */
    @Deprecated
    public ControlPoint()
            throws IllegalStateException {
        this(Collections.<NetworkInterface>emptyList());
    }

    /**
     * 利用するインターフェースを指定してインスタンス作成。
     *
     * @param interfaces 使用するインターフェース、nullもしくは空の場合自動選択となる。
     * @throws IllegalStateException 使用可能なインターフェースがない。
     * @deprecated Use {@link ControlPointFactory#create(Collection)} instead.
     */
    @Deprecated
    public ControlPoint(@Nullable final Collection<NetworkInterface> interfaces)
            throws IllegalStateException {
        this(Protocol.DEFAULT, interfaces);
    }

    /**
     * インスタンス初期化
     *
     * <p>プロトコルスタックのみ指定して初期化を行う。
     * 使用するインターフェースは自動的に選定される。
     *
     * @param protocol 使用するプロトコルスタック
     * @throws IllegalStateException 使用可能なインターフェースがない。
     * @deprecated Use {@link ControlPointFactory#create(Protocol)} instead.
     */
    @Deprecated
    public ControlPoint(@Nonnull final Protocol protocol) throws IllegalStateException {
        this(protocol, Collections.<NetworkInterface>emptyList());
    }

    /**
     * 利用するインターフェースを指定してインスタンス作成。
     *
     * @param protocol   使用するプロトコルスタック
     * @param interfaces 使用するインターフェース、nullもしくは空の場合自動選択となる。
     * @throws IllegalStateException 使用可能なインターフェースがない。
     * @deprecated Use {@link ControlPointFactory#create(Protocol, Collection)} instead.
     */
    @Deprecated
    public ControlPoint(
            @Nonnull final Protocol protocol,
            @Nullable final Collection<NetworkInterface> interfaces)
            throws IllegalStateException {
        this(protocol, getDefaultInterfacesIfEmpty(protocol, interfaces), new DiFactory(protocol));
    }

    @Nonnull
    private static Collection<NetworkInterface> getDefaultInterfacesIfEmpty(
            @Nonnull final Protocol protocol,
            @Nullable final Collection<NetworkInterface> interfaces) {
        if (interfaces == null || interfaces.isEmpty()) {
            return protocol.getAvailableInterfaces();
        }
        return interfaces;
    }

    // VisibleForTesting
    ControlPoint(
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

        mSearchList = factory.createSsdpSearchServerList(interfaces, new ResponseListener() {
            @Override
            public void onReceiveResponse(@Nonnull final SsdpResponse message) {
                mThreadPool.executeInParallel(new Runnable() {
                    @Override
                    public void run() {
                        onReceiveSsdp(message);
                    }
                });
            }
        });
        mNotifyList = factory.createSsdpNotifyReceiverList(interfaces, new NotifyListener() {
            @Override
            public void onReceiveNotify(@Nonnull final SsdpRequest message) {
                mThreadPool.executeInParallel(new Runnable() {
                    @Override
                    public void run() {
                        onReceiveSsdp(message);
                    }
                });
            }
        });
        mDeviceHolder = factory.createDeviceHolder(new ExpireListener() {
            @Override
            public void onExpire(@Nonnull final Device device) {
                lostDevice(device);
            }
        });
        mSubscribeManager = factory.createSubscribeManager(mThreadPool, mNotifyEventListenerList);
    }

    // VisibleForTesting
    @Nonnull
    HttpClient createHttpClient() {
        return new HttpClient(true);
    }

    // VisibleForTesting
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
    void onReceiveSsdp(@Nonnull final SsdpMessage message) {
        synchronized (mDeviceHolder) {
            final String uuid = message.getUuid();
            final Device device = mDeviceHolder.get(uuid);
            if (device == null) {
                if (mEmbeddedDeviceUdnSet.contains(uuid)) {
                    return;
                }
                onReceiveNewSsdp(message);
                return;
            }
            if (TextUtils.equals(message.getNts(), SsdpMessage.SSDP_BYEBYE)) {
                lostDevice(device);
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
    public void setIconFilter(@Nonnull final IconFilter filter) {
        mIconFilter = filter;
    }

    /**
     * 機器発見のリスナーを登録する。
     *
     * @param listener リスナー
     * @see DiscoveryListener
     */
    public void addDiscoveryListener(@Nonnull final DiscoveryListener listener) {
        mDiscoveryListenerList.add(listener);
    }

    /**
     * 機器発見リスナーを削除する。
     *
     * @param listener リスナー
     * @see DiscoveryListener
     */
    public void removeDiscoveryListener(@Nonnull final DiscoveryListener listener) {
        mDiscoveryListenerList.remove(listener);
    }

    /**
     * NotifyEvent受信リスナーを登録する。
     *
     * @param listener リスナー
     * @see NotifyEventListener
     */
    public void addNotifyEventListener(@Nonnull final NotifyEventListener listener) {
        mNotifyEventListenerList.add(listener);
    }

    /**
     * NotifyEvent受信リスナーを削除する。
     *
     * @param listener リスナー
     * @see NotifyEventListener
     */
    public void removeNotifyEventListener(@Nonnull final NotifyEventListener listener) {
        mNotifyEventListenerList.remove(listener);
    }

    // VisibleForTesting
    void discoverDevice(@Nonnull final Device device) {
        mEmbeddedDeviceUdnSet.addAll(device.getEmbeddedDeviceUdnSet());
        mDeviceHolder.add(device);
        mThreadPool.executeInSequential(new Runnable() {
            @Override
            public void run() {
                mDiscoveryListenerList.onDiscover(device);
            }
        });
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
    void lostDevice(@Nonnull final Device device) {
        mEmbeddedDeviceUdnSet.removeAll(device.getEmbeddedDeviceUdnSet());
        synchronized (mDeviceHolder) {
            final List<Service> list = device.getServiceList();
            for (final Service s : list) {
                mSubscribeManager.unregisterSubscribeService(s);
            }
            mDeviceHolder.remove(device);
        }
        mThreadPool.executeInSequential(new Runnable() {
            @Override
            public void run() {
                mDiscoveryListenerList.onLost(device);
            }
        });
    }

    /**
     * 発見したデバイスの数を返す。
     *
     * @return デバイスの数
     */
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
    @Nullable
    public Device getDevice(@Nonnull final String udn) {
        return mDeviceHolder.get(udn);
    }
}
