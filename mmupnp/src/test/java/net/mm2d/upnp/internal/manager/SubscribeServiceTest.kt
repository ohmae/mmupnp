/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.manager

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.mm2d.upnp.Service
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit

@Suppress("TestFunctionName", "NonAsciiCharacters")
@RunWith(JUnit4::class)
class SubscribeServiceTest {
    @Test
    fun getService_Serviceが取得できる() {
        val service: Service = mockk(relaxed = true)
        val subscribeService = SubscribeService(service, 1000, false)

        assertThat(subscribeService.getService()).isEqualTo(service)
    }

    @Test
    fun getNextScanTime_keepでないとき開始時間とタイムアウトの合計に等しい() {
        val service: Service = mockk(relaxed = true)
        val timeout = 1000L
        val start = System.currentTimeMillis()
        val subscribeService = SubscribeService(service, timeout, false)

        assertThat(subscribeService.getNextScanTime() - (start + timeout)).isLessThan(100L)
    }

    @Test
    fun getNextScanTime_keepである時serviceのgetSubscriptionStartとの差はgetSubscriptionTimeoutより小さい() {
        val service: Service = mockk(relaxed = true)
        val start = System.currentTimeMillis()
        val timeout = 1000L
        val subscribeService = SubscribeService(service, timeout, true)

        val time = subscribeService.getNextScanTime()
        assertThat(time).isGreaterThan(start)
        assertThat(time).isLessThan(start + timeout)
    }

    @Test
    fun getNextScanTime_keepである時failしてもserviceのgetSubscriptionStartとの差はgetSubscriptionTimeoutより小さい() {
        val service: Service = mockk(relaxed = true)
        val start = System.currentTimeMillis()
        val timeout = 1000L
        every { service.renewSubscribeSync() } returns false
        val subscribeService = SubscribeService(service, timeout, true)

        subscribeService.renewSubscribe(subscribeService.getNextScanTime())
        val time = subscribeService.getNextScanTime()

        assertThat(time).isGreaterThan(start)
        assertThat(time).isLessThan(start + timeout)
    }

    @Test
    fun getNextScanTime_keepである時failCountが0のときより1のほうが大きな値になる() {
        val timeout = 1000L
        val service: Service = mockk(relaxed = true)
        every { service.renewSubscribeSync() } returns false
        val subscribeService = SubscribeService(service, timeout, true)

        val time1 = subscribeService.getNextScanTime()
        subscribeService.renewSubscribe(time1)
        val time2 = subscribeService.getNextScanTime()

        assertThat(time1).isLessThan(time2)
    }

    @Test
    fun isFailed_2回連続失敗でtrue() {
        val service: Service = mockk(relaxed = true)
        every { service.renewSubscribeSync() } returns false
        val subscribeService = SubscribeService(service, 0L, true)
        val start = System.currentTimeMillis()

        assertThat(subscribeService.isFailed()).isFalse()

        subscribeService.renewSubscribe(start)
        assertThat(subscribeService.isFailed()).isFalse()

        subscribeService.renewSubscribe(start)
        assertThat(subscribeService.isFailed()).isTrue()
    }

    @Test
    fun isFailed_連続しない2回失敗ではfalse() {
        val service: Service = mockk(relaxed = true)
        val subscribeService = SubscribeService(service, 0L, true)

        assertThat(subscribeService.isFailed()).isFalse()

        every { service.renewSubscribeSync() } returns false
        subscribeService.renewSubscribe(0)
        assertThat(subscribeService.isFailed()).isFalse()

        every { service.renewSubscribeSync() } returns true
        subscribeService.renewSubscribe(0)
        assertThat(subscribeService.isFailed()).isFalse()

        every { service.renewSubscribeSync() } returns false
        subscribeService.renewSubscribe(0)
        assertThat(subscribeService.isFailed()).isFalse()
    }

