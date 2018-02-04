/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.log.Log;
import net.mm2d.upnp.EventReceiver.EventMessageListener;
import net.mm2d.upnp.SsdpNotifyReceiver.NotifyListener;
import net.mm2d.upnp.SsdpSearchServer.ResponseListener;
import net.mm2d.util.NetworkUtils;
import net.mm2d.util.StringPair;
import net.mm2d.util.TextUtils;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

/**
 * UPnP ControlPointのクラス。
 *
 * <p>このライブラリを利用する上でアプリからインスタンス化する必要がある唯一のクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
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
    private final Map<String, Device.Builder> mLoadingDeviceMap;
    @Nonnull
    private final EventReceiver mEventReceiver;
    @Nonnull
    private final ExecutorService mIoExecutor;
    @Nonnull
    private final ExecutorService mNotifyExecutor;
    @Nonnull
    private final AtomicBoolean mInitialized = new AtomicBoolean();
    @Nonnull
    private final AtomicBoolean mStarted = new AtomicBoolean();
    @Nonnull
    private final DeviceHolder mDeviceHolder;
    @Nonnull
    private final SubscribeHolder mSubscribeHolder;

    // VisibleForTesting
    @Nonnull
    HttpClient createHttpClient() {
        return new HttpClient(true);
    }

    // VisibleForTesting
    void onReceiveSsdp(@Nonnull final SsdpMessage message) {
        synchronized (mDeviceHolder) {
            final Device device = mDeviceHolder.get(message.getUuid());
            if (device == null) {
                onReceiveNewSsdp(message);
                return;
            }
            if (TextUtils.equals(message.getNts(), SsdpMessage.SSDP_BYEBYE)) {
                lostDevice(device);
            } else {
                device.updateSsdpMessage(message);
            }
        }
    }

    private void onReceiveNewSsdp(@Nonnull final SsdpMessage message) {
        final String uuid = message.getUuid();
        if (TextUtils.equals(message.getNts(), SsdpMessage.SSDP_BYEBYE)) {
            mLoadingDeviceMap.remove(uuid);
            return;
        }
        final Device.Builder deviceBuilder = mLoadingDeviceMap.get(uuid);
        if (deviceBuilder != null) {
            deviceBuilder.updateSsdpMessage(message);
            return;
        }
        final Device.Builder builder = new Device.Builder(this, message);
        mLoadingDeviceMap.put(uuid, builder);
        if (!executeInParallel(new DeviceLoader(builder))) {
            mLoadingDeviceMap.remove(uuid);
        }
    }

    private class DeviceLoader implements Runnable {
        @Nonnull
        private final Device.Builder mDeviceBuilder;

        DeviceLoader(@Nonnull final Device.Builder builder) {
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

    private class EventNotifyTask implements Runnable {
        @Nonnull
        private final Service mService;
        private final long mSeq;
        @Nonnull
        private final List<StringPair> mProperties;

        EventNotifyTask(
                @Nonnull final Service service,
                final long seq,
                @Nonnull final List<StringPair> properties) {
            mService = service;
            mSeq = seq;
            mProperties = properties;
        }

        @Override
        public void run() {
            for (final StringPair pair : mProperties) {
                notifyEvent(pair.getKey(), pair.getValue());
            }
        }

        private void notifyEvent(
                @Nullable final String name,
                @Nullable final String value) {
            final StateVariable variable = mService.findStateVariable(name);
            if (variable == null || !variable.isSendEvents() || value == null) {
                Log.w("illegal notify argument:" + name + " " + value);
                return;
            }
            mNotifyEventListenerList.onNotifyEvent(mService, mSeq, variable.getName(), value);
        }
    }

    /**
     * インスタンス初期化
     *
     * <p>引数のインターフェースを利用するように初期化される。
     * 引数なしの場合、使用するインターフェースは自動的に選定される。
     *
     * @param interfaces 使用するインターフェース、指定しない場合は自動選択となる。
     * @throws IllegalStateException 使用可能なインターフェースがない。
     */
    public ControlPoint(@Nonnull final NetworkInterface... interfaces)
            throws IllegalStateException {
        this(Arrays.asList(interfaces));
    }

    /**
     * 利用するインターフェースを指定してインスタンス作成。
     *
     * @param interfaces 使用するインターフェース、nullもしくは空の場合自動選択となる。
     * @throws IllegalStateException 使用可能なインターフェースがない。
     */
    public ControlPoint(@Nullable final Collection<NetworkInterface> interfaces)
            throws IllegalStateException {
        this(getDefaultInterfacesIfEmpty(interfaces), new ControlPointDiFactory());
    }

    @Nonnull
    private static Collection<NetworkInterface> getDefaultInterfacesIfEmpty(
            @Nullable final Collection<NetworkInterface> interfaces) {
        if (interfaces == null || interfaces.isEmpty()) {
            return NetworkUtils.getAvailableInet4Interfaces();
        }
        return interfaces;
    }

    // VisibleForTesting
    ControlPoint(
            @Nonnull final Collection<NetworkInterface> interfaces,
            @Nonnull final ControlPointDiFactory factory) {
        if (interfaces.isEmpty()) {
            throw new IllegalStateException("no valid network interface.");
        }
        mLoadingDeviceMap = factory.createLoadingDeviceMap();
        mNotifyExecutor = factory.createNotifyExecutor();
        mIoExecutor = factory.createIoExecutor();
        mDiscoveryListenerList = new DiscoveryListenerList();
        mNotifyEventListenerList = new NotifyEventListenerList();

        mSearchList = factory.createSsdpSearchServerList(interfaces, new ResponseListener() {
            @Override
            public void onReceiveResponse(@Nonnull final SsdpResponseMessage message) {
                executeInParallel(new Runnable() {
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
                executeInParallel(new Runnable() {
                    @Override
                    public void run() {
                        onReceiveSsdp(message);
                    }
                });
            }
        });
        mDeviceHolder = factory.createDeviceHolder(this);
        mSubscribeHolder = factory.createSubscribeHolder();
        mEventReceiver = factory.createEventReceiver(new EventMessageListener() {
            @Override
            public boolean onEventReceived(
                    @Nonnull final String sid,
                    final long seq,
                    @Nonnull final List<StringPair> properties) {
                final Service service = getSubscribeService(sid);
                return service != null && executeInSequential(new EventNotifyTask(service, seq, properties));
            }
        });
    }

    // VisibleForTesting
    boolean executeInParallel(@Nonnull final Runnable command) {
        if (mIoExecutor.isShutdown()) {
            return false;
        }
        try {
            mIoExecutor.execute(command);
        } catch (final RejectedExecutionException ignored) {
            return false;
        }
        return true;
    }

    // VisibleForTesting
    boolean executeInSequential(@Nonnull final Runnable command) {
        if (mNotifyExecutor.isShutdown()) {
            return false;
        }
        try {
            mNotifyExecutor.execute(command);
        } catch (final RejectedExecutionException ignored) {
            return false;
        }
        return true;
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
        mSubscribeHolder.start();
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
        mNotifyExecutor.shutdownNow();
        mIoExecutor.shutdown();
        try {
            if (!mIoExecutor.awaitTermination(
                    Property.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                mIoExecutor.shutdownNow();
            }
        } catch (final InterruptedException e) {
            Log.w(e);
        }
        mSubscribeHolder.shutdownRequest();
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
        try {
            mEventReceiver.open();
        } catch (final IOException e) {
            Log.w(e);
        }
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
        final List<Service> serviceList = mSubscribeHolder.getServiceList();
        for (final Service service : serviceList) {
            executeInParallel(new Runnable() {
                @Override
                public void run() {
                    try {
                        service.unsubscribe();
                    } catch (final IOException e) {
                        Log.w(e);
                    }
                }
            });
        }
        mSubscribeHolder.clear();
        mSearchList.stop();
        mNotifyList.stop();
        mSearchList.close();
        mNotifyList.close();
        mEventReceiver.close();
        final List<Device> list = getDeviceList();
        for (final Device device : list) {
            lostDevice(device);
        }
        mDeviceHolder.clear();
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
        mDeviceHolder.add(device);
        executeInSequential(new Runnable() {
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
        synchronized (mDeviceHolder) {
            final List<Service> list = device.getServiceList();
            for (final Service s : list) {
                unregisterSubscribeService(s);
            }
            mDeviceHolder.remove(device);
        }
        executeInSequential(new Runnable() {
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

    /**
     * イベント通知を受け取るポートを返す。
     *
     * @return イベント通知受信用ポート番号
     * @see EventReceiver
     */
    int getEventPort() {
        return mEventReceiver.getLocalPort();
    }

    /**
     * SubscriptionIDに合致するServiceを返す。
     *
     * <p>合致するServiceがない場合null
     *
     * @param subscriptionId SubscriptionID
     * @return 該当Service
     * @see Service
     */
    @Nullable
    Service getSubscribeService(@Nonnull final String subscriptionId) {
        return mSubscribeHolder.getService(subscriptionId);
    }

    /**
     * SubscriptionIDが確定したServiceを購読リストに登録する
     *
     * <p>Serviceのsubscribeが実行された後にServiceからコールされる。
     *
     * @param service 登録するService
     * @see Service
     * @see Service#subscribe()
     */
    void registerSubscribeService(
            @Nonnull final Service service,
            final boolean keep) {
        mSubscribeHolder.add(service, keep);
    }

    /**
     * 指定SubscriptionIDのサービスを購読リストから削除する。
     *
     * @param service 削除するService
     * @see Service
     * @see Service#unsubscribe()
     */
    void unregisterSubscribeService(@Nonnull final Service service) {
        mSubscribeHolder.remove(service);
    }
}
