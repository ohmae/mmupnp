/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.upnp.ControlPoint.DiscoveryListener;
import net.mm2d.upnp.ControlPoint.NotifyEventListener;
import net.mm2d.upnp.EventReceiver.EventMessageListener;
import net.mm2d.upnp.SsdpNotifyReceiver.NotifyListener;
import net.mm2d.upnp.SsdpSearchServer.ResponseListener;
import net.mm2d.util.NetworkUtils;
import net.mm2d.util.StringPair;
import net.mm2d.util.TestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class ControlPointTest {
    @RunWith(JUnit4.class)
    public static class mock未使用 {
        @Test
        public void constructor_引数無しでコール() throws Exception {
            new ControlPoint();
        }

        @Test
        public void constructor_インターフェース指定() throws Exception {
            new ControlPoint(NetworkUtils.getAvailableInet4Interfaces());
        }

        @Test(expected = IllegalStateException.class)
        public void constructor_インターフェース空で指定() throws Exception {
            new ControlPoint(Protocol.DEFAULT, Collections.<NetworkInterface>emptyList(), mock(ControlPointDiFactory.class));
        }

        @Test
        public void constructor_インターフェース選別() throws Exception {
            new ControlPoint(Protocol.IP_V4_ONLY, NetworkUtils.getNetworkInterfaceList());
            new ControlPoint(Protocol.IP_V6_ONLY, NetworkUtils.getNetworkInterfaceList());
            new ControlPoint(Protocol.DUAL_STACK, NetworkUtils.getNetworkInterfaceList());
        }

        @Test(timeout = 2000L)
        public void initialize_terminate() throws Exception {
            final ControlPoint cp = new ControlPoint();
            cp.initialize();
            cp.terminate();
        }

        @Test(timeout = 2000L)
        public void initialize_initialize_terminate() throws Exception {
            final ControlPoint cp = new ControlPoint();
            cp.initialize();
            cp.initialize();
            cp.terminate();
        }

        @Test(timeout = 2000L)
        public void initialize_terminate_intercept() throws Exception {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    final ControlPoint cp = new ControlPoint();
                    cp.initialize();
                    cp.terminate();
                }
            });
            thread.start();
            Thread.sleep(200);
            thread.interrupt();
            thread.join();
        }

        @Test(timeout = 2000L)
        public void initialize_terminate_timeover() throws Exception {
            final ExecutorService executor = mock(ExecutorService.class);
            final ControlPoint cp = new ControlPoint(Protocol.DEFAULT, NetworkUtils.getAvailableInet4Interfaces(),
                    new ControlPointDiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        ExecutorService createIoExecutor() {
                            return executor;
                        }
                    });
            doReturn(false).when(executor).awaitTermination(anyLong(), ArgumentMatchers.any(TimeUnit.class));
            cp.initialize();
            cp.terminate();
            verify(executor, times(1)).shutdownNow();
        }

        @Test(timeout = 2000L)
        public void initialize_terminate_exception() throws Exception {
            final ExecutorService executor = mock(ExecutorService.class);
            final ControlPoint cp = new ControlPoint(Protocol.DEFAULT, NetworkUtils.getAvailableInet4Interfaces(),
                    new ControlPointDiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        ExecutorService createIoExecutor() {
                            return executor;
                        }
                    });
            doThrow(new InterruptedException()).when(executor).awaitTermination(anyLong(), ArgumentMatchers.any(TimeUnit.class));
            cp.initialize();
            cp.terminate();
        }

        @Test(timeout = 1000L)
        public void terminate() throws Exception {
            final ControlPoint cp = new ControlPoint((Collection<NetworkInterface>) null);
            cp.terminate();
        }

        @Test(timeout = 10000L)
        public void start_stop() throws Exception {
            final ControlPoint cp = new ControlPoint();
            cp.initialize();
            cp.start();
            cp.stop();
            cp.terminate();
        }

        @Test(timeout = 10000L)
        public void start_stop2() throws Exception {
            final ControlPoint cp = new ControlPoint();
            cp.initialize();
            cp.start();
            cp.stop();
            cp.stop();
            cp.terminate();
        }

        @Test(timeout = 1000L)
        public void start_stop_illegal() throws Exception {
            final ControlPoint cp = new ControlPoint();
            cp.start();
            cp.start();
            cp.terminate();
            cp.terminate();
        }

        @Test(timeout = 10000L)
        public void start_stop_exception() throws Exception {
            final EventReceiver eventReceiver = mock(EventReceiver.class);
            final ControlPoint cp = new ControlPoint(Protocol.DEFAULT, NetworkUtils.getAvailableInet4Interfaces(),
                    new ControlPointDiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        EventReceiver createEventReceiver(@Nonnull final EventMessageListener listener) {
                            return eventReceiver;
                        }
                    });
            doThrow(new IOException()).when(eventReceiver).open();
            cp.initialize();
            cp.start();
            cp.stop();
            cp.terminate();
        }

        @Test(expected = IllegalStateException.class)
        public void search_not_started() throws Exception {
            final ControlPoint cp = new ControlPoint();
            cp.search();
        }

        @Test
        public void search() throws Exception {
            final SsdpSearchServerList list = mock(SsdpSearchServerList.class);
            final ControlPoint cp = new ControlPoint(Protocol.DEFAULT, NetworkUtils.getAvailableInet4Interfaces(),
                    new ControlPointDiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        SsdpSearchServerList createSsdpSearchServerList(
                                @Nonnull Collection<NetworkInterface> interfaces,
                                @Nonnull ResponseListener listener) {
                            return list;
                        }
                    });
            cp.initialize();
            cp.start();
            cp.search();
            verify(list).search(null);
            cp.stop();
            cp.terminate();
        }

        @Test
        public void createHttpClient() throws Exception {
            final ControlPoint cp = new ControlPoint();
            final HttpClient client = cp.createHttpClient();
            assertThat(client.isKeepAlive(), is(true));
        }

        @Test
        public void needToUpdateSsdpMessage_DUAL_STACK() throws Exception {
            final ControlPoint cp = new ControlPoint(Protocol.DUAL_STACK, NetworkUtils.getNetworkInterfaceList());
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("fe80::1:1:1:1"), makeAddressMock("fe80::1:1:1:1")), is(true));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("fe80::1:1:1:1"), makeAddressMock("169.254.1.1")), is(false));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("fe80::1:1:1:1"), makeAddressMock("192.168.1.1")), is(true));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("169.254.1.1"), makeAddressMock("169.254.1.1")), is(true));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("169.254.1.1"), makeAddressMock("192.168.1.1")), is(true));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("169.254.1.1"), makeAddressMock("fe80::1:1:1:1")), is(true));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("192.168.1.1"), makeAddressMock("169.254.1.1")), is(true));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("192.168.1.1"), makeAddressMock("192.168.1.1")), is(true));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("192.168.1.1"), makeAddressMock("fe80::1:1:1:1")), is(false));
        }

        @Test
        public void needToUpdateSsdpMessage_IP_V4_ONLY() throws Exception {
            final ControlPoint cp = new ControlPoint(Protocol.IP_V4_ONLY, NetworkUtils.getNetworkInterfaceList());
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("fe80::1:1:1:1"), makeAddressMock("fe80::1:1:1:1")), is(false));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("fe80::1:1:1:1"), makeAddressMock("169.254.1.1")), is(true));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("fe80::1:1:1:1"), makeAddressMock("192.168.1.1")), is(true));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("169.254.1.1"), makeAddressMock("169.254.1.1")), is(true));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("169.254.1.1"), makeAddressMock("192.168.1.1")), is(true));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("169.254.1.1"), makeAddressMock("fe80::1:1:1:1")), is(false));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("192.168.1.1"), makeAddressMock("169.254.1.1")), is(true));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("192.168.1.1"), makeAddressMock("192.168.1.1")), is(true));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("192.168.1.1"), makeAddressMock("fe80::1:1:1:1")), is(false));
        }

        @Test
        public void needToUpdateSsdpMessage_IP_V6_ONLY() throws Exception {
            final ControlPoint cp = new ControlPoint(Protocol.IP_V6_ONLY, NetworkUtils.getNetworkInterfaceList());
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("fe80::1:1:1:1"), makeAddressMock("fe80::1:1:1:1")), is(true));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("fe80::1:1:1:1"), makeAddressMock("169.254.1.1")), is(false));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("fe80::1:1:1:1"), makeAddressMock("192.168.1.1")), is(false));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("169.254.1.1"), makeAddressMock("169.254.1.1")), is(false));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("169.254.1.1"), makeAddressMock("192.168.1.1")), is(false));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("169.254.1.1"), makeAddressMock("fe80::1:1:1:1")), is(true));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("192.168.1.1"), makeAddressMock("169.254.1.1")), is(false));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("192.168.1.1"), makeAddressMock("192.168.1.1")), is(false));
            assertThat(cp.needToUpdateSsdpMessage(makeAddressMock("192.168.1.1"), makeAddressMock("fe80::1:1:1:1")), is(true));
        }

        private static SsdpMessage makeAddressMock(@Nonnull final String address) throws Exception {
            final SsdpMessage message = mock(SsdpMessage.class);
            final InterfaceAddress ifa = TestUtils.createInterfaceAddress(address, "255.255.255.255", 24);
            doReturn(ifa).when(message).getInterfaceAddress();
            return message;
        }
    }

    @RunWith(JUnit4.class)
    public static class ネットワーク未使用 {
        private ControlPoint mCp;
        private final SsdpSearchServerList mSsdpSearchServerList = mock(SsdpSearchServerList.class);
        private final SsdpNotifyReceiverList mSsdpNotifyReceiverList = mock(SsdpNotifyReceiverList.class);
        private final EventReceiver mEventReceiver = mock(EventReceiver.class);

        @Before
        public void setUp() throws Exception {
            mCp = spy(new ControlPoint(Protocol.DEFAULT, NetworkUtils.getAvailableInet4Interfaces(),
                    new ControlPointDiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        SsdpSearchServerList createSsdpSearchServerList(
                                @Nonnull final Collection<NetworkInterface> interfaces,
                                @Nonnull final ResponseListener listener) {
                            return mSsdpSearchServerList;
                        }

                        @Nonnull
                        @Override
                        SsdpNotifyReceiverList createSsdpNotifyReceiverList(
                                @Nonnull final Collection<NetworkInterface> interfaces,
                                @Nonnull final NotifyListener listener) {
                            return mSsdpNotifyReceiverList;
                        }

                        @Nonnull
                        @Override
                        EventReceiver createEventReceiver(@Nonnull final EventMessageListener listener) {
                            return mEventReceiver;
                        }
                    }));
        }

        @Test
        public void discoverDevice_onDesicoverが通知される() throws Exception {
            final DiscoveryListener l = mock(DiscoveryListener.class);
            mCp.addDiscoveryListener(l);
            final String uuid = "uuid";
            final Device device = mock(Device.class);
            doReturn(uuid).when(device).getUdn();
            mCp.discoverDevice(device);
            Thread.sleep(100);

            assertThat(mCp.getDevice(uuid), is(device));
            assertThat(mCp.getDeviceList(), hasItem(device));
            assertThat(mCp.getDeviceListSize(), is(1));
            verify(l).onDiscover(device);
        }

        @Test
        public void clearDeviceList_Deviceがクリアされる() throws Exception {
            final DiscoveryListener l = mock(DiscoveryListener.class);
            mCp.addDiscoveryListener(l);
            final String uuid = "uuid";
            final Device device = mock(Device.class);
            doReturn(uuid).when(device).getUdn();
            mCp.discoverDevice(device);
            Thread.sleep(100);

            assertThat(mCp.getDevice(uuid), is(device));
            assertThat(mCp.getDeviceList(), hasItem(device));
            assertThat(mCp.getDeviceListSize(), is(1));
            verify(l, times(1)).onDiscover(device);

            mCp.clearDeviceList();
            Thread.sleep(100);

            assertThat(mCp.getDevice(uuid), is(nullValue()));
            assertThat(mCp.getDeviceList(), not(hasItem(device)));
            assertThat(mCp.getDeviceListSize(), is(0));
            verify(l, times(1)).onLost(device);
        }

        @Test
        public void lostDevice_onLostが通知される() throws Exception {
            final DiscoveryListener l = mock(DiscoveryListener.class);
            mCp.addDiscoveryListener(l);
            final String uuid = "uuid";
            final Device device = mock(Device.class);
            final Service service = mock(Service.class);
            doReturn(uuid).when(device).getUdn();
            doReturn(Collections.singletonList(service)).when(device).getServiceList();
            mCp.discoverDevice(device);
            Thread.sleep(100);
            mCp.lostDevice(device);
            Thread.sleep(100);

            assertThat(mCp.getDevice(uuid), is(nullValue()));
            assertThat(mCp.getDeviceListSize(), is(0));
            verify(l).onLost(device);
            verify(mCp).unregisterSubscribeService(service);
        }

        @Test
        public void stop_lostDeviceが通知される() throws Exception {
            final String uuid = "uuid";
            final Device device = mock(Device.class);
            final Service service = mock(Service.class);
            doReturn(uuid).when(device).getUdn();
            doReturn(System.currentTimeMillis() + 1000L).when(device).getExpireTime();
            doReturn(Collections.singletonList(service)).when(device).getServiceList();
            mCp.start();
            mCp.discoverDevice(device);
            Thread.sleep(100);
            mCp.stop();
            Thread.sleep(100);

            assertThat(mCp.getDevice(uuid), is(nullValue()));
            assertThat(mCp.getDeviceListSize(), is(0));
            verify(mCp).lostDevice(device);
            verify(mCp).unregisterSubscribeService(service);
        }

        @Test
        public void removeDiscoveryListener_削除できる() throws Exception {
            final DiscoveryListener l = mock(DiscoveryListener.class);
            mCp.addDiscoveryListener(l);

            mCp.removeDiscoveryListener(l);

            final String uuid = "uuid";
            final Device device = mock(Device.class);
            doReturn(uuid).when(device).getUdn();
            mCp.discoverDevice(device);
            Thread.sleep(100);

            verify(l, never()).onDiscover(device);
        }

        @Test
        public void addDiscoveryListener_多重登録防止() throws Exception {
            final DiscoveryListener l = mock(DiscoveryListener.class);
            mCp.addDiscoveryListener(l);
            mCp.addDiscoveryListener(l);

            mCp.removeDiscoveryListener(l);

            final String uuid = "uuid";
            final Device device = mock(Device.class);
            doReturn(uuid).when(device).getUdn();
            mCp.discoverDevice(device);
            Thread.sleep(100);

            verify(l, never()).onDiscover(device);
        }


        @Test
        public void registerSubscribeService_による登録() throws Exception {
            final String sid = "sid";
            final Service service = mock(Service.class);
            doReturn(sid).when(service).getSubscriptionId();

            mCp.registerSubscribeService(service, true);
            assertThat(mCp.getSubscribeService(sid), is(service));
        }


        @Test
        public void unregisterSubscribeService_による削除() throws Exception {
            final String sid = "sid";
            final Service service = mock(Service.class);
            doReturn(sid).when(service).getSubscriptionId();

            mCp.registerSubscribeService(service, true);
            mCp.unregisterSubscribeService(service);
            assertThat(mCp.getSubscribeService(sid), is(nullValue()));
        }
    }

    @RunWith(JUnit4.class)
    public static class DeviceDiscovery {
        private ControlPoint mCp;
        private Map<String, DeviceImpl.Builder> mLoadingDeviceMap = spy(new HashMap<String, DeviceImpl.Builder>());
        private DeviceHolder mDeviceHolder;

        @Before
        public void setUp() throws Exception {
            mCp = spy(new ControlPoint(Protocol.DEFAULT, NetworkUtils.getAvailableInet4Interfaces(),
                    new ControlPointDiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        Map<String, DeviceImpl.Builder> createLoadingDeviceMap() {
                            return mLoadingDeviceMap;
                        }

                        @Nonnull
                        @Override
                        DeviceHolder createDeviceHolder(@Nonnull ControlPoint cp) {
                            if (mDeviceHolder == null) {
                                mDeviceHolder = spy(new DeviceHolder(cp));
                            }
                            return mDeviceHolder;
                        }
                    }));
        }

        @Test
        public void onReceiveSsdp_読み込み済みデバイスにないbyebye受信() throws Exception {
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-byebye0.bin");
            final InterfaceAddress ifa = TestUtils.createInterfaceAddress("192.0.2.3", "255.255.255.0", 0);
            final SsdpMessage message = new SsdpRequest(ifa, data, data.length);
            mCp.onReceiveSsdp(message);
            verify(mLoadingDeviceMap).remove(anyString());
        }

        @Test
        public void onReceiveSsdp_読み込み済みデバイスのbyebye受信() throws Exception {
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-byebye0.bin");
            final InterfaceAddress ifa = TestUtils.createInterfaceAddress("192.0.2.3", "255.255.255.0", 0);
            final SsdpMessage message = new SsdpRequest(ifa, data, data.length);
            final Device device = mock(Device.class);
            final String udn = "uuid:01234567-89ab-cdef-0123-456789abcdef";
            doReturn(udn).when(device).getUdn();
            mDeviceHolder.add(device);
            assertThat(mDeviceHolder.get(udn), is(device));
            mCp.onReceiveSsdp(message);
            assertThat(mDeviceHolder.get(udn), is(nullValue()));
        }

        @Test
        public void onReceiveSsdp_alive受信後失敗() throws Exception {
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
            final InterfaceAddress ifa = TestUtils.createInterfaceAddress("192.0.2.3", "255.255.255.0", 0);
            final SsdpMessage message = new SsdpRequest(ifa, data, data.length);
            final String udn = "uuid:01234567-89ab-cdef-0123-456789abcdef";
            doReturn(new HttpClient(true) {
                @Nonnull
                @Override
                public HttpResponse download(@Nonnull URL url) throws IOException {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    throw new IOException();
                }
            }).when(mCp).createHttpClient();
            mCp.onReceiveSsdp(message);
            assertThat(mLoadingDeviceMap, hasKey(udn));
            Thread.sleep(1000); // Exception発生を待つ
            assertThat(mLoadingDeviceMap, not(hasKey(udn)));
            assertThat(mDeviceHolder.size(), is(0));
        }

        @Test
        public void onReceiveSsdp_alive受信後成功() throws Exception {
            final HttpClient httpClient = mock(HttpClient.class);
            doReturn(TestUtils.getResourceAsString("device.xml"))
                    .when(httpClient).downloadString(new URL("http://192.0.2.2:12345/device.xml"));
            doReturn(TestUtils.getResourceAsString("cds.xml"))
                    .when(httpClient).downloadString(new URL("http://192.0.2.2:12345/cds.xml"));
            doReturn(TestUtils.getResourceAsString("cms.xml"))
                    .when(httpClient).downloadString(new URL("http://192.0.2.2:12345/cms.xml"));
            doReturn(TestUtils.getResourceAsString("mmupnp.xml"))
                    .when(httpClient).downloadString(new URL("http://192.0.2.2:12345/mmupnp.xml"));
            doReturn(TestUtils.getResourceAsByteArray("icon/icon120.jpg"))
                    .when(httpClient).downloadBinary(new URL("http://192.0.2.2:12345/icon/icon120.jpg"));
            doReturn(TestUtils.getResourceAsByteArray("icon/icon48.jpg"))
                    .when(httpClient).downloadBinary(new URL("http://192.0.2.2:12345/icon/icon48.jpg"));
            doReturn(TestUtils.getResourceAsByteArray("icon/icon120.png"))
                    .when(httpClient).downloadBinary(new URL("http://192.0.2.2:12345/icon/icon120.png"));
            doReturn(TestUtils.getResourceAsByteArray("icon/icon48.png"))
                    .when(httpClient).downloadBinary(new URL("http://192.0.2.2:12345/icon/icon48.png"));
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
            final InterfaceAddress ifa = TestUtils.createInterfaceAddress("192.0.2.3", "255.255.255.0", 0);
            final SsdpMessage message = new SsdpRequest(ifa, data, data.length);
            final String udn = "uuid:01234567-89ab-cdef-0123-456789abcdef";
            doReturn(httpClient).when(mCp).createHttpClient();
            final IconFilter iconFilter = spy(new IconFilter() {
                @Nonnull
                @Override
                public List<Icon> filter(@Nonnull List<Icon> list) {
                    return Collections.singletonList(list.get(0));
                }
            });
            mCp.setIconFilter(iconFilter);
            mCp.onReceiveSsdp(message);
            Thread.sleep(1000); // 読み込みを待つ
            final Device device = mCp.getDevice(udn);
            verify(iconFilter).filter(ArgumentMatchers.<Icon>anyList());
            assertThat(device.getIconList(), hasSize(4));
            assertThat(device.getIconList().get(0).getBinary(), is(not(nullValue())));
            assertThat(device.getIconList().get(1).getBinary(), is(nullValue()));
            assertThat(device.getIconList().get(2).getBinary(), is(nullValue()));
            assertThat(device.getIconList().get(3).getBinary(), is(nullValue()));
        }

        @Test
        public void onReceiveSsdp_読み込み済みデバイスのalive受信() throws Exception {
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
            final InterfaceAddress ifa = TestUtils.createInterfaceAddress("192.0.2.3", "255.255.255.0", 0);
            final SsdpMessage message = new SsdpRequest(ifa, data, data.length);
            final Device device = mock(Device.class);
            doReturn(message).when(device).getSsdpMessage();
            final String udn = "uuid:01234567-89ab-cdef-0123-456789abcdef";
            doReturn(udn).when(device).getUdn();

            mDeviceHolder.add(device);
            mCp.onReceiveSsdp(message);
        }

        @Test
        public void onReceiveSsdp_ロード中デバイスのalive受信() throws Exception {
            final byte[] data1 = TestUtils.getResourceAsByteArray("ssdp-notify-alive1.bin");
            final InterfaceAddress ifa = TestUtils.createInterfaceAddress("192.0.2.3", "255.255.255.0", 0);
            final SsdpMessage message1 = new SsdpRequest(ifa, data1, data1.length);
            final DeviceImpl.Builder deviceBuilder = spy(new DeviceImpl.Builder(mCp, message1));
            mLoadingDeviceMap.put(deviceBuilder.getUuid(), deviceBuilder);
            final byte[] data2 = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
            final SsdpMessage message2 = new SsdpRequest(ifa, data2, data2.length);
            mCp.onReceiveSsdp(message2);
            verify(deviceBuilder).updateSsdpMessage(message2);
        }
    }

    @RunWith(JUnit4.class)
    public static class イベント伝搬テスト {
        private ControlPoint mCp;
        private Map<String, DeviceImpl.Builder> mLoadingDeviceMap = spy(new HashMap<String, DeviceImpl.Builder>());
        private DeviceHolder mDeviceHolder;
        private SubscribeHolder mSubscribeHolder = new SubscribeHolder();
        private SsdpSearchServerList mSsdpSearchServerList = mock(SsdpSearchServerList.class);
        private SsdpNotifyReceiverList mSsdpNotifyReceiverList = mock(SsdpNotifyReceiverList.class);
        private EventReceiver mEventReceiver = mock(EventReceiver.class);
        private ResponseListener mResponseListener;
        private NotifyListener mNotifyListener;

        @Before
        public void setUp() throws Exception {
            mCp = spy(new ControlPoint(Protocol.DEFAULT, NetworkUtils.getAvailableInet4Interfaces(),
                    new ControlPointDiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        Map<String, DeviceImpl.Builder> createLoadingDeviceMap() {
                            return mLoadingDeviceMap;
                        }

                        @Nonnull
                        @Override
                        DeviceHolder createDeviceHolder(@Nonnull ControlPoint cp) {
                            if (mDeviceHolder == null) {
                                mDeviceHolder = spy(new DeviceHolder(cp));
                            }
                            return mDeviceHolder;
                        }

                        @Nonnull
                        @Override
                        SubscribeHolder createSubscribeHolder() {
                            return mSubscribeHolder;
                        }

                        @Nonnull
                        @Override
                        SsdpSearchServerList createSsdpSearchServerList(
                                @Nonnull final Collection<NetworkInterface> interfaces,
                                @Nonnull final ResponseListener listener) {
                            mResponseListener = listener;
                            return mSsdpSearchServerList;
                        }

                        @Nonnull
                        @Override
                        SsdpNotifyReceiverList createSsdpNotifyReceiverList(
                                @Nonnull final Collection<NetworkInterface> interfaces,
                                @Nonnull final NotifyListener listener) {
                            mNotifyListener = listener;
                            return mSsdpNotifyReceiverList;
                        }

                        @Nonnull
                        @Override
                        EventReceiver createEventReceiver(@Nonnull final EventMessageListener listener) {
                            return mEventReceiver;
                        }
                    }));
        }

        @Test
        public void stop時にunsubscribeとlostが発生すること() throws Exception {
            mCp.start();
            final Device device = mock(Device.class);
            doReturn("udn").when(device).getUdn();
            mDeviceHolder.add(device);
            final Service service = mock(Service.class);
            doReturn("SubscriptionId").when(service).getSubscriptionId();
            doReturn(System.currentTimeMillis() + 1000).when(service).getSubscriptionExpiryTime();
            mSubscribeHolder.add(service, false);
            mCp.stop();
            mCp.terminate();
            Thread.sleep(100);
            verify(service).unsubscribe();
            verify(mDeviceHolder).remove(device);
        }

        @Test
        public void stop時にunsubscribeでexceptionが発生しても無視する() throws Exception {
            mCp.start();
            final Device device = mock(Device.class);
            doReturn("udn").when(device).getUdn();
            mDeviceHolder.add(device);
            final Service service = mock(Service.class);
            doReturn("SubscriptionId").when(service).getSubscriptionId();
            doReturn(System.currentTimeMillis() + 1000).when(service).getSubscriptionExpiryTime();
            doThrow(new IOException()).when(service).unsubscribe();
            mSubscribeHolder.add(service, false);
            mCp.stop();
            mCp.terminate();
            Thread.sleep(100);
            verify(service).unsubscribe();
            verify(mDeviceHolder).remove(device);
        }

        @Test
        public void onReceiveSsdp_ResponseListenerから伝搬() throws Exception {
            final String udn = "uuid:01234567-89ab-cdef-0123-456789abcdef";
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin");
            final SsdpResponse message = new SsdpResponse(mock(InterfaceAddress.class), data, data.length);
            mResponseListener.onReceiveResponse(message);
            Thread.sleep(100);
            verify(mDeviceHolder).get(udn);
        }


        @Test
        public void onReceiveSsdp_NotifyListenerから伝搬() throws Exception {
            final String udn = "uuid:01234567-89ab-cdef-0123-456789abcdef";
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-byebye0.bin");
            final SsdpRequest message = new SsdpRequest(mock(InterfaceAddress.class), data, data.length);
            mNotifyListener.onReceiveNotify(message);
            Thread.sleep(100);
            verify(mDeviceHolder).get(udn);
        }
    }

    @RunWith(JUnit4.class)
    public static class EventReceiverに起因するテスト {
        private static final int PORT = 1234;
        private ControlPoint mCp;
        private EventReceiver mEventReceiver;
        private EventMessageListener mEventMessageListener;

        @Before
        public void setUp() throws Exception {
            mEventReceiver = mock(EventReceiver.class);
            doReturn(PORT).when(mEventReceiver).getLocalPort();

            mCp = spy(new ControlPoint(Protocol.DEFAULT, NetworkUtils.getAvailableInet4Interfaces(),
                    new ControlPointDiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        SsdpSearchServerList createSsdpSearchServerList(
                                @Nonnull final Collection<NetworkInterface> interfaces,
                                @Nonnull final ResponseListener listener) {
                            return mock(SsdpSearchServerList.class);
                        }

                        @Nonnull
                        @Override
                        SsdpNotifyReceiverList createSsdpNotifyReceiverList(
                                @Nonnull final Collection<NetworkInterface> interfaces,
                                @Nonnull final NotifyListener listener) {
                            return mock(SsdpNotifyReceiverList.class);
                        }

                        @Nonnull
                        @Override
                        EventReceiver createEventReceiver(@Nonnull final EventMessageListener listener) {
                            mEventMessageListener = listener;
                            return mEventReceiver;
                        }
                    }));
        }

        @Test
        public void getEventPort_EventReceiverのportが返る() {
            assertThat(mCp.getEventPort(), is(PORT));
        }

        @Test
        public void notifyEvent_イベントがリスナーに通知されること() throws Exception {
            final String sid = "sid";
            final Service service = mock(Service.class);
            doReturn(sid).when(service).getSubscriptionId();

            final String variableName = "variable";
            final StateVariable variable = mock(StateVariable.class);
            doReturn(true).when(variable).isSendEvents();
            doReturn(variableName).when(variable).getName();
            doReturn(variable).when(service).findStateVariable(variableName);

            mCp.registerSubscribeService(service, false);

            final NotifyEventListener l = mock(NotifyEventListener.class);
            mCp.addNotifyEventListener(l);

            final String value = "value";
            mEventMessageListener.onEventReceived(sid, 0, Collections.singletonList(new StringPair(variableName, value)));

            Thread.sleep(200);

            verify(l).onNotifyEvent(service, 0, variableName, value);
        }

        @Test
        public void notifyEvent_削除したリスナーに通知されないこと() throws Exception {
            final String sid = "sid";
            final Service service = mock(Service.class);
            doReturn(sid).when(service).getSubscriptionId();

            final String variableName = "variable";
            final StateVariable variable = mock(StateVariable.class);
            doReturn(true).when(variable).isSendEvents();
            doReturn(variableName).when(variable).getName();
            doReturn(variable).when(service).findStateVariable(variableName);

            mCp.registerSubscribeService(service, false);

            final NotifyEventListener l = mock(NotifyEventListener.class);
            mCp.addNotifyEventListener(l);
            mCp.removeNotifyEventListener(l);

            final String value = "value";
            mEventMessageListener.onEventReceived(sid, 0, Collections.singletonList(new StringPair(variableName, value)));
            Thread.sleep(100);

            verify(l, never()).onNotifyEvent(service, 0, variableName, value);
        }


        @Test
        public void notifyEvent_対応する変数のないイベントが無視されること() throws Exception {
            final String sid = "sid";
            final Service service = mock(Service.class);
            doReturn(sid).when(service).getSubscriptionId();

            final String variableName = "variable";
            final StateVariable variable = mock(StateVariable.class);
            doReturn(true).when(variable).isSendEvents();
            doReturn(variableName).when(variable).getName();
            doReturn(variable).when(service).findStateVariable(variableName);

            mCp.registerSubscribeService(service, false);

            final NotifyEventListener l = mock(NotifyEventListener.class);
            mCp.addNotifyEventListener(l);

            final String value = "value";
            mEventMessageListener.onEventReceived(sid, 0, Collections.singletonList(new StringPair(variableName + 1, value)));
            Thread.sleep(100);

            verify(l, never()).onNotifyEvent(service, 0, variableName, value);
        }
    }

    @RunWith(JUnit4.class)
    public static class Executorのテスト {
        @Test
        public void executeInParallel_executeが実行される() throws Exception {
            final ExecutorService executor = mock(ExecutorService.class);
            final ControlPoint cp = new ControlPoint(Protocol.DEFAULT, NetworkUtils.getAvailableInet4Interfaces(),
                    new ControlPointDiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        ExecutorService createIoExecutor() {
                            return executor;
                        }
                    });
            final Runnable command = mock(Runnable.class);

            assertThat(cp.executeInParallel(command), is(true));

            verify(executor, times(1)).execute(command);
        }

        @Test
        public void executeInParallel_shutdown済みなら何もしないでfalse() throws Exception {
            final ExecutorService executor = mock(ExecutorService.class);
            final ControlPoint cp = new ControlPoint(Protocol.DEFAULT, NetworkUtils.getAvailableInet4Interfaces(),
                    new ControlPointDiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        ExecutorService createIoExecutor() {
                            return executor;
                        }
                    });
            doReturn(true).when(executor).isShutdown();
            final Runnable command = mock(Runnable.class);

            assertThat(cp.executeInParallel(command), is(false));

            verify(executor, never()).execute(command);
        }

        @Test
        public void executeInParallel_exceptionが発生すればfalse() throws Exception {
            final ExecutorService executor = mock(ExecutorService.class);
            final ControlPoint cp = new ControlPoint(Protocol.DEFAULT, NetworkUtils.getAvailableInet4Interfaces(),
                    new ControlPointDiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        ExecutorService createIoExecutor() {
                            return executor;
                        }
                    });
            final Runnable command = mock(Runnable.class);
            doThrow(new RejectedExecutionException()).when(executor).execute(command);

            assertThat(cp.executeInParallel(command), is(false));
        }

        @Test
        public void executeInSequential_executeが実行される() throws Exception {
            final ExecutorService executor = mock(ExecutorService.class);
            final ControlPoint cp = new ControlPoint(Protocol.DEFAULT, NetworkUtils.getAvailableInet4Interfaces(),
                    new ControlPointDiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        ExecutorService createNotifyExecutor() {
                            return executor;
                        }
                    });
            final Runnable command = mock(Runnable.class);

            assertThat(cp.executeInSequential(command), is(true));

            verify(executor, times(1)).execute(command);
        }

        @Test
        public void executeInSequential_shutdown済みなら何もしないでfalse() throws Exception {
            final ExecutorService executor = mock(ExecutorService.class);
            final ControlPoint cp = new ControlPoint(Protocol.DEFAULT, NetworkUtils.getAvailableInet4Interfaces(),
                    new ControlPointDiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        ExecutorService createNotifyExecutor() {
                            return executor;
                        }
                    });
            doReturn(true).when(executor).isShutdown();
            final Runnable command = mock(Runnable.class);

            assertThat(cp.executeInSequential(command), is(false));

            verify(executor, never()).execute(command);
        }

        @Test
        public void executeInSequential_exceptionが発生すればfalse() throws Exception {
            final ExecutorService executor = mock(ExecutorService.class);
            final ControlPoint cp = new ControlPoint(Protocol.DEFAULT, NetworkUtils.getAvailableInet4Interfaces(),
                    new ControlPointDiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        ExecutorService createNotifyExecutor() {
                            return executor;
                        }
                    });
            final Runnable command = mock(Runnable.class);
            doThrow(new RejectedExecutionException()).when(executor).execute(command);

            assertThat(cp.executeInSequential(command), is(false));
        }
    }
}
