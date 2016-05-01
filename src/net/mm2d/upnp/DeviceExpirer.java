/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Deviceの有効期限を確認し、有効期限が切れたDeviceをLost扱いするクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class DeviceExpirer extends Thread {
    private static final String TAG = "DeviceExpirer";
    private static final long MARGIN_TIME = 10000;
    private final ControlPoint mControlPoint;
    private volatile boolean mShutdownRequest = false;
    private final List<Device> mDeviceList;
    private final Comparator<Device> mComparator = new Comparator<Device>() {
        @Override
        public int compare(Device d1, Device d2) {
            return (int) (d1.getExpireTime() - d2.getExpireTime());
        }
    };

    /**
     * インスタンス作成。
     *
     * @param cp ControlPoint
     */
    public DeviceExpirer(ControlPoint cp) {
        super(TAG);
        mDeviceList = new ArrayList<>();
        mControlPoint = cp;
    }

    /**
     * スレッドに割り込みをかけ終了させる。
     */
    public void shutdownRequest() {
        mShutdownRequest = true;
        interrupt();
    }

    /**
     * Deviceの有効期限変化時にコールする。
     */
    public synchronized void update() {
        Collections.sort(mDeviceList, mComparator);
    }

    /**
     * Device追加。
     *
     * @param device 追加されるDevice
     */
    public synchronized void add(Device device) {
        mDeviceList.add(device);
        Collections.sort(mDeviceList, mComparator);
        notifyAll();
    }

    /**
     * Device削除。
     *
     * @param device 削除されるDevice。
     */
    public synchronized void remove(Device device) {
        mDeviceList.remove(device);
    }

    /**
     * 登録されたDeviceをクリア。
     */
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
        } catch (final InterruptedException ignored) {
        }
    }
}
