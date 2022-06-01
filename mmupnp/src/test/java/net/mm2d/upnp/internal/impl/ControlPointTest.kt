/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl

import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import net.mm2d.upnp.Adapter.discoveryListener
import net.mm2d.upnp.Adapter.eventListener
import net.mm2d.upnp.Adapter.iconFilter
import net.mm2d.upnp.Adapter.multicastEventListener
import net.mm2d.upnp.Adapter.notifyEventListener
import net.mm2d.upnp.Adapter.taskExecutor
import net.mm2d.upnp.ControlPoint.DiscoveryListener
import net.mm2d.upnp.ControlPoint.EventListener
import net.mm2d.upnp.ControlPoint.NotifyEventListener
import net.mm2d.upnp.ControlPointFactory
import net.mm2d.upnp.Device
import net.mm2d.upnp.SingleHttpClient
import net.mm2d.upnp.Protocol
import net.mm2d.upnp.Service
import net.mm2d.upnp.SsdpMessage
import net.mm2d.upnp.StateVariable
import net.mm2d.upnp.internal.manager.DeviceHolder
import net.mm2d.upnp.internal.manager.SubscribeManagerImpl
import net.mm2d.upnp.internal.message.SsdpRequest
import net.mm2d.upnp.internal.message.SsdpResponse
import net.mm2d.upnp.internal.parser.DeviceParser
import net.mm2d.upnp.internal.server.DEFAULT_SSDP_MESSAGE_FILTER
import net.mm2d.upnp.internal.server.MulticastEventReceiverList
import net.mm2d.upnp.internal.server.SsdpNotifyServer
import net.mm2d.upnp.internal.server.SsdpNotifyServerList
import net.mm2d.upnp.internal.server.SsdpSearchServer
import net.mm2d.upnp.internal.server.SsdpSearchServerList
import net.mm2d.upnp.internal.thread.TaskExecutors
import net.mm2d.upnp.util.NetworkUtils
import net.mm2d.upnp.util.TestUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException
import java.net.InetAddress
import java.net.URL

@Suppress("TestFunctionName", "NonAsciiCharacters", "ClassName")
@RunWith(Enclosed::class)
class ControlPointTest {
    @RunWith(JUnit4::class)
    class mock未使用 {
        @Test(expected = IllegalStateException::class)
        fun constructor_インターフェース空で指定() {
            ControlPointImpl(
                Protocol.DEFAULT, emptyList(),
                notifySegmentCheckEnabled = false,
                subscriptionEnabled = true,
                multicastEventingEnabled = false,
                factory = mockk(relaxed = true)
            )
        }

        @Test(timeout = 10000L)
        fun initialize_terminate() {
            val cp = ControlPointFactory.create()
            cp.initialize()
            cp.terminate()
        }

        @Test(timeout = 10000L)
        fun initialize_initialize_terminate() {
            val cp = ControlPointFactory.create()
            cp.initialize()
            cp.initialize()
            cp.terminate()
        }

        @Test(timeout = 10000L)
        fun initialize_terminate_intercept() {
            val thread = Thread {
                val cp = ControlPointFactory.create()
                cp.initialize()
                cp.terminate()
            }
            thread.start()
            Thread.sleep(200)
            thread.interrupt()
            thread.join()
        }

        @Test(timeout = 10000L)
        fun terminate() {
            val cp = ControlPointFactory.create()
            cp.terminate()
        }

        @Test(timeout = 10000L)
        fun start_stop() {
            val cp = ControlPointFactory.create()
            cp.initialize()
            cp.start()
            cp.stop()
            cp.terminate()
        }

        @Test(timeout = 10000L)
        fun start_stop2() {
            val cp = ControlPointFactory.create()
            cp.initialize()
            cp.start()
            cp.stop()
            cp.stop()
            cp.terminate()
        }

        @Test(timeout = 10000L)
        fun start_stop_with_multicast_eventing() {
            val multicastEventReceiverList: MulticastEventReceiverList = mockk(relaxed = true)
            val factory: DiFactory = mockk(relaxed = true)
            every { factory.createMulticastEventReceiverList(any(), any(), any()) } returns multicastEventReceiverList
            val cp = ControlPointImpl(
                Protocol.DEFAULT, Protocol.DEFAULT.getAvailableInterfaces(),
                notifySegmentCheckEnabled = false,
                subscriptionEnabled = true,
                multicastEventingEnabled = true,
                factory = factory
            )
            cp.start()
            verify(exactly = 1) { multicastEventReceiverList.start() }
            cp.stop()
            verify(exactly = 1) { multicastEventReceiverList.stop() }
        }

        @Test(timeout = 10000L)
        fun start_stop_illegal() {
            val cp = ControlPointFactory.create()
            cp.start()
            cp.start()
            cp.terminate()
            cp.terminate()
        }

        @Test(expected = IllegalStateException::class)
        fun search_not_started() {
            val cp = ControlPointFactory.create()
            cp.search()
        }

        @Test
        fun search() {
            val list: SsdpSearchServerList = mockk(relaxed = true)
            val factory: DiFactory = mockk(relaxed = true)
            every { factory.createSsdpSearchServerList(any(), any(), any()) } returns list
            val cp = ControlPointImpl(
                Protocol.DEFAULT,
                NetworkUtils.getAvailableInet4Interfaces(),
                notifySegmentCheckEnabled = false,
                subscriptionEnabled = true,
                multicastEventingEnabled = false,
                factory = factory
            )
            cp.initialize()
            cp.start()
            cp.search()
            verify(exactly = 1) { list.search(null) }
            cp.stop()
            cp.terminate()
        }

