/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import net.mm2d.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class SubscribeKeeper extends Thread {
    private static final String TAG = "SubscribeKeeper";
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
        super("SubscribeKeeper");
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
                        mControlPoint.unregisterSubscribeService(service, true);
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