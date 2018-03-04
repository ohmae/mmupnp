/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class SubscribeServiceTest {
    @Test
    public void getService_Serviceが取得できる() throws Exception {
        final Service service = mock(Service.class);
        final SubscribeService subscribeService = new SubscribeService(service, false);

        assertThat(subscribeService.getService(), is(service));
    }

    @Test
    public void getNextScanTime_keepでないときserviceのgetSubscriptionExpiryTimeと等しい() throws Exception {
        final long expiryTime = 10000L;
        final Service service = mock(Service.class);
        doReturn(expiryTime).when(service).getSubscriptionExpiryTime();
        final SubscribeService subscribeService = new SubscribeService(service, false);

        assertThat(subscribeService.getNextScanTime(), is(expiryTime));
    }

    @Test
    public void getNextScanTime_keepである時serviceのgetSubscriptionStartとの差はgetSubscriptionTimeoutより小さい() throws Exception {
        final long start = 10000L;
        final long timeout = 1000L;
        final Service service = mock(Service.class);
        doReturn(timeout).when(service).getSubscriptionTimeout();
        doReturn(start).when(service).getSubscriptionStart();
        final SubscribeService subscribeService = new SubscribeService(service, true);

        final long time = subscribeService.getNextScanTime();
        assertThat(time, greaterThan(start));
        assertThat(time, lessThan(start + timeout));
    }

    @Test
    public void getNextScanTime_keepである時failしてもserviceのgetSubscriptionStartとの差はgetSubscriptionTimeoutより小さい() throws Exception {
        final long start = 10000L;
        final long timeout = 1000L;
        final Service service = mock(Service.class);
        doReturn(timeout).when(service).getSubscriptionTimeout();
        doReturn(start).when(service).getSubscriptionStart();
        doReturn(false).when(service).renewSubscribe();
        final SubscribeService subscribeService = new SubscribeService(service, true);

        subscribeService.renewSubscribe(subscribeService.getNextScanTime());
        final long time = subscribeService.getNextScanTime();

        assertThat(time, greaterThan(start));
        assertThat(time, lessThan(start + timeout));
    }

    @Test
    public void getNextScanTime_keepである時failCountが0のときより1のほうが大きな値になる() throws Exception {
        final long start = 10000L;
        final long timeout = 1000L;
        final Service service = mock(Service.class);
        doReturn(timeout).when(service).getSubscriptionTimeout();
        doReturn(start).when(service).getSubscriptionStart();
        doReturn(false).when(service).renewSubscribe();
        final SubscribeService subscribeService = new SubscribeService(service, true);

        final long time1 = subscribeService.getNextScanTime();
        subscribeService.renewSubscribe(time1);
        final long time2 = subscribeService.getNextScanTime();

        assertThat(time1, lessThan(time2));
    }

    @Test
    public void isFailed_2回連続失敗でtrue() throws Exception {
        final Service service = mock(Service.class);
        doReturn(0L).when(service).getSubscriptionTimeout();
        doReturn(0L).when(service).getSubscriptionStart();
        doReturn(false).when(service).renewSubscribe();
        final SubscribeService subscribeService = new SubscribeService(service, true);

        assertThat(subscribeService.isFailed(), is(false));

        subscribeService.renewSubscribe(0);
        assertThat(subscribeService.isFailed(), is(false));

        subscribeService.renewSubscribe(0);
        assertThat(subscribeService.isFailed(), is(true));
    }

    @Test
    public void isFailed_連続しない2回失敗ではfalse() throws Exception {
        final Service service = mock(Service.class);
        doReturn(0L).when(service).getSubscriptionTimeout();
        doReturn(0L).when(service).getSubscriptionStart();
        final SubscribeService subscribeService = new SubscribeService(service, true);

        assertThat(subscribeService.isFailed(), is(false));

        doReturn(false).when(service).renewSubscribe();
        subscribeService.renewSubscribe(0);
        assertThat(subscribeService.isFailed(), is(false));

        doReturn(true).when(service).renewSubscribe();
        subscribeService.renewSubscribe(0);
        assertThat(subscribeService.isFailed(), is(false));

        doReturn(false).when(service).renewSubscribe();
        subscribeService.renewSubscribe(0);
        assertThat(subscribeService.isFailed(), is(false));
    }

    @Test
    public void isExpired_期限が切れるとtrue() throws Exception {
        final long expiryTime = 10000L;
        final Service service = mock(Service.class);
        doReturn(expiryTime).when(service).getSubscriptionExpiryTime();
        final SubscribeService subscribeService = new SubscribeService(service, false);

        assertThat(subscribeService.isExpired(expiryTime), is(false));
        assertThat(subscribeService.isExpired(expiryTime + 1), is(true));
    }

    @Test
    public void renewSubscribe_keepがfalseならrenewSubscribeを呼ばない() throws Exception {
        final long start = 10000L;
        final long timeout = 1000L;
        final Service service = mock(Service.class);
        doReturn(timeout).when(service).getSubscriptionTimeout();
        doReturn(start).when(service).getSubscriptionStart();
        doReturn(true).when(service).renewSubscribe();
        final SubscribeService subscribeService = new SubscribeService(service, false);

        final long time = subscribeService.getNextScanTime();
        assertThat(subscribeService.renewSubscribe(time), is(true));
        verify(service, never()).renewSubscribe();
    }

    @Test
    public void renewSubscribe_keepがtrueでも時間の前ではrenewSubscribeを呼ばない() throws Exception {
        final long start = 10000L;
        final long timeout = 1000L;
        final Service service = mock(Service.class);
        doReturn(timeout).when(service).getSubscriptionTimeout();
        doReturn(start).when(service).getSubscriptionStart();
        doReturn(true).when(service).renewSubscribe();
        final SubscribeService subscribeService = new SubscribeService(service, true);

        final long time = subscribeService.getNextScanTime();
        assertThat(subscribeService.renewSubscribe(time - 1), is(true));
        verify(service, never()).renewSubscribe();
    }

    @Test
    public void renewSubscribe_keepがtrueで時間を過ぎていたらrenewSubscribeを呼ぶ() throws Exception {
        final long start = 10000L;
        final long timeout = 1000L;
        final Service service = mock(Service.class);
        doReturn(timeout).when(service).getSubscriptionTimeout();
        doReturn(start).when(service).getSubscriptionStart();
        doReturn(true).when(service).renewSubscribe();
        final SubscribeService subscribeService = new SubscribeService(service, true);

        final long time = subscribeService.getNextScanTime();
        assertThat(subscribeService.renewSubscribe(time), is(true));
        verify(service, times(1)).renewSubscribe();
    }

    @Test
    public void calculateRenewTime() throws Exception {
        final Service service = mock(Service.class);
        final SubscribeService subscribeService = new SubscribeService(service, false);
        doReturn(TimeUnit.SECONDS.toMillis(0)).when(service).getSubscriptionStart();
        doReturn(TimeUnit.SECONDS.toMillis(300)).when(service).getSubscriptionTimeout();

        assertThat(subscribeService.calculateRenewTime(), is(TimeUnit.SECONDS.toMillis(140)));

        doReturn(TimeUnit.SECONDS.toMillis(16)).when(service).getSubscriptionTimeout();

        assertThat(subscribeService.calculateRenewTime(), is(TimeUnit.SECONDS.toMillis(4)));
    }

    @Test
    public void renewSubscribe() throws Exception {
        final Service service = mock(Service.class);
        final SubscribeService subscribeService = new SubscribeService(service, true);
        doReturn(TimeUnit.SECONDS.toMillis(0)).when(service).getSubscriptionStart();
        doReturn(TimeUnit.SECONDS.toMillis(300)).when(service).getSubscriptionTimeout();
        doThrow(new IOException()).when(service).renewSubscribe();

        subscribeService.renewSubscribe(TimeUnit.SECONDS.toMillis(300));
        subscribeService.renewSubscribe(TimeUnit.SECONDS.toMillis(300));

        assertThat(subscribeService.isFailed(), is(true));
    }

    @Test
    public void hashCode_Serviceと同一() throws Exception {
        final Service service = mock(Service.class);
        final SubscribeService subscribeService = new SubscribeService(service, false);

        assertThat(subscribeService.hashCode(), is(service.hashCode()));
    }

    @Test
    public void equals_同一インスタンスであれば真() throws Exception {
        final Service service = mock(Service.class);
        final SubscribeService subscribeService = new SubscribeService(service, false);

        assertThat(subscribeService.equals(subscribeService), is(true));
    }

    @Test
    public void equals_Serviceが同一であれば真() throws Exception {
        final Service service = mock(Service.class);
        final SubscribeService subscribeService1 = new SubscribeService(service, false);
        final SubscribeService subscribeService2 = new SubscribeService(service, false);

        assertThat(subscribeService1.equals(subscribeService1), is(true));
        assertThat(subscribeService1.equals(subscribeService2), is(true));
    }

    @Test
    public void equals_異なるクラス() throws Exception {
        final Service service = mock(Service.class);
        final SubscribeService subscribeService = new SubscribeService(service, false);

        assertThat(subscribeService.equals(service), is(false));
        assertThat(subscribeService.equals(null), is(false));
    }
}
