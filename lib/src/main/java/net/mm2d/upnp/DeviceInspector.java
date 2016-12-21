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
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Deviceの有効期限を確認し、有効期限が切れたDeviceをLost扱いするクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class DeviceInspector implements Runnable {
    private static final String TAG = DeviceInspector.class.getSimpleName();
    private static final long MARGIN_TIME = TimeUnit.SECONDS.toMillis(10);
    @Nonnull
    private final ControlPoint mControlPoint;
    private volatile boolean mShutdownRequest = false;
    private Thread mThread;
    @Nonnull
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
    DeviceInspector(@Nonnull ControlPoint cp) {
        mDeviceList = new ArrayList<>();
        mControlPoint = cp;
    }

    void start() {
        mShutdownRequest = false;
        mThread = new Thread(this, TAG);
        mThread.start();
    }
    /**
     * スレッドに割り込みをかけ終了させる。
     */
    void shutdownRequest() {
        mShutdownRequest = true;
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
    }

    /**
     * Deviceの有効期限変化時にコールする。
     */
    synchronized void update() {
        Collections.sort(mDeviceList, mComparator);
    }

    /**
     * Device追加。
     *
     * @param device 追加されるDevice
     */
     synchronized void add(@Nonnull Device device) {
        mDeviceList.add(device);
        Collections.sort(mDeviceList, mComparator);
        notifyAll();
    }

    /**
     * Device削除。
     *
     * @param device 削除されるDevice。
     */
    synchronized void remove(@Nonnull Device device) {
        mDeviceList.remove(device);
    }

    /**
     * 登録されたDeviceをクリア。
     */
    synchronized void clear() {
        mDeviceList.clear();
    }

    @Override
    public synchronized void run() {
        try {
            while (!mShutdownRequest) {
                while (mDeviceList.size() == 0) {
                    wait();
                }
                final long now = System.currentTimeMillis();
                final Iterator<Device> i = mDeviceList.iterator();
                while (i.hasNext()) {
                    final Device device = i.next();
                    if (device.getExpireTime() < now) {
                        i.remove();
                        mControlPoint.lostDevice(device, false);
                    } else {
                        break;
                    }
                }
                if (mDeviceList.size() != 0) {
                    final Device device = mDeviceList.get(0);
                    final long sleep = device.getExpireTime() - now + MARGIN_TIME;
                    wait(sleep);
                }
            }
        } catch (final InterruptedException ignored) {
        }
    }
}