    @Test
    fun isExpired_期限が切れるとtrue() {
        val service: Service = mockk(relaxed = true)
        val start = System.currentTimeMillis()
        val timeout = 1000L
        val expiryTime = start + timeout
        val subscribeService = SubscribeService(service, timeout, false)

        assertThat(subscribeService.isExpired(expiryTime)).isFalse()
        assertThat(subscribeService.isExpired(expiryTime + 100L)).isTrue()
    }

    @Test
    fun renewSubscribe_keepがfalseならrenewSubscribeを呼ばない() {
        val timeout = 1000L
        val service: Service = mockk(relaxed = true)
        every { service.renewSubscribeSync() } returns true
        val subscribeService = SubscribeService(service, timeout, false)

        val time = subscribeService.getNextScanTime()
        assertThat(subscribeService.renewSubscribe(time)).isTrue()
        verify(inverse = true) { service.renewSubscribeSync() }
    }

    @Test
    fun renewSubscribe_keepがtrueでも時間の前ではrenewSubscribeを呼ばない() {
        val timeout = 1000L
        val service: Service = mockk(relaxed = true)
        every { service.renewSubscribeSync() } returns true
        val subscribeService = SubscribeService(service, timeout, true)

        val time = subscribeService.getNextScanTime()
        assertThat(subscribeService.renewSubscribe(time - 1)).isTrue()
        verify(inverse = true) { service.renewSubscribeSync() }
    }

    @Test
    fun renewSubscribe_keepがtrueで時間を過ぎていたらrenewSubscribeを呼ぶ() {
        val timeout = 1000L
        val service: Service = mockk(relaxed = true)
        every { service.renewSubscribeSync() } returns true
        val subscribeService = SubscribeService(service, timeout, true)

        val time = subscribeService.getNextScanTime()
        assertThat(subscribeService.renewSubscribe(time)).isTrue()
        verify(exactly = 1) { service.renewSubscribeSync() }
    }

    @Test
    fun calculateRenewTime() {
        val service: Service = mockk(relaxed = true)
        val start = System.currentTimeMillis()
        val subscribeService = SubscribeService(service, TimeUnit.SECONDS.toMillis(300), false)

        assertThat(subscribeService.calculateRenewTime() - start - TimeUnit.SECONDS.toMillis(140)).isLessThan(100L)

        subscribeService.renew(TimeUnit.SECONDS.toMillis(16))

        assertThat(subscribeService.calculateRenewTime() - start - TimeUnit.SECONDS.toMillis(4)).isLessThan(100L)
    }

    @Test
    fun renewSubscribe() {
        val service: Service = mockk(relaxed = true)
        val subscribeService = SubscribeService(service, TimeUnit.SECONDS.toMillis(300), true)
        val start = System.currentTimeMillis()

        subscribeService.renewSubscribe(TimeUnit.SECONDS.toMillis(300) + start)
        subscribeService.renewSubscribe(TimeUnit.SECONDS.toMillis(300) + start)

        assertThat(subscribeService.isFailed()).isTrue()
    }

    @Test
    fun hashCode_Serviceと同一() {
        val service: Service = mockk(relaxed = true)
        val subscribeService = SubscribeService(service, TimeUnit.SECONDS.toMillis(300), false)

        assertThat(subscribeService.hashCode()).isEqualTo(service.hashCode())
    }

    @Test
    fun equals_同一インスタンスであれば真() {
        val service: Service = mockk(relaxed = true)
        val subscribeService = SubscribeService(service, TimeUnit.SECONDS.toMillis(300), false)

        assertThat(subscribeService == subscribeService).isTrue()
    }

    @Test
    fun equals_Serviceが同一であれば真() {
        val service: Service = mockk(relaxed = true)
        val subscribeService1 = SubscribeService(service, TimeUnit.SECONDS.toMillis(300), false)
        val subscribeService2 = SubscribeService(service, TimeUnit.SECONDS.toMillis(300), false)

        assertThat(subscribeService1 == subscribeService1).isTrue()
        assertThat(subscribeService1 == subscribeService2).isTrue()
    }
}
