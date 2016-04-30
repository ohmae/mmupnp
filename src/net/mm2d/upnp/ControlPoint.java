/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.EventReceiver.EventPacketListener;
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
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class ControlPoint {
    public interface DiscoveryListener {
        void onDiscover(Device device);

        void onLost(Device device);
    }

    public interface NotifyEventListener {
        void onNotifyEvent(Service service, long seq, String variable, String value);
    }

    private static final String TAG = "ControlPoint";
    private final List<DiscoveryListener> mDiscoveryListeners;
    private final List<NotifyEventListener> mNotifyEventListeners;
    private final Collection<SsdpSearchServer> mSearchList;
    private final Collection<SsdpNotifyReceiver> mNotifyList;
    private final Map<String, Device> mDeviceMap;
    private final Map<String, Device> mPendingDeviceMap;
    private final Map<String, Service> mSubscribeServiceMap;
    private final EventReceiver mEventServer;
    private final ExecutorService mCachedThreadPool;
    private final ExecutorService mNotifyExecutor;
    private boolean mInitialized = false;
    private boolean mStarted = false;
    private boolean mTerminated = false;
    private DeviceExpirer mDeviceExpirer;
    private SubscribeKeeper mSubscribeKeeper;
    private final ResponseListener mResponseListener = new ResponseListener() {
        @Override
        public void onReceiveResponse(final SsdpResponseMessage message) {
            executeParallel(new Runnable() {
                @Override
                public void run() {
                    onReceiveSsdp(message);
                }
            });
        }
    };
    private final NotifyListener mNotifyListener = new NotifyListener() {
        @Override
        public void onReceiveNotify(final SsdpRequestMessage message) {
            executeParallel(new Runnable() {
                @Override
                public void run() {
                    onReceiveSsdp(message);
                }
            });
        }
    };

    private void onReceiveSsdp(SsdpMessage message) {
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

        public DeviceLoader(Device device) {
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

    private final EventPacketListener mEventPacketListener = new EventPacketListener() {
        @Override
        public boolean onEventReceived(HttpRequest request) {
            final String sid = request.getHeader(Http.SID);
            final Service service = getSubscribeService(sid);
            if (service == null) {
                return false;
            }
            return executeSequential(new EventNotifyTask(request, service));
        }
    };

    private class EventNotifyTask implements Runnable {
        private final HttpRequest mRequest;
        private final Service mService;
        private long mSeq;

        public EventNotifyTask(HttpRequest request, Service service) {
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

        private void notify(String name, String value) {
            final StateVariable variable = mService.findStateVariable(name);
            if (variable == null || !variable.isSendEvents()) {
                Log.w(TAG, "illegal notify argument:" + name);
                return;
            }
            synchronized (mNotifyEventListeners) {
                for (final NotifyEventListener l : mNotifyEventListeners) {
                    l.onNotifyEvent(mService, mSeq, name, value);
                }
            }
        }
    }

    public ControlPoint() throws IllegalStateException {
        this(null);
    }

    public ControlPoint(Collection<NetworkInterface> interfaces) throws IllegalStateException {
        if (interfaces == null) {
            interfaces = getValidNetworkInterfaces();
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
            search.setResponseListener(mResponseListener);
            mSearchList.add(search);
            final SsdpNotifyReceiver notify = new SsdpNotifyReceiver(nif);
            notify.setNotifyListener(mNotifyListener);
            mNotifyList.add(notify);
        }
        mEventServer = new EventReceiver();
        mEventServer.setEventPacketListener(mEventPacketListener);
    }

    private Collection<NetworkInterface> getValidNetworkInterfaces() {
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

    private boolean executeParallel(Runnable command) {
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

    private boolean executeSequential(Runnable command) {
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

    public void start() {
        if (!mInitialized) {
            initialize();
        }
        if (mStarted) {
            return;
        }
        mStarted = true;
        try {
            mEventServer.open();
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

    public void stop() {
        if (!mStarted) {
            return;
        }
        mStarted = false;
        synchronized (mSubscribeServiceMap) {
            final Set<Entry<String, Service>> entrySet = mSubscribeServiceMap.entrySet();
            for (final Iterator<Entry<String, Service>> i = entrySet.iterator(); i.hasNext(); ) {
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
        mEventServer.close();
        mSubscribeKeeper.clear();
        final List<Device> list = new ArrayList<>(mDeviceMap.values());
        for (final Device device : list) {
            lostDevice(device);
        }
        mDeviceMap.clear();
        mDeviceExpirer.clear();
    }

    public void search() {
        search(null);
    }

    public void search(String st) {
        if (!mStarted) {
            throw new IllegalStateException("ControlPoint is not started.");
        }
        for (final SsdpSearchServer server : mSearchList) {
            server.search(st);
        }
    }

    public void addDiscoveryListener(DiscoveryListener listener) {
        synchronized (mDiscoveryListeners) {
            if (!mDiscoveryListeners.contains(listener)) {
                mDiscoveryListeners.add(listener);
            }
        }
    }

    public void removeDiscoveryListener(DiscoveryListener listener) {
        synchronized (mDiscoveryListeners) {
            mDiscoveryListeners.remove(listener);
        }
    }

    public void addNotifyEventListener(NotifyEventListener listener) {
        synchronized (mNotifyEventListeners) {
            if (!mNotifyEventListeners.contains(listener)) {
                mNotifyEventListeners.add(listener);
            }
        }
    }

    public void removeNotifyEventListener(NotifyEventListener listener) {
        synchronized (mNotifyEventListeners) {
            mNotifyEventListeners.remove(listener);
        }
    }

    void discoverDevice(final Device device) {
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

    void lostDevice(Device device) {
        lostDevice(device, false);
    }

    void lostDevice(final Device device, boolean fromExpirer) {
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

    public int getDeviceListSize() {
        return mDeviceMap.size();
    }

    public List<Device> getDeviceList() {
        synchronized (mDeviceMap) {
            return new ArrayList<>(mDeviceMap.values());
        }
    }

    public Device getDevice(String udn) {
        return mDeviceMap.get(udn);
    }

    int getEventPort() {
        return mEventServer.getLocalPort();
    }

    Service getSubscribeService(String subscriptionId) {
        synchronized (mSubscribeServiceMap) {
            return mSubscribeServiceMap.get(subscriptionId);
        }
    }

    void registerSubscribeService(Service service) {
        synchronized (mSubscribeServiceMap) {
            mSubscribeServiceMap.put(service.getSubscriptionId(), service);
        }
    }

    void unregisterSubscribeService(Service service) {
        unregisterSubscribeService(service, false);
    }

    void unregisterSubscribeService(Service service, boolean fromKeeper) {
        synchronized (mSubscribeServiceMap) {
            mSubscribeServiceMap.remove(service.getSubscriptionId());
        }
        if (!fromKeeper) {
            mSubscribeKeeper.remove(service);
        }
    }

    void addSubscribeKeeper(Service service) {
        mSubscribeKeeper.add(service);
    }

    void renewSubscribeService() {
        mSubscribeKeeper.update();
    }
}
