/*
 * Copyright(c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import java.io.IOException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SubscribeServiceTest {
    public void getService_Serviceが取得できる() {
        final Service service = mock(Service.class);
        final SubscribeService subscribeService = new SubscribeService(service, false);

        assertThat(subscribeService.getService(), is(service));
    }

    public void getNextScanTime_keepでないときserviceのgetSubscriptionExpiryTimeと等しい() {
        final long expiryTime = 10000L;
        final Service service = mock(Service.class);
        doReturn(expiryTime).when(service).getSubscriptionExpiryTime();
        final SubscribeService subscribeService = new SubscribeService(service, false);

        assertThat(subscribeService.getNextScanTime(), is(expiryTime));
    }

    public void getNextScanTime_keepである時serviceのgetSubscriptionStartとの差はgetSubscriptionTimeoutより小さい() {
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

    public void getNextScanTime_keepである時failしてもserviceのgetSubscriptionStartとの差はgetSubscriptionTimeoutより小さい() throws IOException {
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

    public void getNextScanTime_keepである時failCountが0のときより1のほうが大きな値になる() throws IOException {
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

    public void isFailed_2回連続失敗でtrue() throws IOException {
        final Service service = mock(Service.class);
        doReturn(0).when(service).getSubscriptionTimeout();
        doReturn(0).when(service).getSubscriptionStart();
        doReturn(false).when(service).renewSubscribe();
        final SubscribeService subscribeService = new SubscribeService(service, true);

        assertThat(subscribeService.isFailed(), is(false));

        subscribeService.renewSubscribe(0);
        assertThat(subscribeService.isFailed(), is(false));

        subscribeService.renewSubscribe(0);
        assertThat(subscribeService.isFailed(), is(true));
    }

    public void isFailed_連続しない2回失敗ではfalse() throws IOException {
        final Service service = mock(Service.class);
        doReturn(0).when(service).getSubscriptionTimeout();
        doReturn(0).when(service).getSubscriptionStart();
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

    public void isExpired_期限が切れるとtrue() {
        final long expiryTime = 10000L;
        final Service service = mock(Service.class);
        doReturn(expiryTime).when(service).getSubscriptionExpiryTime();
        final SubscribeService subscribeService = new SubscribeService(service, false);

        assertThat(subscribeService.isExpired(expiryTime - 1), is(false));
        assertThat(subscribeService.isExpired(expiryTime), is(true));
    }

    public void renewSubscribe_keepがfalseならrenewSubscribeを呼ばない() throws IOException {
        final long start = 10000L;
        final long timeout = 1000L;
        final Service service = mock(Service.class);
        doReturn(timeout).when(service).getSubscriptionTimeout();
        doReturn(start).when(service).getSubscriptionStart();
        doReturn(true).when(service).renewSubscribe();
        final SubscribeService subscribeService = new SubscribeService(service, false);

        final long time = subscribeService.getNextScanTime();
        assertThat(subscribeService.renewSubscribe(time), is(true));
        verify(service, times(0)).renewSubscribe();
    }

    public void renewSubscribe_keepがtrueでも時間の前ではrenewSubscribeを呼ばない() throws IOException {
        final long start = 10000L;
        final long timeout = 1000L;
        final Service service = mock(Service.class);
        doReturn(timeout).when(service).getSubscriptionTimeout();
        doReturn(start).when(service).getSubscriptionStart();
        doReturn(true).when(service).renewSubscribe();
        final SubscribeService subscribeService = new SubscribeService(service, false);

        final long time = subscribeService.getNextScanTime();
        assertThat(subscribeService.renewSubscribe(time - 1), is(true));
        verify(service, times(0)).renewSubscribe();
    }


    public void renewSubscribe_keepがtrueで時間を過ぎていたらrenewSubscribeを呼ぶ() throws IOException {
        final long start = 10000L;
        final long timeout = 1000L;
        final Service service = mock(Service.class);
        doReturn(timeout).when(service).getSubscriptionTimeout();
        doReturn(start).when(service).getSubscriptionStart();
        doReturn(true).when(service).renewSubscribe();
        final SubscribeService subscribeService = new SubscribeService(service, false);

        final long time = subscribeService.getNextScanTime();
        assertThat(subscribeService.renewSubscribe(time - 1), is(true));
        verify(service, times(1)).renewSubscribe();
    }
}
