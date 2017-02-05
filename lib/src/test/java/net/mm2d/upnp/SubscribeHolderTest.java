/*
 * Copyright(c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class SubscribeHolderTest {
    @Test(timeout = 100L)
    public void start_shutdownRequest_でロックしない() {
        final SubscribeHolder subscribeHolder = new SubscribeHolder();
        subscribeHolder.start();
        subscribeHolder.shutdownRequest();
    }

    @Test
    public void add_getServiceできる() {
        final String id = "id";
        final Service service = mock(Service.class);
        doReturn(Long.MAX_VALUE).when(service).getSubscriptionExpiryTime();
        doReturn(id).when(service).getSubscriptionId();
        final SubscribeHolder subscribeHolder = new SubscribeHolder();

        subscribeHolder.add(service, false);

        assertThat(subscribeHolder.getService(id), is(service));
    }

    @Test
    public void remove_getServiceできない() {
        final String id1 = "id1";
        final Service service1 = mock(Service.class);
        doReturn(Long.MAX_VALUE).when(service1).getSubscriptionExpiryTime();
        doReturn(id1).when(service1).getSubscriptionId();
        final String id2 = "id2";
        final Service service2 = mock(Service.class);
        doReturn(Long.MAX_VALUE).when(service2).getSubscriptionExpiryTime();
        doReturn(id2).when(service2).getSubscriptionId();
        final SubscribeHolder subscribeHolder = new SubscribeHolder();

        subscribeHolder.add(service1, false);
        subscribeHolder.add(service2, false);

        assertThat(subscribeHolder.getService(id1), is(service1));
        assertThat(subscribeHolder.getService(id2), is(service2));

        subscribeHolder.remove(service1);

        assertThat(subscribeHolder.getService(id1), is(nullValue()));
        assertThat(subscribeHolder.getService(id2), is(service2));
    }

    @Test
    public void getServiceList_addしたServiceが取得できる() {
        final String id1 = "id1";
        final Service service1 = mock(Service.class);
        doReturn(Long.MAX_VALUE).when(service1).getSubscriptionExpiryTime();
        doReturn(id1).when(service1).getSubscriptionId();
        final String id2 = "id2";
        final Service service2 = mock(Service.class);
        doReturn(Long.MAX_VALUE).when(service2).getSubscriptionExpiryTime();
        doReturn(id2).when(service2).getSubscriptionId();
        final SubscribeHolder subscribeHolder = new SubscribeHolder();

        subscribeHolder.add(service1, false);
        subscribeHolder.add(service2, false);

        assertThat(subscribeHolder.getServiceList(), hasItem(service1));
        assertThat(subscribeHolder.getServiceList(), hasItem(service2));

        subscribeHolder.remove(service1);

        assertThat(subscribeHolder.getServiceList(), not(hasItem(service1)));
        assertThat(subscribeHolder.getServiceList(), hasItem(service2));
    }

    @Test
    public void clear_すべて取得できなくなる() {
        final String id1 = "id1";
        final Service service1 = mock(Service.class);
        doReturn(Long.MAX_VALUE).when(service1).getSubscriptionExpiryTime();
        doReturn(id1).when(service1).getSubscriptionId();
        final String id2 = "id2";
        final Service service2 = mock(Service.class);
        doReturn(Long.MAX_VALUE).when(service2).getSubscriptionExpiryTime();
        doReturn(id2).when(service2).getSubscriptionId();
        final SubscribeHolder subscribeHolder = new SubscribeHolder();

        subscribeHolder.add(service1, false);
        subscribeHolder.add(service2, false);

        assertThat(subscribeHolder.getService(id1), is(service1));
        assertThat(subscribeHolder.getService(id2), is(service2));

        subscribeHolder.clear();

        assertThat(subscribeHolder.getService(id1), is(nullValue()));
        assertThat(subscribeHolder.getService(id2), is(nullValue()));
    }

    @Test(timeout = 1000L)
    public void expire_時間経過で削除される() throws InterruptedException {
        final long now = System.currentTimeMillis();
        final String id1 = "id1";
        final Service service1 = mock(Service.class);
        doReturn(now + 100).when(service1).getSubscriptionExpiryTime();
        doReturn(id1).when(service1).getSubscriptionId();
        final String id2 = "id2";
        final Service service2 = mock(Service.class);
        doReturn(now + 200).when(service2).getSubscriptionExpiryTime();
        doReturn(id2).when(service2).getSubscriptionId();
        final SubscribeHolder subscribeHolder = new SubscribeHolder();
        subscribeHolder.start();

        subscribeHolder.add(service1, false);
        subscribeHolder.add(service2, false);

        assertThat(subscribeHolder.getService(id1), is(service1));
        assertThat(subscribeHolder.getService(id2), is(service2));

        Thread.sleep(150L);

        assertThat(subscribeHolder.getService(id1), is(nullValue()));
        assertThat(subscribeHolder.getService(id2), is(service2));

        Thread.sleep(100L);

        assertThat(subscribeHolder.getService(id1), is(nullValue()));
        assertThat(subscribeHolder.getService(id2), is(nullValue()));

        subscribeHolder.shutdownRequest();
    }

    @Test(timeout = 1000L)
    public void renew_定期的にrenewが実行される() throws Exception {
        final long now = System.currentTimeMillis();
        final String id = "id";
        final Service service = mock(Service.class);
        doReturn(Long.MAX_VALUE).when(service).getSubscriptionExpiryTime();
        doReturn(now).when(service).getSubscriptionStart();
        doReturn(100L).when(service).getSubscriptionTimeout();
        doReturn(true).when(service).renewSubscribe();
        doReturn(id).when(service).getSubscriptionId();
        final SubscribeHolder subscribeHolder = new SubscribeHolder();
        subscribeHolder.start();

        subscribeHolder.add(service, true);
        verify(service, never()).renewSubscribe();

        Thread.sleep(100L);
        verify(service).renewSubscribe();

        subscribeHolder.shutdownRequest();
    }


    @Test(timeout = 1000L)
    public void renew_失敗したら削除される() throws Exception {
        final long now = System.currentTimeMillis();
        final String id = "id";
        final Service service = mock(Service.class);
        doReturn(Long.MAX_VALUE).when(service).getSubscriptionExpiryTime();
        doReturn(now).when(service).getSubscriptionStart();
        doReturn(100L).when(service).getSubscriptionTimeout();
        doReturn(false).when(service).renewSubscribe();
        doReturn(id).when(service).getSubscriptionId();
        final SubscribeHolder subscribeHolder = new SubscribeHolder();
        subscribeHolder.start();

        subscribeHolder.add(service, true);
        Thread.sleep(250L);
        assertThat(subscribeHolder.getService(id), is(nullValue()));

        subscribeHolder.shutdownRequest();
    }
}
