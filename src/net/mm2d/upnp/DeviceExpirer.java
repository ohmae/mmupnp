/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.upnp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 *
 */
class DeviceExpirer extends Thread {
    private static final String TAG = "DeviceExpirer";
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
        super(TAG);
        mDeviceList = new ArrayList<>();
        mControlPoint = cp;
    }

    public void shutdownRequest() {
        mShutdownRequest = true;
        interrupt();
    }

    public synchronized void update() {
        mDeviceList.sort(mComparator);
    }

    public synchronized void add(Device device) {
        mDeviceList.add(device);
        mDeviceList.sort(mComparator);
        notifyAll();
    }

    public synchronized void remove(Device device) {
        mDeviceList.remove(device);
    }

    public synchronized void clear() {
        mDeviceList.clear();
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
        }
    }
}
