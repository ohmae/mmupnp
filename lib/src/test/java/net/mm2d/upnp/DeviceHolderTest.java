/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.DeviceHolder.ExpireListener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class DeviceHolderTest {
    private static final String UDN = "uuid:01234567-89ab-cdef-0123-456789abcdef";

    @Test(timeout = 1000L)
    public void start_shutdown_デッドロックしない() throws Exception {
        final DeviceHolder holder = new DeviceHolder(mock(ExpireListener.class));
        holder.start();
        Thread.sleep(1);
        holder.shutdownRequest();
        holder.shutdownRequest();
    }

    @Test
    public void add() throws Exception {
        final DeviceHolder holder = new DeviceHolder(mock(ExpireListener.class));
        final Device device = mock(Device.class);
        doReturn(UDN).when(device).getUdn();
        holder.add(device);

        assertThat(holder.get(UDN), sameInstance(device));
    }

    @Test
    public void remove() throws Exception {
        final DeviceHolder holder = new DeviceHolder(mock(ExpireListener.class));
        final Device device = mock(Device.class);
        doReturn(UDN).when(device).getUdn();
        holder.add(device);

        assertThat(holder.get(UDN), sameInstance(device));

        holder.remove(device);
        assertThat(holder.get(UDN), is(nullValue()));
    }

    @Test
    public void clear() throws Exception {
        final DeviceHolder holder = new DeviceHolder(mock(ExpireListener.class));
        final Device device = mock(Device.class);
        doReturn(UDN).when(device).getUdn();
        holder.add(device);

        assertThat(holder.get(UDN), sameInstance(device));

        holder.clear();
        assertThat(holder.getDeviceList(), hasSize(0));
    }

    @Test
    public void getDeviceList() throws Exception {
        final DeviceHolder holder = new DeviceHolder(mock(ExpireListener.class));
        final Device device = mock(Device.class);
        doReturn(UDN).when(device).getUdn();
        holder.add(device);

        assertThat(holder.getDeviceList(), hasSize(1));
        assertThat(holder.getDeviceList(), hasItem(device));
    }

    @Test
    public void size() throws Exception {
        final DeviceHolder holder = new DeviceHolder(mock(ExpireListener.class));
        final Device device = mock(Device.class);
        doReturn(UDN).when(device).getUdn();
        holder.add(device);

        assertThat(holder.size(), is(1));
    }

    @Test(timeout = 1000)
    public void shutdownRequest() throws Exception {
        final ExpireListener expireListener = mock(ExpireListener.class);
        final DeviceHolder holder = new DeviceHolder(expireListener);

        holder.shutdownRequest();
        holder.run();
    }

    @Test(timeout = 20000L)
    public void expireDevice_時間経過後に削除される() throws Exception {
        final ExpireListener expireListener = mock(ExpireListener.class);
        final DeviceHolder holder = new DeviceHolder(expireListener);
        final Device device1 = mock(Device.class);
        doReturn(UDN).when(device1).getUdn();
        final Device device2 = mock(Device.class);
        doReturn(UDN + "2").when(device2).getUdn();

        doReturn(System.currentTimeMillis() + 100L).when(device1).getExpireTime();
        doReturn(System.currentTimeMillis() + 200L).when(device2).getExpireTime();

        holder.start();
        Thread.sleep(1);
        holder.add(device1);
        holder.add(device2);

        assertThat(holder.size(), is(2));

        Thread.sleep(11000L); // 内部で10秒のマージンを持っているため十分な時間を開ける

        assertThat(holder.size(), is(0));
        verify(expireListener).onExpire(device1);
        holder.shutdownRequest();
    }
}
