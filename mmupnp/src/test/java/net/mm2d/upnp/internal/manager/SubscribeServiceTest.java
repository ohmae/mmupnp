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

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class SubscribeServiceTest {
    @Test
    public void getService_Serviceが取得できる() {
        final Service service = mock(Service.class);
        final SubscribeService subscribeService = new SubscribeService(service, 1000, false);

        assertThat(subscribeService.getService(), is(service));
    }

    @Test
    public void getNextScanTime_keepでないとき開始時間とタイムアウトの合計に等しい() {
        final Service service = mock(Service.class);
        final long timeout = 1000L;
        final long start = System.currentTimeMillis();
        final SubscribeService subscribeService = new SubscribeService(service, timeout, false);

        assertThat(subscribeService.getNextScanTime() - (start + timeout), lessThan(100L));
    }

    @Test
    public void getNextScanTime_keepである時serviceのgetSubscriptionStartとの差はgetSubscriptionTimeoutより小さい() {
        final Service service = mock(Service.class);
        final long start = System.currentTimeMillis();
        final long timeout = 1000L;
        final SubscribeService subscribeService = new SubscribeService(service, timeout, true);

        final long time = subscribeService.getNextScanTime();
        assertThat(time, greaterThan(start));
        assertThat(time, lessThan(start + timeout));
    }

    @Test
    public void getNextScanTime_keepである時failしてもserviceのgetSubscriptionStartとの差はgetSubscriptionTimeoutより小さい() {
        final Service service = mock(Service.class);
        final long start = System.currentTimeMillis();
        final long timeout = 1000L;
        doReturn(false).when(service).renewSubscribeSync();
        final SubscribeService subscribeService = new SubscribeService(service, timeout, true);

        subscribeService.renewSubscribe(subscribeService.getNextScanTime());
        final long time = subscribeService.getNextScanTime();

        assertThat(time, greaterThan(start));
        assertThat(time, lessThan(start + timeout));
    }

    @Test
    public void getNextScanTime_keepである時failCountが0のときより1のほうが大きな値になる() {
        final long timeout = 1000L;
        final Service service = mock(Service.class);
        doReturn(false).when(service).renewSubscribeSync();
        final SubscribeService subscribeService = new SubscribeService(service, timeout, true);

        final long time1 = subscribeService.getNextScanTime();
        subscribeService.renewSubscribe(time1);
        final long time2 = subscribeService.getNextScanTime();

        assertThat(time1, lessThan(time2));
    }

    @Test
    public void isFailed_2回連続失敗でtrue() {
        final Service service = mock(Service.class);
        doReturn(false).when(service).renewSubscribeSync();
        final SubscribeService subscribeService = new SubscribeService(service, 0L, true);
        final long start = System.currentTimeMillis();

        assertThat(subscribeService.isFailed(), is(false));

        subscribeService.renewSubscribe(start);
        assertThat(subscribeService.isFailed(), is(false));

        subscribeService.renewSubscribe(start);
        assertThat(subscribeService.isFailed(), is(true));
    }

    @Test
    public void isFailed_連続しない2回失敗ではfalse() {
        final Service service = mock(Service.class);
        final SubscribeService subscribeService = new SubscribeService(service, 0L, true);

        assertThat(subscribeService.isFailed(), is(false));

        doReturn(false).when(service).renewSubscribeSync();
        subscribeService.renewSubscribe(0);
        assertThat(subscribeService.isFailed(), is(false));

        doReturn(true).when(service).renewSubscribeSync();
        subscribeService.renewSubscribe(0);
        assertThat(subscribeService.isFailed(), is(false));

        doReturn(false).when(service).renewSubscribeSync();
        subscribeService.renewSubscribe(0);
        assertThat(subscribeService.isFailed(), is(false));
    }

    @Test
    public void isExpired_期限が切れるとtrue() {
        final Service service = mock(Service.class);
        final long start = System.currentTimeMillis();
        final long timeout = 1000L;
        final long expiryTime = start + timeout;
        final SubscribeService subscribeService = new SubscribeService(service, timeout, false);

        assertThat(subscribeService.isExpired(expiryTime), is(false));
        assertThat(subscribeService.isExpired(expiryTime + 100L), is(true));
    }

    @Test
    public void renewSubscribe_keepがfalseならrenewSubscribeを呼ばない() {
        final long timeout = 1000L;
        final Service service = mock(Service.class);
        doReturn(true).when(service).renewSubscribeSync();
        final SubscribeService subscribeService = new SubscribeService(service, timeout, false);

        final long time = subscribeService.getNextScanTime();
        assertThat(subscribeService.renewSubscribe(time), is(true));
        verify(service, never()).renewSubscribeSync();
    }

    @Test
    public void renewSubscribe_keepがtrueでも時間の前ではrenewSubscribeを呼ばない() {
        final long timeout = 1000L;
        final Service service = mock(Service.class);
        doReturn(true).when(service).renewSubscribeSync();
        final SubscribeService subscribeService = new SubscribeService(service, timeout, true);

        final long time = subscribeService.getNextScanTime();
        assertThat(subscribeService.renewSubscribe(time - 1), is(true));
        verify(service, never()).renewSubscribeSync();
    }

    @Test
    public void renewSubscribe_keepがtrueで時間を過ぎていたらrenewSubscribeを呼ぶ() {
        final long timeout = 1000L;
        final Service service = mock(Service.class);
        doReturn(true).when(service).renewSubscribeSync();
        final SubscribeService subscribeService = new SubscribeService(service, timeout, true);

        final long time = subscribeService.getNextScanTime();
        assertThat(subscribeService.renewSubscribe(time), is(true));
        verify(service, times(1)).renewSubscribeSync();
    }

    @Test
    public void calculateRenewTime() {
        final Service service = mock(Service.class);
        final long start = System.currentTimeMillis();
        final SubscribeService subscribeService = new SubscribeService(service, TimeUnit.SECONDS.toMillis(300), false);

        assertThat(subscribeService.calculateRenewTime() - start - TimeUnit.SECONDS.toMillis(140), lessThan(100L));

        subscribeService.renew(TimeUnit.SECONDS.toMillis(16));

        assertThat(subscribeService.calculateRenewTime() - start - TimeUnit.SECONDS.toMillis(4), lessThan(100L));
    }

    @Test
    public void renewSubscribe() {
        final Service service = mock(Service.class);
        final SubscribeService subscribeService = new SubscribeService(service, TimeUnit.SECONDS.toMillis(300), true);
        final long start = System.currentTimeMillis();

        subscribeService.renewSubscribe(TimeUnit.SECONDS.toMillis(300) + start);
        subscribeService.renewSubscribe(TimeUnit.SECONDS.toMillis(300) + start);

        assertThat(subscribeService.isFailed(), is(true));
    }

    @Test
    public void hashCode_Serviceと同一() {
        final Service service = mock(Service.class);
        final SubscribeService subscribeService = new SubscribeService(service, TimeUnit.SECONDS.toMillis(300), false);

        assertThat(subscribeService.hashCode(), is(service.hashCode()));
    }

    @Test
    public void equals_同一インスタンスであれば真() {
        final Service service = mock(Service.class);
        final SubscribeService subscribeService = new SubscribeService(service, TimeUnit.SECONDS.toMillis(300), false);

        assertThat(subscribeService.equals(subscribeService), is(true));
    }

    @Test
    public void equals_Serviceが同一であれば真() {
        final Service service = mock(Service.class);
        final SubscribeService subscribeService1 = new SubscribeService(service, TimeUnit.SECONDS.toMillis(300), false);
        final SubscribeService subscribeService2 = new SubscribeService(service, TimeUnit.SECONDS.toMillis(300), false);

        assertThat(subscribeService1.equals(subscribeService1), is(true));
        assertThat(subscribeService1.equals(subscribeService2), is(true));
    }

    @Test
    public void equals_異なるクラス() {
        final Service service = mock(Service.class);
        final SubscribeService subscribeService = new SubscribeService(service, TimeUnit.SECONDS.toMillis(300), false);

        assertThat(subscribeService.equals(service), is(false));
        assertThat(subscribeService.equals(null), is(false));
    }
}
