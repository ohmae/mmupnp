/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager;

import net.mm2d.upnp.Service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
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
        doReturn(id).when(service).getSubscriptionId();
        final SubscribeHolder subscribeHolder = new SubscribeHolder();

        subscribeHolder.add(service, 1000L, false);

        assertThat(subscribeHolder.getService(id), is(service));
    }

    @Test
    public void remove_getServiceできない() {
        final String id1 = "id1";
        final Service service1 = mock(Service.class);
        doReturn(id1).when(service1).getSubscriptionId();
        final String id2 = "id2";
        final Service service2 = mock(Service.class);
        doReturn(id2).when(service2).getSubscriptionId();
        final SubscribeHolder subscribeHolder = new SubscribeHolder();

        subscribeHolder.add(service1, 1000L, false);
        subscribeHolder.add(service2, 1000L, false);

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
        doReturn(id1).when(service1).getSubscriptionId();
        final String id2 = "id2";
        final Service service2 = mock(Service.class);
        doReturn(id2).when(service2).getSubscriptionId();
        final SubscribeHolder subscribeHolder = new SubscribeHolder();

        subscribeHolder.add(service1, 1000L, false);
        subscribeHolder.add(service2, 1000L, false);

        assertThat(subscribeHolder.getServiceList(), hasItem(service1));
        assertThat(subscribeHolder.getServiceList(), hasItem(service2));

        subscribeHolder.remove(service1);

        assertThat(subscribeHolder.getServiceList(), not(hasItem(service1)));
        assertThat(subscribeHolder.getServiceList(), hasItem(service2));
    }

    @Test
    public void getServiceList_subscriptionIdがnullだとaddできない() {
        final Service service1 = mock(Service.class);
        final SubscribeHolder subscribeHolder = new SubscribeHolder();
        subscribeHolder.add(service1, 1000L, false);

        assertThat(subscribeHolder.getServiceList(), not(hasItem(service1)));
    }

    @Test
    public void clear_すべて取得できなくなる() {
        final String id1 = "id1";
        final Service service1 = mock(Service.class);
        doReturn(id1).when(service1).getSubscriptionId();
        final String id2 = "id2";
        final Service service2 = mock(Service.class);
        doReturn(id2).when(service2).getSubscriptionId();
        final SubscribeHolder subscribeHolder = new SubscribeHolder();

        subscribeHolder.add(service1, 1000L, false);
        subscribeHolder.add(service2, 1000L, false);

        assertThat(subscribeHolder.getService(id1), is(service1));
        assertThat(subscribeHolder.getService(id2), is(service2));

        subscribeHolder.clear();

        assertThat(subscribeHolder.getService(id1), is(nullValue()));
        assertThat(subscribeHolder.getService(id2), is(nullValue()));
    }

    @Test(timeout = 10000L)
    public void expire_時間経過で削除される() throws InterruptedException {
        final long now = System.currentTimeMillis();
        final String id1 = "id1";
        final Service service1 = mock(Service.class);
        doReturn(id1).when(service1).getSubscriptionId();
        final String id2 = "id2";
        final Service service2 = mock(Service.class);
        doReturn(id2).when(service2).getSubscriptionId();
        final SubscribeHolder subscribeHolder = new SubscribeHolder();
        subscribeHolder.start();

        subscribeHolder.add(service1, 1000L, false);
        subscribeHolder.add(service2, 4000L, false);

        assertThat(subscribeHolder.getService(id1), is(service1));
        assertThat(subscribeHolder.getService(id2), is(service2));

        Thread.sleep(3000L);

        assertThat(subscribeHolder.getService(id1), is(nullValue()));
        assertThat(subscribeHolder.getService(id2), is(service2));

        Thread.sleep(3000L);

        assertThat(subscribeHolder.getService(id1), is(nullValue()));
        assertThat(subscribeHolder.getService(id2), is(nullValue()));

        subscribeHolder.shutdownRequest();
    }

    @Test(timeout = 10000L)
    public void renew_定期的にrenewが実行される() throws Exception {
        final long now = System.currentTimeMillis();
        final Service service1 = mock(Service.class);
        doReturn(true).when(service1).renewSubscribe();
        doReturn("id1").when(service1).getSubscriptionId();

        final Service service2 = mock(Service.class);
        doReturn(true).when(service2).renewSubscribe();
        doReturn("id2").when(service2).getSubscriptionId();

        final SubscribeHolder subscribeHolder = new SubscribeHolder();
        subscribeHolder.start();

        subscribeHolder.add(service1, 1000L, true);
        subscribeHolder.add(service2, 500L, true);
        verify(service1, never()).renewSubscribe();

        Thread.sleep(2000L);
        verify(service1, atLeastOnce()).renewSubscribe();

        subscribeHolder.shutdownRequest();
    }


    @Test(timeout = 10000L)
    public void renew_失敗したら削除される() throws Exception {
        final long now = System.currentTimeMillis();
        final String id = "id";
        final Service service = mock(Service.class);
        doReturn(false).when(service).renewSubscribe();
        doReturn(id).when(service).getSubscriptionId();
        final SubscribeHolder subscribeHolder = new SubscribeHolder();
        subscribeHolder.start();

        subscribeHolder.add(service, 1000L, true);
        Thread.sleep(3000L);
        assertThat(subscribeHolder.getService(id), is(nullValue()));

        subscribeHolder.shutdownRequest();
    }
}
