/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import net.mm2d.upnp.EventReceiver.EventMessageListener;
import net.mm2d.upnp.SsdpNotifyReceiver.NotifyListener;
import net.mm2d.upnp.SsdpSearchServer.ResponseListener;
import net.mm2d.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * UPnP ControlPointのクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class ControlPoint {
    /**
     * 機器発見イベント通知用リスナー。
     *
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
        void onDiscover(@NotNull Device device);

        /**
         * 機器喪失時にコールされる。
         *
         * 有効期限切れ、SSDP byebye受信、ControlPointの停止によって発生する
         *
         * @param device 喪失したDevice
         * @see Device
         */
        void onLost(@NotNull Device device);
    }

    /**
     * NotifyEvent通知を受け取るリスナー。
     *
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
         * NofiyEvnet受信時にコールされる。
         *
         * @param service 対応するService
         * @param seq シーケンス番号
         * @param variable 変数名
         * @param value 値
         * @see Service
         */
        void onNotifyEvent(@NotNull Service service, long seq, @NotNull String variable, @NotNull String value);
    }

    private static final String TAG = "ControlPoint";
    private final List<DiscoveryListener> mDiscoveryListeners;
    private final List<NotifyEventListener> mNotifyEventListeners;
    private final Collection<SsdpSearchServer> mSearchList;
    private final Collection<SsdpNotifyReceiver> mNotifyList;
    private final Map<String, Device> mDeviceMap;
    private final Map<String, Device> mPendingDeviceMap;
    private final Map<String, Service> mSubscribeServiceMap;
    private final EventReceiver mEventReceiver;
    private final ExecutorService mCachedThreadPool;
    private final ExecutorService mNotifyExecutor;
    private boolean mInitialized = false;
    private boolean mStarted = false;
    private boolean mTerminated = false;
    private DeviceExpirer mDeviceExpirer;
    private SubscribeKeeper mSubscribeKeeper;

    private void onReceiveSsdp(@NotNull SsdpMessage message) {
        final String uuid = message.getUuid();
        synchronized (mDeviceMap) {
            Device device = mDeviceMap.get(uuid);
            if (device == null) {
                if (SsdpMessage.SSDP_BYEBYE.equals(message.getNts())) {
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
                if (SsdpMessage.SSDP_BYEBYE.equals(message.getNts())) {
                    lostDevice(device);
                } else {
                    device.setSsdpMessage(message);
                    mDeviceExpirer.update();
                }
            }
        }
    }

    private class DeviceLoader implements Runnable {
        private final Device mDevice;

        public DeviceLoader(@NotNull Device device) {
            mDevice = device;
        }

        @Override
        public void run() {
            final String uuid = mDevice.getUuid();
            try {
                mDevice.loadDescription();
                synchronized (mDeviceMap) {
                    if (mPendingDeviceMap.get(uuid) != null) {
                        mPendingDeviceMap.remove(uuid);
                        discoverDevice(mDevice);
                    }
                }
            } catch (final IOException | SAXException | ParserConfigurationException e) {
                mPendingDeviceMap.remove(uuid);
            }
        }
    }

    private class EventNotifyTask implements Runnable {
        private final HttpRequest mRequest;
        private final Service mService;
        private long mSeq;

        public EventNotifyTask(@NotNull HttpRequest request, @NotNull Service service) {
            mRequest = request;
            mService = service;
            try {
                mSeq = Long.parseLong(mRequest.getHeader(Http.SEQ));
            } catch (final NumberFormatException e) {
                mSeq = 0;
            }
        }

        @Override
        public void run() {
            try {
                final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                final DocumentBuilder db = dbf.newDocumentBuilder();
                final Document doc = db
                        .parse(new InputSource(new StringReader(mRequest.getBody())));
                Node node = doc.getDocumentElement().getFirstChild();
                for (; node != null; node = node.getNextSibling()) {
                    if (node.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    if ("property".equals(node.getLocalName())) {
                        Node c = node.getFirstChild();
                        for (; c != null; c = c.getNextSibling()) {
                            if (c.getNodeType() != Node.ELEMENT_NODE) {
                                continue;
                            }
                            final String name = c.getLocalName();
                            final String value = c.getTextContent();
                            notify(name, value);
                        }
                    }
                }
            } catch (IOException | SAXException | ParserConfigurationException ignored) {
            }
        }

        private void notify(@Nullable String name, @Nullable String value) {
            final StateVariable variable = mService.findStateVariable(name);
            if (variable == null || !variable.isSendEvents() || value == null) {
                Log.w(TAG, "illegal notify argument:" + name + " " + value);
                return;
            }
            synchronized (mNotifyEventListeners) {
                for (final NotifyEventListener l : mNotifyEventListeners) {
                    l.onNotifyEvent(mService, mSeq, name, value);
                }
            }
        }
    }

    /**
     * インスタンス初期化
     *
     * 引数のインターフェースを利用するように初期化される。
     * 引数なしの場合、使用するインターフェースは自動的に選定される。
     *
     * @param interfaces 使用するインターフェース、指定しない場合は自動選択となる。
     * @throws IllegalStateException 使用可能なインターフェースがない。
     */
    public ControlPoint(@NotNull NetworkInterface... interfaces) {
        this(Arrays.asList(interfaces));
    }

    /**
     * 利用するインターフェースを指定してインスタンス作成。
     *
     * @param interfaces 使用するインターフェース、nullもしくは空の場合自動選択となる。
     * @throws IllegalStateException 使用可能なインターフェースがない。
     */
    public ControlPoint(@Nullable Collection<NetworkInterface> interfaces) throws IllegalStateException {
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
        mSubscribeServiceMap = new HashMap<>();
        mDiscoveryListeners = new ArrayList<>();
        mNotifyEventListeners = new ArrayList<>();
        mNotifyExecutor = Executors.newSingleThreadExecutor();
        mCachedThreadPool = Executors.newCachedThreadPool();
        for (final NetworkInterface nif : interfaces) {
            final SsdpSearchServer search = new SsdpSearchServer(nif);
            search.setResponseListener(new ResponseListener() {
                @Override
                public void onReceiveResponse(final SsdpResponseMessage message) {
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
                public void onReceiveNotify(final SsdpRequestMessage message) {
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
            public boolean onEventReceived(HttpRequest request) {
                final String sid = request.getHeader(Http.SID);
                final Service service = getSubscribeService(sid);
                return service != null && executeSequential(new EventNotifyTask(request, service));
            }
        });
    }

    @NotNull
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

    private boolean executeParallel(@NotNull Runnable command) {
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

    private boolean executeSequential(@NotNull Runnable command) {
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
     * 利用前にかならず実行する。
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
        mDeviceExpirer = new DeviceExpirer(this);
        mDeviceExpirer.start();
        mSubscribeKeeper = new SubscribeKeeper(this);
        mSubscribeKeeper.start();
        mInitialized = true;
    }

    /**
     * 終了処理を行う。
     *
     * 動作中の場合、停止処理を行う。
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
        mSubscribeKeeper = null;
        mDeviceExpirer.shutdownRequest();
        mDeviceExpirer = null;
    }

    /**
     * 処理を開始する。
     *
     * 本メソッドのコール前はネットワークに関連する処理を実行することはできない。
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
     * 開始していない状態、既に停止済みの状態の場合なにも行われない。
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
        synchronized (mSubscribeServiceMap) {
            final Set<Entry<String, Service>> entrySet = mSubscribeServiceMap.entrySet();
            for (final Iterator<Entry<String, Service>> i = entrySet.iterator(); i.hasNext();) {
                final Entry<String, Service> entry = i.next();
                i.remove();
                executeParallel(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            entry.getValue().unsubscribe();
                        } catch (final IOException e) {
                            Log.w(TAG, e);
                        }
                    }
                });
            }
        }
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
        mSubscribeKeeper.clear();
        final List<Device> list = new ArrayList<>(mDeviceMap.values());
        for (final Device device : list) {
            lostDevice(device);
        }
        mDeviceMap.clear();
        mDeviceExpirer.clear();
    }

    /**
     * Searchパケットを送出する。
     *
     * {@link #search(String)}を
     * search(null)でコールするのと等価。
     */
    public void search() {
        search(null);
    }

    /**
     * Searchパケットを送出する。
     *
     * stがnullの場合、"ssdp:all"として動作する。
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
     * 機器発見のリスナーを登録する。
     *
     * @param listener リスナー
     * @see DiscoveryListener
     */
    public void addDiscoveryListener(@NotNull DiscoveryListener listener) {
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
    public void removeDiscoveryListener(@NotNull DiscoveryListener listener) {
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
    public void addNotifyEventListener(@NotNull NotifyEventListener listener) {
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
    public void removeNotifyEventListener(@NotNull NotifyEventListener listener) {
        synchronized (mNotifyEventListeners) {
            mNotifyEventListeners.remove(listener);
        }
    }

    private void discoverDevice(@NotNull final Device device) {
        synchronized (mDeviceMap) {
            mDeviceMap.put(device.getUuid(), device);
            mDeviceExpirer.add(device);
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

    private void lostDevice(@NotNull Device device) {
        lostDevice(device, false);
    }

    /**
     * デバイスの喪失を行う。
     *
     * Expirerからコールするためにパッケージデフォルトとする
     * 他からはコールしないこと。
     *
     * @param device 喪失してデバイス
     * @param fromExpirer trueの場合Expirerに通知しない。
     * @see Device
     * @see DeviceExpirer
     */
    void lostDevice(@NotNull final Device device, boolean fromExpirer) {
        synchronized (mDeviceMap) {
            final List<Service> list = device.getServiceList();
            for (final Service s : list) {
                unregisterSubscribeService(s);
            }
            mDeviceMap.remove(device.getUuid());
            if (!fromExpirer) {
                mDeviceExpirer.remove(device);
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
     * 内部で保持するリストのコピーが返される。
     *
     * @return デバイスのリスト
     * @see Device
     */
    @NotNull
    public List<Device> getDeviceList() {
        synchronized (mDeviceMap) {
            return new ArrayList<>(mDeviceMap.values());
        }
    }

    /**
     * 指定UDNのデバイスを返す。
     *
     * 見つからない場合nullが返る。
     *
     * @param udn UDN
     * @return 指定UDNのデバイス
     * @see Device
     */
    @Nullable
    public Device getDevice(String udn) {
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
     * 合致するServiceがない場合null
     *
     * @param subscriptionId SubscriptionID
     * @return 該当Service
     * @see Service
     */
    @Nullable
    Service getSubscribeService(@NotNull String subscriptionId) {
        synchronized (mSubscribeServiceMap) {
            return mSubscribeServiceMap.get(subscriptionId);
        }
    }

    /**
     * SubscriptionIDが確定したServiceを購読リストに登録する
     *
     * Serviceのsubscribeが実行された後にServiceからコールされる。
     *
     * @param service 登録するService
     * @see Service
     * @see Service#subscribe()
     */
    void registerSubscribeService(@NotNull Service service) {
        synchronized (mSubscribeServiceMap) {
            mSubscribeServiceMap.put(service.getSubscriptionId(), service);
        }
    }

    /**
     * 指定SubscriptionIDのサービスを購読リストから削除する。
     *
     * @param service 削除するService
     * @see Service
     * @see Service#unsubscribe()
     */
    void unregisterSubscribeService(@NotNull Service service) {
        synchronized (mSubscribeServiceMap) {
            mSubscribeServiceMap.remove(service.getSubscriptionId());
        }
        mSubscribeKeeper.remove(service);
    }

    /**
     * 購読期限切れのServiceを削除する。
     */
    void removeExpiredSubscribeService() {
        synchronized (mSubscribeServiceMap) {
            final long now = System.currentTimeMillis();
            final Iterator<Entry<String, Service>> i = mSubscribeServiceMap.entrySet().iterator();
            while (i.hasNext()) {
                final Service service = i.next().getValue();
                if (service.getSubscriptionStart()
                        + service.getSubscriptionTimeout() < now) {
                    mSubscribeKeeper.remove(service);
                    i.remove();
                    service.expired();
                }
            }
        }
    }

    /**
     * Subscribe継続処理へ登録する。
     *
     * @param service 登録するService
     * @see Service
     * @see SubscribeKeeper
     */
    void addSubscribeKeeper(@NotNull Service service) {
        mSubscribeKeeper.add(service);
    }

    /**
     * renew処理実行の通知
     */
    void renewSubscribeService() {
        mSubscribeKeeper.update();
    }
}
