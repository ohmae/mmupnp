/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ControlPointで発見したDeviceを保持するクラス。
 *
 * <p>Deviceの有効期限を確認し、有効期限が切れたDeviceをLostとして通知する。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
class DeviceHolder implements Runnable {
    private static final String TAG = DeviceHolder.class.getSimpleName();
    private static final long MARGIN_TIME = TimeUnit.SECONDS.toMillis(10);

    private final Object mThreadLock = new Object();
    private volatile boolean mShutdownRequest = false;
    private Thread mThread;

    @Nonnull
    private final ControlPoint mControlPoint;
    @Nonnull
    private final Map<String, Device> mDeviceMap;

    /**
     * インスタンス作成。
     *
     * @param cp ControlPoint
     */
    DeviceHolder(@Nonnull final ControlPoint cp) {
        mDeviceMap = new LinkedHashMap<>();
        mControlPoint = cp;
    }

    /**
     * スレッドを開始する。
     */
    void start() {
        mShutdownRequest = false;
        synchronized (mThreadLock) {
            mThread = new Thread(this, TAG);
            mThread.start();
        }
    }

    /**
     * スレッドに割り込みをかけ終了させる。
     */
    void shutdownRequest() {
        mShutdownRequest = true;
        synchronized (mThreadLock) {
            if (mThread != null) {
                mThread.interrupt();
                mThread = null;
            }
        }
    }

    /**
     * Device追加。
     *
     * @param device 追加されるDevice
     */
    synchronized void add(@Nonnull final Device device) {
        mDeviceMap.put(device.getUdn(), device);
        notifyAll();
    }

    @Nullable
    synchronized Device get(@Nonnull final String udn) {
        return mDeviceMap.get(udn);
    }

    /**
     * Device削除。
     *
     * @param device 削除されるDevice。
     */
    synchronized void remove(@Nonnull final Device device) {
        mDeviceMap.remove(device.getUdn());
    }

    /**
     * 登録されたDeviceをクリア。
     */
    synchronized void clear() {
        mDeviceMap.clear();
    }

    /**
     * 現在保持しているDeviceの順序を保持したリストを作成して返す。
     *
     * @return Deviceのリスト
     */
    @Nonnull
    synchronized List<Device> getDeviceList() {
        return new ArrayList<>(mDeviceMap.values());
    }

    /**
     * 保持しているDeviceの数を返す。
     *
     * @return Deviceの数
     */
    synchronized int size() {
        return mDeviceMap.size();
    }

    @Override
    public synchronized void run() {
        try {
            while (!mShutdownRequest) {
                while (mDeviceMap.size() == 0) {
                    wait();
                }
                expireDevice();
                waitNextExpireTime();
            }
        } catch (final InterruptedException ignored) {
        }
    }

    private void expireDevice() {
        final long now = System.currentTimeMillis();
        final Iterator<Device> i = mDeviceMap.values().iterator();
        while (i.hasNext()) {
            final Device device = i.next();
            if (device.getExpireTime() < now) {
                i.remove();
                mControlPoint.lostDevice(device);
            }
        }
    }

    private void waitNextExpireTime() throws InterruptedException {
        if (mDeviceMap.size() == 0) {
            return;
        }
        final long sleep = findMostRecentExpireTime() - System.currentTimeMillis() + MARGIN_TIME;
        wait(Math.max(sleep, MARGIN_TIME)); // 負の値となる可能性を排除
    }

    private long findMostRecentExpireTime() {
        long recent = Long.MAX_VALUE;
        for (final Device device : mDeviceMap.values()) {
            final long expireTime = device.getExpireTime();
            if (recent > expireTime) {
                recent = expireTime;
            }
        }
        return recent;
    }
}
