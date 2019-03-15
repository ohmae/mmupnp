/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager;

import net.mm2d.upnp.Device;

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
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class DeviceHolder implements Runnable {
    private static final long MARGIN_TIME = TimeUnit.SECONDS.toMillis(10);

    public interface ExpireListener {
        void onExpire(@Nonnull Device device);
    }

    @Nonnull
    private final Object mThreadLock = new Object();
    private volatile boolean mShutdownRequest = false;
    @Nullable
    private Thread mThread;

    @Nonnull
    private final ExpireListener mExpireListener;
    @Nonnull
    private final Map<String, Device> mDeviceMap;

    /**
     * インスタンス作成。
     *
     * @param listener 期限切れの通知を受け取るリスナー
     */
    public DeviceHolder(@Nonnull final ExpireListener listener) {
        mDeviceMap = new LinkedHashMap<>();
        mExpireListener = listener;
    }

    /**
     * スレッドを開始する。
     */
    public void start() {
        mShutdownRequest = false;
        synchronized (mThreadLock) {
            mThread = new Thread(this, getClass().getSimpleName());
            mThread.start();
        }
    }

    /**
     * スレッドに割り込みをかけ終了させる。
     */
    public void shutdownRequest() {
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
    public synchronized void add(@Nonnull final Device device) {
        mDeviceMap.put(device.getUdn(), device);
        notifyAll();
    }

    @Nullable
    public synchronized Device get(@Nonnull final String udn) {
        return mDeviceMap.get(udn);
    }

    /**
     * Device削除。
     *
     * @param device 削除されるDevice。
     * @return 削除されたDevice
     */
    public synchronized Device remove(@Nonnull final Device device) {
        return mDeviceMap.remove(device.getUdn());
    }

    /**
     * Device削除。
     *
     * @param udn 削除されるDeviceのudn。
     * @return 削除されたDevice
     */
    public synchronized Device remove(@Nonnull final String udn) {
        return mDeviceMap.remove(udn);
    }

    /**
     * 登録されたDeviceをクリア。
     */
    public synchronized void clear() {
        mDeviceMap.clear();
    }

    /**
     * 現在保持しているDeviceの順序を保持したリストを作成して返す。
     *
     * @return Deviceのリスト
     */
    @Nonnull
    public synchronized List<Device> getDeviceList() {
        return new ArrayList<>(mDeviceMap.values());
    }

    /**
     * 保持しているDeviceの数を返す。
     *
     * @return Deviceの数
     */
    public synchronized int size() {
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
                mExpireListener.onExpire(device);
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
