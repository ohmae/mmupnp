/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
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
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class ControlPoint {
    private static final String TAG = "ControlPoint";

    public interface DiscoveryListener {
        void onDiscover(Device device);

        void onLost(Device device);
    }

    public interface NotifyEventListener {
        void onNotifyEvent(Service service, long seq, String variable, String value);
    }

    private final List<DiscoveryListener> mDiscoveryListeners;
    private final List<NotifyEventListener> mNotifyEventListeners;
    private final Collection<SsdpSearchServer> mSearchList;
    private final Collection<SsdpNotifyReceiver> mNotifyList;
    private final Map<String, Device> mDeviceMap;
    private final Map<String, Device> mPendingDeviceMap;
    private final Map<String, Service> mSubscribeServiceMap;
    private final EventReceiver mEventServer;
    private final ExecutorService mNetworkExecutor;
    private final ExecutorService mNotifyExecutor;
    private DeviceExpirer mDeviceExpirer;
    private SubscribeKeeper mSubscribeKeeper;
    private final ResponseListener mResponseListener = new ResponseListener() {
        @Override
        public void onReceiveResponse(final SsdpResponseMessage message) {
            mNetworkExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    onReceiveSsdp(message);
                }
            });
        }
    };
    private final NotifyListener mNotifyListener = new NotifyListener() {
        @Override
        public void onReceiveNotify(SsdpRequestMessage message) {
            mNetworkExecutor.submit(new Runnable() {
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
                    mNetworkExecutor.submit(new DeviceLoader(device));
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

    private final EventPacketListener mEventPacketListener = new EventPacketListener() {
        @Override
        public boolean onEventReceived(HttpRequest request) {
            final String sid = request.getHeader(Http.SID);
            final Service service = getSubscribeService(sid);
            if (service == null) {
                return false;
            }
            mNotifyExecutor.submit(new EventNotifyTask(request, service));
            return true;
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
            } catch (IOException | SAXException | ParserConfigurationException e) {
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

    private void discoverDevice(final Device device) {
        synchronized (mDeviceMap) {
            mDeviceMap.put(device.getUuid(), device);
            mDeviceExpirer.add(device);
        }
        mNotifyExecutor.submit(new Runnable() {
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

    private void lostDevice(Device device) {
        lostDevice(device, false);
    }

    private void lostDevice(final Device device, boolean expire) {
        synchronized (mDeviceMap) {
            final List<Service> list = device.getServiceList();
            for (final Service s : list) {
                unregisterSubscribeService(s);
            }
            mDeviceMap.remove(device.getUuid());
            if (!expire) {
                mDeviceExpirer.remove(device);
            }
        }
        mNotifyExecutor.submit(new Runnable() {
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

    public List<Device> getDeviceList() {
        synchronized (mDeviceMap) {
            return new ArrayList<Device>(mDeviceMap.values());
        }
    }

    public Device getDevice(String udn) {
        return mDeviceMap.get(udn);
    }

    private static class DeviceExpirer extends Thread {
        private static final long MARGIN_TIME = 10000;
        private final ControlPoint mControlPoint;
        private volatile boolean mShutdownRequest = false;
        private final List<Device> mDeviceList;
        private final Comparator<Device> mComparator = new Comparator<Device>() {
            @Override
            public int compare(Device o1, Device o2) {
                return (int) (o1.getExpireTime() - o2.getExpireTime());
            }
        };

        public DeviceExpirer(ControlPoint cp) {
            mDeviceList = new ArrayList<>();
            mControlPoint = cp;
        }

        public void shutdownRequest() {
            mShutdownRequest = true;
            interrupt();
        }

        public synchronized void update() {
            mDeviceList.sort(mComparator);
            notifyAll();
        }

        public synchronized void add(Device device) {
            mDeviceList.add(device);
            mDeviceList.sort(mComparator);
            notifyAll();
        }

        public synchronized void remove(Device device) {
            if (mDeviceList.remove(device)) {
                notifyAll();
            }
        }

        public synchronized void clear() {
            mDeviceList.clear();
            notifyAll();
        }

        @Override
        public synchronized void run() {
            try {
                while (!mShutdownRequest) {
                    while (mDeviceList.size() == 0) {
                        wait();
                    }
                    final long current = System.currentTimeMillis();
                    final Iterator<Device> i = mDeviceList.iterator();
                    while (i.hasNext()) {
                        final Device device = i.next();
                        if (device.getExpireTime() < current) {
                            i.remove();
                            mControlPoint.lostDevice(device, true);
                        } else {
                            break;
                        }
                    }
                    if (mDeviceList.size() != 0) {
                        final Device device = mDeviceList.get(0);
                        final long sleep = device.getExpireTime() - current + MARGIN_TIME;
                        wait(sleep);
                    }
                }
            } catch (final InterruptedException e) {
                Log.w(TAG, e);
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
        mDeviceMap = Collections.synchronizedMap(new HashMap<>());
        mPendingDeviceMap = new HashMap<>();
        mSubscribeServiceMap = new HashMap<>();
        mDiscoveryListeners = new ArrayList<>();
        mNotifyEventListeners = new ArrayList<>();
        mNotifyExecutor = Executors.newSingleThreadExecutor();
        mNetworkExecutor = Executors.newCachedThreadPool();
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
        Enumeration<NetworkInterface> nis;
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
                list.add(ni);
            } catch (final SocketException e) {
                continue;
            }
        }
        return list;
    }

    public void initialize() {
        mDeviceExpirer = new DeviceExpirer(this);
        mDeviceExpirer.start();
        mSubscribeKeeper = new SubscribeKeeper(this);
        mSubscribeKeeper.start();
    }

    public void start() {
        try {
            mEventServer.open();
        } catch (final IOException e1) {
            Log.w(TAG, e1);
        }
        for (final SsdpServer socket : mSearchList) {
            try {
                socket.open();
                socket.start();
            } catch (final IOException e) {
                Log.w(TAG, e);
            }
        }
        for (final SsdpServer socket : mNotifyList) {
            try {
                socket.open();
                socket.start();
            } catch (final IOException e) {
                Log.w(TAG, e);
            }
        }
    }

    public void stop() {
        synchronized (mSubscribeServiceMap) {
            final Set<Entry<String, Service>> entrySet = mSubscribeServiceMap.entrySet();
            for (final Iterator<Entry<String, Service>> i = entrySet.iterator(); i.hasNext();) {
                final Entry<String, Service> e = i.next();
                i.remove();
                mNetworkExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            e.getValue().unsubscribe();
                        } catch (final IOException e) {
                            Log.w(TAG, e);
                        }
                    }
                });
            }
        }
        for (final SsdpServer socket : mSearchList) {
            socket.stop();
        }
        for (final SsdpServer socket : mNotifyList) {
            socket.stop();
        }
        for (final SsdpServer socket : mSearchList) {
            socket.close();
        }
        for (final SsdpServer socket : mNotifyList) {
            socket.close();
        }
        mEventServer.close();
        mSubscribeKeeper.clear();
        mDeviceMap.clear();
        mDeviceExpirer.clear();
    }

    public void terminate() {
        mNotifyExecutor.shutdownNow();
        mNetworkExecutor.shutdown();
        try {
            if (!mNetworkExecutor.awaitTermination(
                    Property.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                mNetworkExecutor.shutdownNow();
            }
        } catch (final InterruptedException e) {
            Log.w(TAG, e);
        }
        mSubscribeKeeper.shutdownRequest();
        mSubscribeKeeper = null;
        mDeviceExpirer.shutdownRequest();
        mDeviceExpirer = null;
    }

    public void search(String st) {
        for (final SsdpSearchServer socket : mSearchList) {
            socket.search(st);
        }
    }

    public void search() {
        for (final SsdpSearchServer socket : mSearchList) {
            socket.search();
        }
    }

    int getEventPort() {
        return mEventServer.getLocalPort();
    }

    private static class SubscribeKeeper extends Thread {
        private static final long MARGIN_TIME = 10000;
        private static final long MIN_INTERVAL = 1000;
        private final ControlPoint mControlPoint;
        private volatile boolean mShutdownRequest = false;
        private final List<Service> mServiceList;
        private final Comparator<Service> mComparator = new Comparator<Service>() {
            @Override
            public int compare(Service o1, Service o2) {
                return (int) (getRenewTime(o1) - getRenewTime(o2));
            }
        };

        private long getRenewTime(Service service) {
            long timeout = service.getSubscriptionTimeout();
            if (timeout > MARGIN_TIME) {
                timeout -= MARGIN_TIME;
            } else {
                timeout = timeout * 9 / 10;
            }
            return service.getSubscriptionStart() + timeout;
        }

        public SubscribeKeeper(ControlPoint controlPoint) {
            mControlPoint = controlPoint;
            mServiceList = new ArrayList<>();
        }

        public void shutdownRequest() {
            mShutdownRequest = true;
            interrupt();
        }

        public synchronized void update() {
            mServiceList.sort(mComparator);
            notifyAll();
        }

        public synchronized void add(Service service) {
            mServiceList.add(service);
            mServiceList.sort(mComparator);
            notifyAll();
        }

        public synchronized void remove(Service service) {
            if (mServiceList.remove(service)) {
                notifyAll();
            }
        }

        public synchronized void clear() {
            mServiceList.clear();
        }

        @Override
        public synchronized void run() {
            try {
                while (!mShutdownRequest) {
                    while (mServiceList.size() == 0) {
                        wait();
                    }
                    final long current = System.currentTimeMillis();
                    for (final Service service : mServiceList) {
                        if (getRenewTime(service) < current) {
                            try {
                                service.renewSubscribe(false);
                            } catch (final IOException e) {
                                Log.w(TAG, e);
                            }
                        } else {
                            break;
                        }
                    }
                    mServiceList.sort(mComparator);
                    final Iterator<Service> i = mServiceList.iterator();
                    while (i.hasNext()) {
                        final Service service = i.next();
                        if (service.getSubscriptionStart()
                                + service.getSubscriptionTimeout() < current) {
                            mControlPoint.unregisterSubscribeService(service);
                            i.remove();
                        }
                    }
                    if (mServiceList.size() != 0) {
                        final Service service = mServiceList.get(0);
                        long sleep = getRenewTime(service) - System.currentTimeMillis();
                        if (sleep < MIN_INTERVAL) {
                            sleep = MIN_INTERVAL;
                        }
                        wait(sleep);
                    }
                }
            } catch (final InterruptedException e) {
                Log.w(TAG, e);
            }
        }
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
        synchronized (mSubscribeServiceMap) {
            mSubscribeServiceMap.remove(service.getSubscriptionId());
        }
        mSubscribeKeeper.remove(service);
    }

    void addSubscribeKeeper(Service service) {
        mSubscribeKeeper.add(service);
    }

    void renewSubscribeService() {
        mSubscribeKeeper.update();
    }
}