        @Test
        fun needToUpdateSsdpMessage_DUAL_STACK() {
            val cp = ControlPointFactory.create(
                protocol = Protocol.DUAL_STACK,
                interfaces = NetworkUtils.getAvailableInterfaces()
            ) as ControlPointImpl
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("fe80::1:1:1:1"),
                    makeAddressMock("fe80::1:1:1:1")
                )
            ).isTrue()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("fe80::1:1:1:1"),
                    makeAddressMock("169.254.1.1")
                )
            ).isFalse()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("fe80::1:1:1:1"),
                    makeAddressMock("192.168.1.1")
                )
            ).isTrue()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("169.254.1.1"),
                    makeAddressMock("169.254.1.1")
                )
            ).isTrue()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("169.254.1.1"),
                    makeAddressMock("192.168.1.1")
                )
            ).isTrue()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("169.254.1.1"),
                    makeAddressMock("fe80::1:1:1:1")
                )
            ).isTrue()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("192.168.1.1"),
                    makeAddressMock("169.254.1.1")
                )
            ).isTrue()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("192.168.1.1"),
                    makeAddressMock("192.168.1.1")
                )
            ).isTrue()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("192.168.1.1"),
                    makeAddressMock("fe80::1:1:1:1")
                )
            ).isFalse()
        }

        @Test
        fun needToUpdateSsdpMessage_IP_V4_ONLY() {
            val cp = ControlPointFactory.create(
                protocol = Protocol.IP_V4_ONLY,
                interfaces = NetworkUtils.getAvailableInterfaces()
            ) as ControlPointImpl
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("fe80::1:1:1:1"),
                    makeAddressMock("fe80::1:1:1:1")
                )
            ).isFalse()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("fe80::1:1:1:1"),
                    makeAddressMock("169.254.1.1")
                )
            ).isTrue()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("fe80::1:1:1:1"),
                    makeAddressMock("192.168.1.1")
                )
            ).isTrue()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("169.254.1.1"),
                    makeAddressMock("169.254.1.1")
                )
            ).isTrue()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("169.254.1.1"),
                    makeAddressMock("192.168.1.1")
                )
            ).isTrue()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("169.254.1.1"),
                    makeAddressMock("fe80::1:1:1:1")
                )
            ).isFalse()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("192.168.1.1"),
                    makeAddressMock("169.254.1.1")
                )
            ).isTrue()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("192.168.1.1"),
                    makeAddressMock("192.168.1.1")
                )
            ).isTrue()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("192.168.1.1"),
                    makeAddressMock("fe80::1:1:1:1")
                )
            ).isFalse()
        }

        @Test
        fun needToUpdateSsdpMessage_IP_V6_ONLY() {
            val cp = ControlPointFactory.create(
                protocol = Protocol.IP_V6_ONLY,
                interfaces = NetworkUtils.getAvailableInterfaces()
            ) as ControlPointImpl
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("fe80::1:1:1:1"),
                    makeAddressMock("fe80::1:1:1:1")
                )
            ).isTrue()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("fe80::1:1:1:1"),
                    makeAddressMock("169.254.1.1")
                )
            ).isFalse()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("fe80::1:1:1:1"),
                    makeAddressMock("192.168.1.1")
                )
            ).isFalse()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("169.254.1.1"),
                    makeAddressMock("169.254.1.1")
                )
            ).isFalse()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("169.254.1.1"),
                    makeAddressMock("192.168.1.1")
                )
            ).isFalse()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("169.254.1.1"),
                    makeAddressMock("fe80::1:1:1:1")
                )
            ).isTrue()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("192.168.1.1"),
                    makeAddressMock("169.254.1.1")
                )
            ).isFalse()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("192.168.1.1"),
                    makeAddressMock("192.168.1.1")
                )
            ).isFalse()
            assertThat(
                cp.needToUpdateSsdpMessage(
                    makeAddressMock("192.168.1.1"),
                    makeAddressMock("fe80::1:1:1:1")
                )
            ).isTrue()
        }

        private fun makeAddressMock(address: String): SsdpMessage {
            val message: SsdpMessage = mockk(relaxed = true)
            val inetAddress = InetAddress.getByName(address)
            every { message.localAddress } returns inetAddress
            return message
        }

        @Test
        fun setIconFilter_nullを指定しても問題ない() {
            val cp = ControlPointFactory.create()
            cp.setIconFilter(null)
        }
    }

    @RunWith(JUnit4::class)
    class ネットワーク未使用 {
        private lateinit var cp: ControlPointImpl
        private val ssdpSearchServerList: SsdpSearchServerList = mockk(relaxed = true)
        private val ssdpNotifyServerList: SsdpNotifyServerList = mockk(relaxed = true)
        private val taskExecutors: TaskExecutors = mockk(relaxed = true)
        private val diFactory = spyk(DiFactory())
        private val subscribeManager = spyk(SubscribeManagerImpl(taskExecutors, mockk(relaxed = true), diFactory))

        @Before
        fun setUp() {
            val factory = spyk(DiFactory())
            every { factory.createSsdpSearchServerList(any(), any(), any()) } returns ssdpSearchServerList
            every { factory.createSsdpNotifyServerList(any(), any(), any()) } returns ssdpNotifyServerList
            every { factory.createSubscribeManager(any(), any(), any()) } returns subscribeManager
            cp = spyk(
                ControlPointImpl(
                    Protocol.DEFAULT,
                    NetworkUtils.getAvailableInet4Interfaces(),
                    notifySegmentCheckEnabled = false,
                    subscriptionEnabled = true,
                    multicastEventingEnabled = false,
                    factory = factory
                )
            )
        }

        @Test
        fun discoverDevice_onDiscoverが通知される() {
            val l: DiscoveryListener = mockk(relaxed = true)
            cp.addDiscoveryListener(l)
            val uuid = "uuid"
            val device: Device = mockk(relaxed = true)
            every { device.udn } returns uuid
            cp.discoverDevice(device)
            Thread.sleep(100)

            assertThat(cp.getDevice(uuid)).isEqualTo(device)
            assertThat(cp.deviceList).contains(device)
            assertThat(cp.deviceListSize).isEqualTo(1)
            verify(exactly = 1) { l.onDiscover(device) }
        }

        @Test
        fun clearDeviceList_Deviceがクリアされる() {
            val l: DiscoveryListener = mockk(relaxed = true)
            cp.addDiscoveryListener(l)
            val uuid = "uuid"
            val device: Device = mockk(relaxed = true)
            every { device.udn } returns uuid
            cp.discoverDevice(device)
            Thread.sleep(100)

            assertThat(cp.getDevice(uuid)).isEqualTo(device)
            assertThat(cp.deviceList).contains(device)
            assertThat(cp.deviceListSize).isEqualTo(1)
            verify(exactly = 1) { l.onDiscover(device) }

            cp.clearDeviceList()
            Thread.sleep(100)

            assertThat(cp.getDevice(uuid)).isNull()
            assertThat(cp.deviceList).doesNotContain(device)
            assertThat(cp.deviceListSize).isEqualTo(0)
            verify(exactly = 1) { l.onDiscover(device) }
        }

        @Test
        fun lostDevice_onLostが通知される() {
            val l: DiscoveryListener = mockk(relaxed = true)
            cp.addDiscoveryListener(l)
            val uuid = "uuid"
            val device: Device = mockk(relaxed = true)
            val service: Service = mockk(relaxed = true)
            every { device.udn } returns uuid
            every { device.serviceList } returns listOf(service)
            cp.discoverDevice(device)
            Thread.sleep(100)
            cp.lostDevice(device)
            Thread.sleep(100)

            assertThat(cp.getDevice(uuid)).isNull()
            assertThat(cp.deviceListSize).isEqualTo(0)

            verify(exactly = 1) { l.onLost(device) }
            verify(exactly = 1) { subscribeManager.unregister(service) }
        }

        @Test
        fun stop_lostDeviceが通知される() {
            val uuid = "uuid"
            val device: Device = mockk(relaxed = true)
            val service: Service = mockk(relaxed = true)
            every { device.udn } returns uuid
            every { device.expireTime } returns System.currentTimeMillis() + 1000L
            every { device.serviceList } returns listOf(service)
            cp.start()
            cp.discoverDevice(device)
            Thread.sleep(100)
            cp.stop()
            Thread.sleep(100)

            assertThat(cp.getDevice(uuid)).isNull()
            assertThat(cp.deviceListSize).isEqualTo(0)

            verify(exactly = 1) { cp.lostDevice(device) }
            verify(exactly = 1) { subscribeManager.unregister(service) }
        }

        @Test
        fun removeDiscoveryListener_削除できる() {
            val l: DiscoveryListener = mockk(relaxed = true)
            cp.addDiscoveryListener(l)

            cp.removeDiscoveryListener(l)

            val uuid = "uuid"
            val device: Device = mockk(relaxed = true)
            every { device.udn } returns uuid
            cp.discoverDevice(device)
            Thread.sleep(100)

            verify(inverse = true) { l.onDiscover(device) }
        }

        @Test
        fun addDiscoveryListener_多重登録防止() {
            val l: DiscoveryListener = mockk(relaxed = true)
            cp.addDiscoveryListener(l)
            cp.addDiscoveryListener(l)

            cp.removeDiscoveryListener(l)

            val uuid = "uuid"
            val device: Device = mockk(relaxed = true)
            every { device.udn } returns uuid
            cp.discoverDevice(device)
            Thread.sleep(100)

            verify(inverse = true) { l.onDiscover(device) }
        }

        @Test
        fun addDiscoveryListener_by_adapter() {
            val discover: (Device) -> Unit = mockk(relaxed = true)
            val lost: (Device) -> Unit = mockk(relaxed = true)
            cp.addDiscoveryListener(discoveryListener(discover, lost))
            val uuid = "uuid"
            val device: Device = mockk(relaxed = true)
            every { device.udn } returns uuid
            cp.discoverDevice(device)
            Thread.sleep(100)

            verify(exactly = 1) { discover(device) }

            cp.lostDevice(device)
            Thread.sleep(100)

            verify(exactly = 1) { lost(device) }
        }

        @Test
        fun registerSubscribeService_による登録() {
            val sid = "sid"
            val service: Service = mockk(relaxed = true)
            every { service.subscriptionId } returns sid

            subscribeManager.register(service, 1000L, true)
            assertThat(subscribeManager.getSubscribeService(sid)).isEqualTo(service)
        }

        @Test
        fun unregisterSubscribeService_による削除() {
            val sid = "sid"
            val service: Service = mockk(relaxed = true)
            every { service.subscriptionId } returns sid

            subscribeManager.register(service, 1000L, true)
            subscribeManager.unregister(service)
            assertThat(subscribeManager.getSubscribeService(sid)).isNull()
        }
    }

    @RunWith(JUnit4::class)
    class DeviceDiscovery {
        private lateinit var cp: ControlPointImpl
        private lateinit var loadingDeviceMap: MutableMap<String, DeviceImpl.Builder>
        private lateinit var deviceHolder: DeviceHolder
        private lateinit var taskExecutors: TaskExecutors

        @Before
        fun setUp() {
            loadingDeviceMap = spyk(HashMap())
            val factory = spyk(DiFactory())
            taskExecutors = spyk(TaskExecutors())
            every { factory.createTaskExecutors() } returns taskExecutors
            every { factory.createLoadingDeviceMap() } returns loadingDeviceMap
            every { factory.createDeviceHolder(any(), any()) } answers {
                spyk(
                    DeviceHolder(
                        arg(0),
                        arg(1)
                    )
                ).also { deviceHolder = it }
            }
            cp = spyk(
                ControlPointImpl(
                    Protocol.DEFAULT,
                    NetworkUtils.getAvailableInet4Interfaces(),
                    notifySegmentCheckEnabled = false,
                    subscriptionEnabled = true,
                    multicastEventingEnabled = false,
                    factory = factory
                )
            )
        }

        @Test
        fun onReceiveSsdp_読み込み済みデバイスにないbyebye受信() {
            val data = TestUtils.getResourceAsByteArray("ssdp-notify-byebye0.bin")
            val addr = InetAddress.getByName("192.0.2.3")
            val message = SsdpRequest.create(addr, data, data.size)
            cp.onReceiveSsdpMessage(message)
            verify(exactly = 1) { loadingDeviceMap.remove(any()) }
        }

        @Test
        fun onReceiveSsdp_読み込み済みデバイスのbyebye受信() {
            val data = TestUtils.getResourceAsByteArray("ssdp-notify-byebye0.bin")
            val addr = InetAddress.getByName("192.0.2.3")
            val message = SsdpRequest.create(addr, data, data.size)
            val device: Device = mockk(relaxed = true)
            val udn = "uuid:01234567-89ab-cdef-0123-456789abcdef"
            every { device.udn } returns udn
            cp.discoverDevice(device)
            assertThat(deviceHolder[udn]).isEqualTo(device)
            cp.onReceiveSsdpMessage(message)
            assertThat(deviceHolder[udn]).isNull()
        }

        @Test
        fun onReceiveSsdp_alive受信後失敗() {
            val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")
            val addr = InetAddress.getByName("192.0.2.3")
            val message = SsdpRequest.create(addr, data, data.size)
            val udn = "uuid:01234567-89ab-cdef-0123-456789abcdef"
            val client: SingleHttpClient = mockk(relaxed = true)
            every { client.downloadString(any()) } answers {
                Thread.sleep(500L)
                throw IOException()
            }
            mockkObject(SingleHttpClient.Companion)
            every { SingleHttpClient.create(any()) } returns client
            cp.onReceiveSsdpMessage(message)
            assertThat(loadingDeviceMap).containsKey(udn)
            Thread.sleep(1000L) // Exception発生を待つ
            assertThat(loadingDeviceMap).doesNotContainKey(udn)
            assertThat(deviceHolder.size).isEqualTo(0)
            unmockkObject(SingleHttpClient.Companion)
        }

        @Test
        fun onReceiveSsdp_alive受信後成功() {
            val httpClient: SingleHttpClient = mockk(relaxed = true)
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device.xml")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/cds.xml"))
            } returns TestUtils.getResourceAsString("cds.xml")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/cms.xml"))
            } returns TestUtils.getResourceAsString("cms.xml")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/mmupnp.xml"))
            } returns TestUtils.getResourceAsString("mmupnp.xml")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/icon/icon120.jpg"))
            } returns TestUtils.getResourceAsString("icon/icon120.jpg")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/icon/icon48.jpg"))
            } returns TestUtils.getResourceAsString("icon/icon48.jpg")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/icon/icon120.png"))
            } returns TestUtils.getResourceAsString("icon/icon120.png")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/icon/icon48.png"))
            } returns TestUtils.getResourceAsString("icon/icon48.png")

            val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")
            val address = InetAddress.getByName("192.0.2.3")
            val message = SsdpRequest.create(address, data, data.size)
            val udn = "uuid:01234567-89ab-cdef-0123-456789abcdef"
            mockkObject(SingleHttpClient.Companion)
            every { SingleHttpClient.create(any()) } returns httpClient
            val iconFilter = spyk(iconFilter { listOf(it[0]) })
            cp.setIconFilter(iconFilter)
            cp.onReceiveSsdpMessage(message)
            Thread.sleep(1000) // 読み込みを待つ
            val device = cp.getDevice(udn)
            verify(exactly = 1) { iconFilter.invoke(any()) }
            assertThat(device!!.iconList).hasSize(4)
            assertThat(device.iconList[0].binary).isNotNull()
            assertThat(device.iconList[1].binary).isNull()
            assertThat(device.iconList[2].binary).isNull()
            assertThat(device.iconList[3].binary).isNull()
            unmockkObject(SingleHttpClient.Companion)
        }

        @Test
        fun onReceiveSsdp_読み込み済みデバイスのalive受信() {
            val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")
            val addr = InetAddress.getByName("192.0.2.3")
            val message = SsdpRequest.create(addr, data, data.size)
            val device: Device = mockk(relaxed = true)
            every { device.ssdpMessage } returns message
            val udn = "uuid:01234567-89ab-cdef-0123-456789abcdef"
            every { device.udn } returns udn

            cp.discoverDevice(device)
            cp.onReceiveSsdpMessage(message)
        }

        @Test
        fun onReceiveSsdp_ロード中デバイスのalive受信() {
            val data1 = TestUtils.getResourceAsByteArray("ssdp-notify-alive1.bin")
            val addr = InetAddress.getByName("192.0.2.3")
            val message1 = SsdpRequest.create(addr, data1, data1.size)
            val deviceBuilder = spyk(DeviceImpl.Builder(cp, message1))
            loadingDeviceMap[deviceBuilder.getUuid()] = deviceBuilder
            val data2 = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")
            val message2 = SsdpRequest.create(addr, data2, data2.size)
            cp.onReceiveSsdpMessage(message2)
            verify(exactly = 1) { deviceBuilder.updateSsdpMessage(message2) }
        }

        @Test
        fun tryAddDevice() {
            val uuid = "uuid"
            every { taskExecutors.io(any<() -> Unit>()) } returns true
            cp.tryAddDevice(uuid, "http://10.0.0.1/")
            verify(exactly = 1) { cp.loadDevice(uuid, any()) }
        }

        @Test
        fun tryAddDevice_already_added() {
            val uuid = "uuid"
            every { taskExecutors.io(any<() -> Unit>()) } returns true
            cp.tryAddDevice(uuid, "http://10.0.0.1/")
            cp.tryAddDevice(uuid, "http://10.0.0.1/")
            verify(exactly = 1) { cp.loadDevice(uuid, any()) }
        }

        @Test
        fun tryAddDevice_already_added_fail_first() {
            val uuid = "uuid"
            every { taskExecutors.io(any<() -> Unit>()) } returns false
            cp.tryAddDevice(uuid, "http://10.0.0.1/")
            cp.tryAddDevice(uuid, "http://10.0.0.1/")
            verify(exactly = 2) { cp.loadDevice(uuid, any()) }
        }

        @Test
        fun tryAddDevice_already_loaded() {
            val location = "http://10.0.0.1/"
            val device: Device = mockk()
            every { device.location } returns location
            every { cp.deviceList } returns listOf(device)
            val uuid = "uuid"
            every { taskExecutors.io(any<() -> Unit>()) } returns true
            cp.tryAddDevice(uuid, location)
            verify(inverse = true) { cp.loadDevice(uuid, any()) }
        }
    }

    @RunWith(JUnit4::class)
    class PinnedDevice {
        private lateinit var cp: ControlPointImpl
        private lateinit var httpClient: SingleHttpClient

        @Before
        fun setUp() {
            cp = spyk(
                ControlPointImpl(
                    Protocol.DEFAULT,
                    NetworkUtils.getAvailableInet4Interfaces(),
                    notifySegmentCheckEnabled = false,
                    subscriptionEnabled = true,
                    multicastEventingEnabled = false,
                    factory = DiFactory(Protocol.DEFAULT)
                )
            )
            httpClient = mockk(relaxed = true)
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device.xml")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/cds.xml"))
            } returns TestUtils.getResourceAsString("cds.xml")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/cms.xml"))
            } returns TestUtils.getResourceAsString("cms.xml")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/mmupnp.xml"))
            } returns TestUtils.getResourceAsString("mmupnp.xml")
            mockkObject(SingleHttpClient.Companion)
            every { SingleHttpClient.create(any()) } returns httpClient
            every { httpClient.localAddress } returns InetAddress.getByName("192.0.2.3")
        }

        @After
        fun teardown() {
            unmockkObject(SingleHttpClient.Companion)
        }

        @Test
        fun tryAddPinnedDevice() {
            cp.tryAddPinnedDevice("http://192.0.2.2:12345/device.xml")
            Thread.sleep(1000) // 読み込みを待つ
            assertThat(cp.deviceListSize).isEqualTo(1)
            val device = cp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcdef")
            assertThat(device!!.isPinned).isTrue()
        }

        @Test
        fun tryAddPinnedDevice2回目() {
            cp.tryAddPinnedDevice("http://192.0.2.2:12345/device.xml")
            Thread.sleep(1000) // 読み込みを待つ
            assertThat(cp.deviceListSize).isEqualTo(1)
            val device = cp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcdef")
            assertThat(device!!.isPinned).isTrue()

            cp.tryAddPinnedDevice("http://192.0.2.2:12345/device.xml")
        }

        @Test
        fun tryAddPinnedDevice_すでに発見済み() {
            val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")
            val address = InetAddress.getByName("192.0.2.3")
            val message = SsdpRequest.create(address, data, data.size)
            cp.onReceiveSsdpMessage(message)
            Thread.sleep(1000) // 読み込みを待つ
            cp.tryAddPinnedDevice("http://192.0.2.2:12345/device.xml")
            Thread.sleep(1000) // 読み込みを待つ
            assertThat(cp.deviceListSize).isEqualTo(1)
            val device = cp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcdef")
            assertThat(device!!.isPinned).isTrue()
        }

        @Test
        fun tryAddPinnedDeviceの後に発見() {
            val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")
            val address = InetAddress.getByName("192.0.2.3")
            val message = SsdpRequest.create(address, data, data.size)
            cp.onReceiveSsdpMessage(message)
            Thread.sleep(1000) // 読み込みを待つ
            val device = cp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcdef")
            assertThat(device!!.isPinned).isFalse()

            cp.tryAddPinnedDevice("http://192.0.2.2:12345/device.xml")
            Thread.sleep(1000) // 読み込みを待つ
            assertThat(cp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcdef")!!.isPinned).isTrue()

            cp.discoverDevice(device)
            assertThat(cp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcdef")!!.isPinned).isTrue()
        }

        @Test
        fun tryAddPinnedDevice_Exceptionが発生してもクラッシュしない() {
            every { httpClient.downloadString(any()) } throws IOException()
            cp.tryAddPinnedDevice("http://192.0.2.2:12345/device.xml")
            Thread.sleep(1000) // 読み込みを待つ
        }

        @Test
        fun removePinnedDevice() {
            cp.tryAddPinnedDevice("http://192.0.2.2:12345/device.xml")
            Thread.sleep(1000) // 読み込みを待つ
            assertThat(cp.deviceListSize).isEqualTo(1)

            cp.removePinnedDevice("http://192.0.2.2:12345/device.xml")
            assertThat(cp.deviceListSize).isEqualTo(0)
        }

        @Test
        fun removePinnedDevice_before_load() {
            cp.tryAddPinnedDevice("http://192.0.2.2:12345/device.xml")
            assertThat(cp.deviceListSize).isEqualTo(0)
            cp.removePinnedDevice("http://192.0.2.2:12345/device.xml")
            Thread.sleep(1000) // 読み込みを待つ
            assertThat(cp.deviceListSize).isEqualTo(0)
        }
    }

    @RunWith(JUnit4::class)
    class イベント伝搬テスト {
        private lateinit var cp: ControlPointImpl
        private lateinit var subscribeManager: SubscribeManagerImpl
        private val loadingDeviceMap = spyk(HashMap<String, DeviceImpl.Builder>())
        private lateinit var deviceHolder: DeviceHolder
        private val ssdpSearchServerList: SsdpSearchServerList = mockk(relaxed = true)
        private val ssdpNotifyServerList: SsdpNotifyServerList = mockk(relaxed = true)
        private lateinit var responseListener: (SsdpMessage) -> Unit
        private lateinit var notifyListener: (SsdpMessage) -> Unit

        @Before
        fun setUp() {
            val factory = spyk(DiFactory())
            every { factory.createLoadingDeviceMap() } returns loadingDeviceMap
            every { factory.createDeviceHolder(any(), any()) } answers {
                spyk(
                    DeviceHolder(
                        arg(0),
                        arg(1)
                    )
                ).also { deviceHolder = it }
            }
            every { factory.createSsdpSearchServerList(any(), any(), any()) } answers {
                responseListener = arg(2)
                ssdpSearchServerList
            }
            every { factory.createSsdpNotifyServerList(any(), any(), any()) } answers {
                notifyListener = arg(2)
                ssdpNotifyServerList
            }
            every { factory.createSubscribeManager(any(), any(), any()) } answers {
                spyk(SubscribeManagerImpl(arg(1), arg(2), factory)).also { subscribeManager = it }
            }
            cp = ControlPointImpl(
                Protocol.DEFAULT,
                NetworkUtils.getAvailableInet4Interfaces(),
                notifySegmentCheckEnabled = false,
                subscriptionEnabled = true,
                multicastEventingEnabled = false,
                factory = factory
            )
        }

        @Test
        fun stop時にunsubscribeとlostが発生すること() {
            cp.start()
            val device: Device = mockk(relaxed = true)
            every { device.udn } returns "udn"
            cp.discoverDevice(device)
            val service: Service = mockk(relaxed = true)
            every { service.subscriptionId } returns "SubscriptionId"
            subscribeManager.register(service, 1000L, false)
            cp.stop()
            cp.terminate()
            Thread.sleep(100)
            coVerify(exactly = 1) { service.unsubscribe() }
            verify(exactly = 1) { deviceHolder.remove(device) }
        }

        @Test
        fun onReceiveSsdp_ResponseListenerから伝搬() {
            val data = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin")
            val message = spyk(SsdpResponse.create(mockk(relaxed = true), data, data.size))

            responseListener.invoke(message)

            Thread.sleep(100)
            verify { message.uuid }
        }

        @Test
        fun onReceiveSsdp_NotifyListenerから伝搬() {
            val data = TestUtils.getResourceAsByteArray("ssdp-notify-byebye0.bin")
            val message = spyk(SsdpRequest.create(mockk(relaxed = true), data, data.size))

            notifyListener.invoke(message)

            Thread.sleep(100)
            verify { message.uuid }
        }
    }

    @RunWith(JUnit4::class)
    class EventReceiverに起因するテスト {
        private lateinit var cp: ControlPointImpl
        private lateinit var subscribeManager: SubscribeManagerImpl

        @Before
        fun setUp() {
            val factory = spyk(DiFactory())
            every { factory.createSsdpSearchServerList(any(), any(), any()) } returns mockk(relaxed = true)
            every { factory.createSsdpNotifyServerList(any(), any(), any()) } returns mockk(relaxed = true)
            every { factory.createSubscribeManager(any(), any(), any()) } answers {
                spyk(SubscribeManagerImpl(arg(1), arg(2), factory)).also { subscribeManager = it }
            }
            cp = spyk(
                ControlPointImpl(
                    Protocol.DEFAULT,
                    NetworkUtils.getAvailableInet4Interfaces(),
                    notifySegmentCheckEnabled = false,
                    subscriptionEnabled = true,
                    multicastEventingEnabled = false,
                    factory = factory
                )
            )
        }

        @Test
        fun notifyEvent_イベントがリスナーに通知されること() {
            val sid = "sid"
            val service: Service = mockk(relaxed = true)
            every { service.subscriptionId } returns sid

            val variableName = "variable"
            val variable: StateVariable = mockk(relaxed = true)
            every { variable.isSendEvents } returns true
            every { variable.name } returns variableName
            every { service.findStateVariable(variableName) } returns variable

            subscribeManager.register(service, 1000L, false)

            val eventListener: EventListener = mockk(relaxed = true)
            cp.addEventListener(eventListener)
            val notifyEventListener: NotifyEventListener = mockk(relaxed = true)
            cp.addNotifyEventListener(notifyEventListener)

            val value = "value"
            subscribeManager.onEventReceived(sid, 0, listOf(variableName to value))

            Thread.sleep(2000)

            verify(exactly = 1) { notifyEventListener.onNotifyEvent(service, 0, variableName, value) }
            verify(exactly = 1) { eventListener.onEvent(service, 0, listOf(variableName to value)) }
        }

        @Test
        fun notifyEvent_削除したリスナーに通知されないこと() {
            val sid = "sid"
            val service: Service = mockk(relaxed = true)
            every { service.subscriptionId } returns sid

            val variableName = "variable"
            val variable: StateVariable = mockk(relaxed = true)
            every { variable.isSendEvents } returns true
            every { variable.name } returns variableName
            every { service.findStateVariable(variableName) } returns variable

            subscribeManager.register(service, 1000L, false)

            val eventListener: EventListener = mockk(relaxed = true)
            cp.addEventListener(eventListener)
            cp.removeEventListener(eventListener)
            val notifyEventListener: NotifyEventListener = mockk(relaxed = true)
            cp.addNotifyEventListener(notifyEventListener)
            cp.removeNotifyEventListener(notifyEventListener)

            val value = "value"
            subscribeManager.onEventReceived(sid, 0, listOf(variableName to value))
            Thread.sleep(100)

            verify(inverse = true) { notifyEventListener.onNotifyEvent(service, 0, variableName, value) }
            verify(inverse = true) { eventListener.onEvent(service, 0, listOf(variableName to value)) }
        }

        @Test
        fun notifyEvent_対応する変数のないイベントが無視されること() {
            val sid = "sid"
            val service: Service = mockk(relaxed = true)
            every { service.subscriptionId } returns sid

            val variableName = "variable"
            val variable: StateVariable = mockk(relaxed = true)
            every { variable.isSendEvents } returns true
            every { variable.name } returns variableName
            every { service.findStateVariable(variableName) } returns variable

            subscribeManager.register(service, 1000L, false)

            val eventListener: EventListener = mockk(relaxed = true)
            cp.addEventListener(eventListener)
            val notifyEventListener: NotifyEventListener = mockk(relaxed = true)
            cp.addNotifyEventListener(notifyEventListener)

            val value = "value"
            subscribeManager.onEventReceived(sid, 0, listOf(variableName + 1 to value))
            Thread.sleep(100)

            verify(inverse = true) { notifyEventListener.onNotifyEvent(service, 0, variableName, value) }
            verify(exactly = 1) { eventListener.onEvent(service, 0, listOf(variableName + 1 to value)) }
        }

        @Test
        fun notifyEvent_対応するサービスがない() {
            val sid = "sid"
            val variableName = "variable"
            val value = "value"
            assertThat(subscribeManager.onEventReceived(sid, 0, listOf(variableName + 1 to value))).isFalse()
        }

        @Test
        fun notifyEvent_by_adapter() {
            val sid = "sid"
            val service: Service = mockk(relaxed = true)
            every { service.subscriptionId } returns sid

            val variableName = "variable"
            val variable: StateVariable = mockk(relaxed = true)
            every { variable.isSendEvents } returns true
            every { variable.name } returns variableName
            every { service.findStateVariable(variableName) } returns variable

            subscribeManager.register(service, 1000L, false)

            val onEvent: (service: Service, seq: Long, properties: List<Pair<String, String>>) -> Unit =
                mockk(relaxed = true)
            cp.addEventListener(eventListener(onEvent))
            val notifyEvent: (service: Service, seq: Long, variable: String, value: String) -> Unit =
                mockk(relaxed = true)
            cp.addNotifyEventListener(notifyEventListener(notifyEvent))

            val value = "value"
            subscribeManager.onEventReceived(sid, 0, listOf(variableName to value))

            Thread.sleep(2000)

            verify(exactly = 1) { notifyEvent(service, 0, variableName, value) }
            verify(exactly = 1) { onEvent(service, 0, listOf(variableName to value)) }
        }

    }

    @RunWith(JUnit4::class)
    class SsdpMessageFilterのテスト {
        private lateinit var cp: ControlPointImpl
        private lateinit var ssdpMessage: SsdpMessage
        private val receiverList: MutableList<SsdpNotifyServer> = mutableListOf()
        private val serverList: MutableList<SsdpSearchServer> = mutableListOf()

        @Before
        fun setUp() {
            mockkObject(SsdpNotifyServerList.Companion)
            mockkObject(SsdpSearchServerList.Companion)
            every { SsdpNotifyServerList.newServer(any(), any(), any(), any()) } answers {
                mockk<SsdpNotifyServer>(relaxed = true).also { receiverList += it }
            }
            every { SsdpSearchServerList.newServer(any(), any(), any(), any()) } answers {
                mockk<SsdpSearchServer>(relaxed = true).also { serverList += it }
            }
            cp = spyk(
                ControlPointImpl(
                    Protocol.DEFAULT,
                    NetworkUtils.getAvailableInet4Interfaces(),
                    notifySegmentCheckEnabled = false,
                    subscriptionEnabled = true,
                    multicastEventingEnabled = false,
                    factory = DiFactory(Protocol.DEFAULT)
                )
            )
            val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")
            val addr = InetAddress.getByName("192.0.2.3")
            ssdpMessage = SsdpRequest.create(addr, data, data.size)
        }

        @After
        fun teardown() {
            unmockkObject(SsdpNotifyServerList.Companion)
            unmockkObject(SsdpSearchServerList.Companion)
        }

        @Test
        fun filterが全serverに設定される() {
            val filter: (SsdpMessage) -> Boolean = mockk()
            cp.setSsdpMessageFilter(filter)
            receiverList.forEach {
                verify { it.setFilter(filter) }
            }
            serverList.forEach {
                verify { it.setFilter(filter) }
            }
        }

        @Test
        fun filterにnullを指定すると全serverに設定される() {
            cp.setSsdpMessageFilter(null)
            receiverList.forEach {
                verify { it.setFilter(DEFAULT_SSDP_MESSAGE_FILTER) }
            }
            serverList.forEach {
                verify { it.setFilter(DEFAULT_SSDP_MESSAGE_FILTER) }
            }
        }
    }

    @RunWith(JUnit4::class)
    class MulticastEventingのテスト {
        private lateinit var cp: ControlPointImpl

        @Before
        fun setUp() {
            cp = spyk(
                ControlPointImpl(
                    Protocol.DEFAULT,
                    NetworkUtils.getAvailableInet4Interfaces(),
                    notifySegmentCheckEnabled = false,
                    subscriptionEnabled = true,
                    multicastEventingEnabled = false,
                    factory = DiFactory(Protocol.DEFAULT, taskExecutor { it.run().let { true } })
                )
            )
        }

        @Test
        fun `onReceiveMulticastEvent listenerに通知される`() {
            val uuid = "uuid"
            val svcid = "svcid"
            val device: Device = mockk(relaxed = true)
            every { device.udn } returns uuid
            val service: Service = mockk(relaxed = true)
            every { device.findServiceById(svcid) } returns service
            cp.discoverDevice(device)

            val listener = spyk(multicastEventListener { _, _, _, _ -> })
            cp.addMulticastEventListener(listener)

            val lvl = "upnp:/info"
            val seq = 0L
            val properties = listOf<Pair<String, String>>()

            cp.onReceiveMulticastEvent(uuid, svcid, lvl, seq, properties)
            verify(exactly = 1) { listener.onEvent(service, lvl, seq, properties) }
        }

        @Test
        fun `onReceiveMulticastEvent removeしたlistenerには通知されない`() {
            val uuid = "uuid"
            val svcid = "svcid"
            val device: Device = mockk(relaxed = true)
            every { device.udn } returns uuid
            val service: Service = mockk(relaxed = true)
            every { device.findServiceById(svcid) } returns service
            cp.discoverDevice(device)

            val listener = spyk(multicastEventListener { _, _, _, _ -> })
            cp.addMulticastEventListener(listener)
            cp.removeMulticastEventListener(listener)

            val lvl = "upnp:/info"
            val seq = 0L
            val properties = listOf<Pair<String, String>>()

            cp.onReceiveMulticastEvent(uuid, svcid, lvl, seq, properties)
            verify(inverse = true) { listener.onEvent(any(), any(), any(), any()) }
        }

        @Test
        fun `onReceiveMulticastEvent uuidやsvcidが違っていると何も起こらない`() {
            val uuid = "uuid"
            val svcid = "svcid"
            val device: Device = mockk(relaxed = true)
            every { device.udn } returns uuid
            val service: Service = mockk(relaxed = true)
            every { device.findServiceById(any()) } returns null
            every { device.findServiceById(svcid) } returns service
            cp.discoverDevice(device)

            val listener = spyk(multicastEventListener { _, _, _, _ -> })
            cp.addMulticastEventListener(listener)

            val lvl = "upnp:/info"
            val seq = 0L
            val properties = listOf<Pair<String, String>>()

            cp.onReceiveMulticastEvent("hoge", svcid, lvl, seq, properties)
            verify(inverse = true) { listener.onEvent(any(), any(), any(), any()) }
            cp.onReceiveMulticastEvent(uuid, "hoge", lvl, seq, properties)
            verify(inverse = true) { listener.onEvent(any(), any(), any(), any()) }
        }
    }

    @RunWith(JUnit4::class)
    class EmbeddedDevice {
        @Test
        fun embeddedDeviceごとにuuidが異なる場合どのuuidでも取り出せる() {
            val cp: ControlPointImpl = spyk(
                ControlPointImpl(
                    Protocol.DEFAULT,
                    NetworkUtils.getAvailableInet4Interfaces(),
                    notifySegmentCheckEnabled = false,
                    subscriptionEnabled = true,
                    multicastEventingEnabled = false,
                    factory = DiFactory(Protocol.DEFAULT)
                )
            )
            val httpClient: SingleHttpClient = mockk(relaxed = true)
            val data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin")
            val ssdpMessage: SsdpMessage = SsdpRequest.create(mockk(relaxed = true), data, data.size)
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/device.xml"))
            } returns TestUtils.getResourceAsString("device-with-embedded-device.xml")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/cds.xml"))
            } returns TestUtils.getResourceAsString("cds.xml")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/cms.xml"))
            } returns TestUtils.getResourceAsString("cms.xml")
            every {
                httpClient.downloadString(URL("http://192.0.2.2:12345/mmupnp.xml"))
            } returns TestUtils.getResourceAsString("mmupnp.xml")

            mockkObject(SingleHttpClient.Companion)
            every { SingleHttpClient.create(any()) } returns httpClient
            every { httpClient.localAddress } returns InetAddress.getByName("192.0.2.3")
            val builder = DeviceImpl.Builder(cp, ssdpMessage)
            DeviceParser.loadDescription(httpClient, builder)
            val device = builder.build()
            cp.discoverDevice(device)
            assertThat(cp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcdee")).isEqualTo(device)
            assertThat(cp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcded")).isEqualTo(device)
            assertThat(cp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcdef")).isEqualTo(device)

            cp.lostDevice(device)
            assertThat(cp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcdee")).isNull()
            assertThat(cp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcded")).isNull()
            assertThat(cp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcdef")).isNull()

            unmockkObject(SingleHttpClient.Companion)
        }
    }
}
