/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.EventReceiver.EventMessageListener;
import net.mm2d.upnp.SsdpNotifyReceiver.NotifyListener;
import net.mm2d.upnp.SsdpSearchServer.ResponseListener;
import net.mm2d.util.Log;
import net.mm2d.util.TextUtils;
import net.mm2d.util.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

/**
 * UPnP ControlPointのクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class ControlPoint {
    private static final String TAG = ControlPoint.class.getSimpleName();

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
        void onNotifyEvent(@Nonnull Service service, long seq,
                           @Nonnull String variable, @Nonnull String value);
    }

    private IconFilter mIconFilter = IconFilter.NONE;
    @Nonnull
    private final List<DiscoveryListener> mDiscoveryListeners;
    @Nonnull
    private final List<NotifyEventListener> mNotifyEventListeners;
    @Nonnull
    private final Collection<SsdpSearchServer> mSearchList;
    @Nonnull
    private final Collection<SsdpNotifyReceiver> mNotifyList;
    @Nonnull
    private final Map<String, Device> mDeviceMap;
    @Nonnull
    private final Map<String, Device> mPendingDeviceMap;
    @Nonnull
    private final EventReceiver mEventReceiver;
    @Nonnull
    private final ExecutorService mCachedThreadPool;
    @Nonnull
    private final ExecutorService mNotifyExecutor;
    private boolean mInitialized = false;
    private boolean mStarted = false;
    private boolean mTerminated = false;
    @Nonnull
    private final DeviceInspector mDeviceInspector;
    @Nonnull
    private final SubscribeKeeper mSubscribeKeeper;

    private void onReceiveSsdp(@Nonnull SsdpMessage message) {
        final String uuid = message.getUuid();
        synchronized (mDeviceMap) {
            Device device = mDeviceMap.get(uuid);
            if (device == null) {
                if (TextUtils.equals(message.getNts(), SsdpMessage.SSDP_BYEBYE)) {
                    if (mPendingDeviceMap.get(uuid) != null) {
                        mPendingDeviceMap.remove(uuid);
                    }
                    return;
                }
                device = mPendingDeviceMap.get(uuid);
                if (device != null) {
                    device.setSsdpMessage(message);
                } else {
                    device = new Device(ControlPoint.this);
                    device.setSsdpMessage(message);
                    mPendingDeviceMap.put(message.getUuid(), device);
                    if (!executeParallel(new DeviceLoader(device))) {
                        mPendingDeviceMap.remove(message.getUuid());
                    }
                }
            } else {
                if (TextUtils.equals(message.getNts(), SsdpMessage.SSDP_BYEBYE)) {
                    lostDevice(device);
                } else {
                    device.setSsdpMessage(message);
                    mDeviceInspector.update();
                }
            }
        }
    }

    private class DeviceLoader implements Runnable {
        private final Device mDevice;

        DeviceLoader(@Nonnull Device device) {
            mDevice = device;
        }

        @Override
        public void run() {
            final String uuid = mDevice.getUuid();
            try {
                mDevice.loadDescription(mIconFilter);
                synchronized (mDeviceMap) {
                    if (mPendingDeviceMap.get(uuid) != null) {
                        mPendingDeviceMap.remove(uuid);
                        discoverDevice(mDevice);
                    }
                }
            } catch (final IOException | SAXException | ParserConfigurationException e) {
                synchronized (mDeviceMap) {
                    mPendingDeviceMap.remove(uuid);
                }
            }
        }
    }

    private class EventNotifyTask implements Runnable {
        private final HttpRequest mRequest;
        private final Service mService;
        private long mSeq;

        public EventNotifyTask(@Nonnull HttpRequest request, @Nonnull Service service) {
            mRequest = request;
            mService = service;
            final String seq = mRequest.getHeader(Http.SEQ);
            if (TextUtils.isEmpty(seq)) {
                mSeq = 0;
                return;
            }
            try {
                mSeq = Long.parseLong(seq);
            } catch (final NumberFormatException e) {
                mSeq = 0;
            }
        }

        @Override
        public void run() {
            final String xml = mRequest.getBody();
            if (TextUtils.isEmpty(xml)) {
                return;
            }
            try {
                final Document doc = XmlUtils.newDocument(xml);
                Node node = doc.getDocumentElement().getFirstChild();
                for (; node != null; node = node.getNextSibling()) {
                    if (node.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    if (TextUtils.equals(node.getLocalName(), "property")) {
                        Node c = node.getFirstChild();
                        for (; c != null; c = c.getNextSibling()) {
                            if (c.getNodeType() != Node.ELEMENT_NODE) {
                                continue;
                            }
                            final String name = c.getLocalName();
                            final String value = c.getTextContent();
                            notifyEvent(name, value);
                        }
                    }
                }
            } catch (IOException | SAXException | ParserConfigurationException ignored) {
            }
        }

        private void notifyEvent(@Nullable String name, @Nullable String value) {
            final StateVariable variable = mService.findStateVariable(name);
            if (variable == null || !variable.isSendEvents() || value == null) {
                Log.w(TAG, "illegal notify argument:" + name + " " + value);
                return;
            }
            synchronized (mNotifyEventListeners) {
                for (final NotifyEventListener l : mNotifyEventListeners) {
                    l.onNotifyEvent(mService, mSeq, variable.getName(), value);
                }
            }
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
    public ControlPoint(@Nonnull NetworkInterface... interfaces) {
        this(Arrays.asList(interfaces));
    }

    /**
     * 利用するインターフェースを指定してインスタンス作成。
     *
     * @param interfaces 使用するインターフェース、nullもしくは空の場合自動選択となる。
     * @throws IllegalStateException 使用可能なインターフェースがない。
     */
    public ControlPoint(@Nullable Collection<NetworkInterface> interfaces)
            throws IllegalStateException {
        if (interfaces == null || interfaces.isEmpty()) {
            interfaces = getAvailableInterfaces();
        }
        if (interfaces.isEmpty()) {
            throw new IllegalStateException("no valid network interface.");
        }
        mSearchList = new ArrayList<>();
        mNotifyList = new ArrayList<>();
        mDeviceMap = Collections.synchronizedMap(new LinkedHashMap<String, Device>());
        mPendingDeviceMap = new HashMap<>();
        mDiscoveryListeners = new ArrayList<>();
        mNotifyEventListeners = new ArrayList<>();
        mNotifyExecutor = Executors.newSingleThreadExecutor();
        mCachedThreadPool = Executors.newCachedThreadPool();
        mDeviceInspector = new DeviceInspector(this);
        mSubscribeKeeper = new SubscribeKeeper();

        for (final NetworkInterface nif : interfaces) {
            final SsdpSearchServer search = new SsdpSearchServer(nif);
            search.setResponseListener(new ResponseListener() {
                @Override
                public void onReceiveResponse(final @Nonnull SsdpResponseMessage message) {
                    executeParallel(new Runnable() {
                        @Override
                        public void run() {
                            onReceiveSsdp(message);
                        }
                    });
                }
            });
            mSearchList.add(search);
            final SsdpNotifyReceiver notify = new SsdpNotifyReceiver(nif);
            notify.setNotifyListener(new NotifyListener() {
                @Override
                public void onReceiveNotify(final @Nonnull SsdpRequestMessage message) {
                    executeParallel(new Runnable() {
                        @Override
                        public void run() {
                            onReceiveSsdp(message);
                        }
                    });
                }
            });
            mNotifyList.add(notify);
        }
        mEventReceiver = new EventReceiver();
        mEventReceiver.setEventMessageListener(new EventMessageListener() {
            @Override
            public boolean onEventReceived(@Nonnull HttpRequest request) {
                final String sid = request.getHeader(Http.SID);
                final Service service = getSubscribeService(sid);
                return service != null && executeSequential(new EventNotifyTask(request, service));
            }
        });
    }

    @Nonnull
    private Collection<NetworkInterface> getAvailableInterfaces() {
        final Collection<NetworkInterface> list = new ArrayList<>();
        final Enumeration<NetworkInterface> nis;
        try {
            nis = NetworkInterface.getNetworkInterfaces();
        } catch (final SocketException e) {
            return list;
        }
        while (nis.hasMoreElements()) {
            final NetworkInterface ni = nis.nextElement();
            try {
                if (ni.isLoopback()
                        || ni.isPointToPoint()
                        || ni.isVirtual()
                        || !ni.isUp()) {
                    continue;
                }
                final List<InterfaceAddress> ifas = ni.getInterfaceAddresses();
                for (final InterfaceAddress a : ifas) {
                    if (a.getAddress() instanceof Inet4Address) {
                        list.add(ni);
                        break;
                    }
                }
            } catch (final SocketException ignored) {
            }
        }
        return list;
    }

    private boolean executeParallel(@Nonnull Runnable command) {
        if (mCachedThreadPool.isShutdown()) {
            return false;
        }
        try {
            mCachedThreadPool.execute(command);
        } catch (final RejectedExecutionException ignored) {
            return false;
        }
        return true;
    }

    private boolean executeSequential(@Nonnull Runnable command) {
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
        if (mInitialized) {
            return;
        }
        if (mTerminated) {
            throw new IllegalStateException(
                    "ControlPoint is already terminated, cannot re-initialize.");
        }
        mDeviceInspector.start();
        mSubscribeKeeper.start();
        mInitialized = true;
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
        if (mStarted) {
            stop();
        }
        if (!mInitialized || mTerminated) {
            return;
        }
        mTerminated = true;
        mNotifyExecutor.shutdownNow();
        mCachedThreadPool.shutdown();
        try {
            if (!mCachedThreadPool.awaitTermination(
                    Property.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                mCachedThreadPool.shutdownNow();
            }
        } catch (final InterruptedException e) {
            Log.w(TAG, e);
        }
        mSubscribeKeeper.shutdownRequest();
        mDeviceInspector.shutdownRequest();
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
        if (!mInitialized) {
            initialize();
        }
        if (mStarted) {
            return;
        }
        mStarted = true;
        try {
            mEventReceiver.open();
        } catch (final IOException e1) {
            Log.w(TAG, e1);
        }
        for (final SsdpServer server : mSearchList) {
            try {
                server.open();
                server.start();
            } catch (final IOException e) {
                Log.w(TAG, e);
            }
        }
        for (final SsdpServer server : mNotifyList) {
            try {
                server.open();
                server.start();
            } catch (final IOException e) {
                Log.w(TAG, e);
            }
        }
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
        if (!mStarted) {
            return;
        }
        mStarted = false;
        List<Service> serviceList = mSubscribeKeeper.getServiceList();
        for (final Service service : serviceList) {
            executeParallel(new Runnable() {
                @Override
                public void run() {
                    try {
                        service.unsubscribe();
                    } catch (final IOException e) {
                        Log.w(TAG, e);
                    }
                }
            });
        }
        mSubscribeKeeper.clear();
        for (final SsdpServer server : mSearchList) {
            server.stop();
        }
        for (final SsdpServer server : mNotifyList) {
            server.stop();
        }
        for (final SsdpServer server : mSearchList) {
            server.close();
        }
        for (final SsdpServer server : mNotifyList) {
            server.close();
        }
        mEventReceiver.close();
        final List<Device> list = new ArrayList<>(mDeviceMap.values());
        for (final Device device : list) {
            lostDevice(device);
        }
        mDeviceMap.clear();
        mDeviceInspector.clear();
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
    public void search(@Nullable String st) {
        if (!mStarted) {
            throw new IllegalStateException("ControlPoint is not started.");
        }
        for (final SsdpSearchServer server : mSearchList) {
            server.search(st);
        }
    }

    /**
     * ダウンロードするIconを選択するフィルタを設定する。
     *
     * @param filter 設定するフィルタ、nullは指定できない。
     * @see IconFilter#NONE
     * @see IconFilter#ALL
     */
    public void setIconFilter(@Nonnull IconFilter filter) {
        mIconFilter = filter;
    }

    /**
     * 機器発見のリスナーを登録する。
     *
     * @param listener リスナー
     * @see DiscoveryListener
     */
    public void addDiscoveryListener(@Nonnull DiscoveryListener listener) {
        synchronized (mDiscoveryListeners) {
            if (!mDiscoveryListeners.contains(listener)) {
                mDiscoveryListeners.add(listener);
            }
        }
    }

    /**
     * 機器発見リスナーを削除する。
     *
     * @param listener リスナー
     * @see DiscoveryListener
     */
    public void removeDiscoveryListener(@Nonnull DiscoveryListener listener) {
        synchronized (mDiscoveryListeners) {
            mDiscoveryListeners.remove(listener);
        }
    }

    /**
     * NotifyEvent受信リスナーを登録する。
     *
     * @param listener リスナー
     * @see NotifyEventListener
     */
    public void addNotifyEventListener(@Nonnull NotifyEventListener listener) {
        synchronized (mNotifyEventListeners) {
            if (!mNotifyEventListeners.contains(listener)) {
                mNotifyEventListeners.add(listener);
            }
        }
    }

    /**
     * NotifyEvent受信リスナーを削除する。
     *
     * @param listener リスナー
     * @see NotifyEventListener
     */
    public void removeNotifyEventListener(@Nonnull NotifyEventListener listener) {
        synchronized (mNotifyEventListeners) {
            mNotifyEventListeners.remove(listener);
        }
    }

    private void discoverDevice(final @Nonnull Device device) {
        synchronized (mDeviceMap) {
            mDeviceMap.put(device.getUuid(), device);
            mDeviceInspector.add(device);
        }
        executeSequential(new Runnable() {
            @Override
            public void run() {
                synchronized (mDiscoveryListeners) {
                    for (final DiscoveryListener l : mDiscoveryListeners) {
                        l.onDiscover(device);
                    }
                }
            }
        });
    }

    void lostDevice(@Nonnull Device device) {
        lostDevice(device, true);
    }

    /**
     * デバイスの喪失を行う。
     *
     * <p>DeviceInspectorからコールするためにパッケージデフォルトとする
     * 他からはコールしないこと。
     *
     * @param device          喪失してデバイス
     * @param notifyInspector falseの場合Inspectorに通知しない
     * @see Device
     * @see DeviceInspector
     */
    void lostDevice(final @Nonnull Device device, boolean notifyInspector) {
        synchronized (mDeviceMap) {
            final List<Service> list = device.getServiceList();
            for (final Service s : list) {
                unregisterSubscribeService(s);
            }
            mDeviceMap.remove(device.getUuid());
            if (notifyInspector) {
                mDeviceInspector.remove(device);
            }
        }
        executeSequential(new Runnable() {
            @Override
            public void run() {
                synchronized (mDiscoveryListeners) {
                    for (final DiscoveryListener l : mDiscoveryListeners) {
                        l.onLost(device);
                    }
                }
            }
        });
    }

    /**
     * 発見したデバイスの数を返す。
     *
     * @return デバイスの数
     */
    public int getDeviceListSize() {
        return mDeviceMap.size();
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
        synchronized (mDeviceMap) {
            return new ArrayList<>(mDeviceMap.values());
        }
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
    public Device getDevice(@Nullable String udn) {
        return mDeviceMap.get(udn);
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
    Service getSubscribeService(@Nonnull String subscriptionId) {
        return mSubscribeKeeper.getService(subscriptionId);
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
    void registerSubscribeService(@Nonnull Service service, boolean keep) {
        mSubscribeKeeper.add(service, keep);
    }

    /**
     * 指定SubscriptionIDのサービスを購読リストから削除する。
     *
     * @param service 削除するService
     * @see Service
     * @see Service#unsubscribe()
     */
    void unregisterSubscribeService(@Nonnull Service service) {
        mSubscribeKeeper.remove(service);
    }
}
