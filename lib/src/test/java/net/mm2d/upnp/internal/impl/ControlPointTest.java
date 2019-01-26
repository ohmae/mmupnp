/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl;

import net.mm2d.upnp.ControlPoint;
import net.mm2d.upnp.ControlPoint.DiscoveryListener;
import net.mm2d.upnp.ControlPoint.NotifyEventListener;
import net.mm2d.upnp.ControlPointFactory;
import net.mm2d.upnp.Device;
import net.mm2d.upnp.HttpClient;
import net.mm2d.upnp.Icon;
import net.mm2d.upnp.IconFilter;
import net.mm2d.upnp.Protocol;
import net.mm2d.upnp.Service;
import net.mm2d.upnp.SsdpMessage;
import net.mm2d.upnp.SsdpMessageFilter;
import net.mm2d.upnp.StateVariable;
import net.mm2d.upnp.internal.manager.DeviceHolder;
import net.mm2d.upnp.internal.manager.DeviceHolder.ExpireListener;
import net.mm2d.upnp.internal.manager.SubscribeManager;
import net.mm2d.upnp.internal.message.HttpResponse;
import net.mm2d.upnp.internal.message.SsdpRequest;
import net.mm2d.upnp.internal.message.SsdpResponse;
import net.mm2d.upnp.internal.server.SsdpNotifyReceiver.NotifyListener;
import net.mm2d.upnp.internal.server.SsdpNotifyReceiverList;
import net.mm2d.upnp.internal.server.SsdpSearchServer.ResponseListener;
import net.mm2d.upnp.internal.server.SsdpSearchServerList;
import net.mm2d.upnp.internal.thread.TaskHandler;
import net.mm2d.util.NetworkUtils;
import net.mm2d.util.StringPair;
import net.mm2d.util.TestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(Enclosed.class)
public class ControlPointTest {
    @RunWith(JUnit4.class)
    public static class mock未使用 {
        @Test(expected = IllegalStateException.class)
        public void constructor_インターフェース空で指定() throws Exception {
            new ControlPointImpl(Protocol.DEFAULT, Collections.emptyList(), false, mock(DiFactory.class));
        }

        @Test(timeout = 2000L)
        public void initialize_terminate() throws Exception {
            final ControlPoint cp = ControlPointFactory.create();
            cp.initialize();
            cp.terminate();
        }

        @Test(timeout = 2000L)
        public void initialize_initialize_terminate() throws Exception {
            final ControlPoint cp = ControlPointFactory.create();
            cp.initialize();
            cp.initialize();
            cp.terminate();
        }

        @Test(timeout = 2000L)
        public void initialize_terminate_intercept() throws Exception {
            final Thread thread = new Thread(() -> {
                final ControlPoint cp = ControlPointFactory.create();
                cp.initialize();
                cp.terminate();
            });
            thread.start();
            Thread.sleep(200);
            thread.interrupt();
            thread.join();
        }

        @Test(timeout = 1000L)
        public void terminate() throws Exception {
            final ControlPoint cp = ControlPointFactory.create((Collection<NetworkInterface>) null);
            cp.terminate();
        }

        @Test(timeout = 10000L)
        public void start_stop() throws Exception {
            final ControlPoint cp = ControlPointFactory.create();
            cp.initialize();
            cp.start();
            cp.stop();
            cp.terminate();
        }

        @Test(timeout = 10000L)
        public void start_stop2() throws Exception {
            final ControlPoint cp = ControlPointFactory.create();
            cp.initialize();
            cp.start();
            cp.stop();
            cp.stop();
            cp.terminate();
        }

        @Test(timeout = 10000L)
        public void start_stop_illegal() throws Exception {
            final ControlPoint cp = ControlPointFactory.create();
            cp.start();
            cp.start();
            cp.terminate();
            cp.terminate();
        }

        @Test(expected = IllegalStateException.class)
        public void search_not_started() throws Exception {
            final ControlPoint cp = ControlPointFactory.create();
            cp.search();
        }

        @Test
        public void search() throws Exception {
            final SsdpSearchServerList list = mock(SsdpSearchServerList.class);
            final ControlPoint cp = new ControlPointImpl(Protocol.DEFAULT,
                    NetworkUtils.getAvailableInet4Interfaces(),
                    false,
                    new DiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        public SsdpSearchServerList createSsdpSearchServerList(
                                @Nonnull final Collection<NetworkInterface> interfaces,
                                @Nonnull final ResponseListener listener) {
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
            final ControlPointImpl cp = (ControlPointImpl) ControlPointFactory.create();
            final HttpClient client = cp.createHttpClient();
            assertThat(client.isKeepAlive(), is(true));
        }

        @Test
        public void needToUpdateSsdpMessage_DUAL_STACK() throws Exception {
            final ControlPointImpl cp = (ControlPointImpl) ControlPointFactory.create(Protocol.DUAL_STACK, NetworkUtils.getNetworkInterfaceList());
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
            final ControlPointImpl cp = (ControlPointImpl) ControlPointFactory.create(Protocol.IP_V4_ONLY, NetworkUtils.getNetworkInterfaceList());
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
            final ControlPointImpl cp = (ControlPointImpl) ControlPointFactory.create(Protocol.IP_V6_ONLY, NetworkUtils.getNetworkInterfaceList());
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
            final InetAddress inetAddress = InetAddress.getByName(address);
            doReturn(inetAddress).when(message).getLocalAddress();
            return message;
        }

        @Test
        public void setIconFilter_nullを指定しても問題ない() throws Exception {
            final ControlPoint cp = ControlPointFactory.create();
            cp.setIconFilter(null);
        }
    }

    @RunWith(JUnit4.class)
    public static class ネットワーク未使用 {
        private ControlPointImpl mCp;
        private final SsdpSearchServerList mSsdpSearchServerList = mock(SsdpSearchServerList.class);
        private final SsdpNotifyReceiverList mSsdpNotifyReceiverList = mock(SsdpNotifyReceiverList.class);
        private final TaskHandler mTaskHandler = mock(TaskHandler.class);
        private final NotifyEventListener mNotifyEventListener = mock(NotifyEventListener.class);
        private final DiFactory mDiFactory = spy(new DiFactory());
        private final SubscribeManager mSubscribeManager = spy(new SubscribeManager(mTaskHandler, mNotifyEventListener, mDiFactory));

        @Before
        public void setUp() throws Exception {
            mCp = spy(new ControlPointImpl(Protocol.DEFAULT,
                    NetworkUtils.getAvailableInet4Interfaces(), false,
                    new DiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        public SsdpSearchServerList createSsdpSearchServerList(
                                @Nonnull final Collection<NetworkInterface> interfaces,
                                @Nonnull final ResponseListener listener) {
                            return mSsdpSearchServerList;
                        }

                        @Nonnull
                        @Override
                        public SsdpNotifyReceiverList createSsdpNotifyReceiverList(
                                @Nonnull final Collection<NetworkInterface> interfaces,
                                @Nonnull final NotifyListener listener) {
                            return mSsdpNotifyReceiverList;
                        }

                        @Nonnull
                        @Override
                        public SubscribeManager createSubscribeManager(
                                @Nonnull final TaskHandler taskHandler,
                                @Nonnull final NotifyEventListener listener) {
                            return mSubscribeManager;
                        }
                    }));
        }

        @Test
        public void discoverDevice_onDiscoverが通知される() throws Exception {
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
            verify(mSubscribeManager).unregister(service);
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
            verify(mSubscribeManager).unregister(service);
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

            mSubscribeManager.register(service, 1000L, true);
            assertThat(mSubscribeManager.getSubscribeService(sid), is(service));
        }


        @Test
        public void unregisterSubscribeService_による削除() throws Exception {
            final String sid = "sid";
            final Service service = mock(Service.class);
            doReturn(sid).when(service).getSubscriptionId();

            mSubscribeManager.register(service, 1000L, true);
            mSubscribeManager.unregister(service);
            assertThat(mSubscribeManager.getSubscribeService(sid), is(nullValue()));
        }
    }

    @RunWith(JUnit4.class)
    public static class DeviceDiscovery {
        private ControlPointImpl mCp;
        private final Map<String, DeviceImpl.Builder> mLoadingDeviceMap = spy(new HashMap<>());
        private DeviceHolder mDeviceHolder;

        @Before
        public void setUp() throws Exception {
            mCp = spy(new ControlPointImpl(Protocol.DEFAULT,
                    NetworkUtils.getAvailableInet4Interfaces(), false,
                    new DiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        public Map<String, DeviceImpl.Builder> createLoadingDeviceMap() {
                            return mLoadingDeviceMap;
                        }

                        @Nonnull
                        @Override
                        public DeviceHolder createDeviceHolder(@Nonnull final ExpireListener listener) {
                            if (mDeviceHolder == null) {
                                mDeviceHolder = spy(new DeviceHolder(listener));
                            }
                            return mDeviceHolder;
                        }
                    }));
        }

        @Test
        public void onReceiveSsdp_読み込み済みデバイスにないbyebye受信() throws Exception {
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-byebye0.bin");
            final InetAddress addr = InetAddress.getByName("192.0.2.3");
            final SsdpMessage message = new SsdpRequest(addr, data, data.length);
            mCp.onAcceptSsdpMessage(message);
            verify(mLoadingDeviceMap).remove(anyString());
        }

        @Test
        public void onReceiveSsdp_読み込み済みデバイスのbyebye受信() throws Exception {
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-byebye0.bin");
            final InetAddress addr = InetAddress.getByName("192.0.2.3");
            final SsdpMessage message = new SsdpRequest(addr, data, data.length);
            final Device device = mock(Device.class);
            final String udn = "uuid:01234567-89ab-cdef-0123-456789abcdef";
            doReturn(udn).when(device).getUdn();
            mDeviceHolder.add(device);
            assertThat(mDeviceHolder.get(udn), is(device));
            mCp.onAcceptSsdpMessage(message);
            assertThat(mDeviceHolder.get(udn), is(nullValue()));
        }

        @Test
        public void onReceiveSsdp_alive受信後失敗() throws Exception {
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
            final InetAddress addr = InetAddress.getByName("192.0.2.3");
            final SsdpMessage message = new SsdpRequest(addr, data, data.length);
            final String udn = "uuid:01234567-89ab-cdef-0123-456789abcdef";
            doReturn(new HttpClient(true) {
                @Nonnull
                @Override
                public HttpResponse download(@Nonnull final URL url) throws IOException {
                    try {
                        Thread.sleep(100);
                    } catch (final InterruptedException e) {
                    }
                    throw new IOException();
                }
            }).when(mCp).createHttpClient();
            mCp.onAcceptSsdpMessage(message);
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
            final InetAddress address = InetAddress.getByName("192.0.2.3");
            final SsdpMessage message = new SsdpRequest(address, data, data.length);
            final String udn = "uuid:01234567-89ab-cdef-0123-456789abcdef";
            doReturn(httpClient).when(mCp).createHttpClient();
            final IconFilter iconFilter = spy(new IconFilter() {
                @Nonnull
                @Override
                public List<Icon> filter(@Nonnull final List<Icon> list) {
                    return Collections.singletonList(list.get(0));
                }
            });
            mCp.setIconFilter(iconFilter);
            mCp.onAcceptSsdpMessage(message);
            Thread.sleep(1000); // 読み込みを待つ
            final Device device = mCp.getDevice(udn);
            verify(iconFilter).filter(ArgumentMatchers.anyList());
            assertThat(device.getIconList(), hasSize(4));
            assertThat(device.getIconList().get(0).getBinary(), is(not(nullValue())));
            assertThat(device.getIconList().get(1).getBinary(), is(nullValue()));
            assertThat(device.getIconList().get(2).getBinary(), is(nullValue()));
            assertThat(device.getIconList().get(3).getBinary(), is(nullValue()));
        }

        @Test
        public void onReceiveSsdp_読み込み済みデバイスのalive受信() throws Exception {
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
            final InetAddress addr = InetAddress.getByName("192.0.2.3");
            final SsdpMessage message = new SsdpRequest(addr, data, data.length);
            final Device device = mock(Device.class);
            doReturn(message).when(device).getSsdpMessage();
            final String udn = "uuid:01234567-89ab-cdef-0123-456789abcdef";
            doReturn(udn).when(device).getUdn();

            mDeviceHolder.add(device);
            mCp.onAcceptSsdpMessage(message);
        }

        @Test
        public void onReceiveSsdp_ロード中デバイスのalive受信() throws Exception {
            final byte[] data1 = TestUtils.getResourceAsByteArray("ssdp-notify-alive1.bin");
            final InetAddress addr = InetAddress.getByName("192.0.2.3");
            final SsdpMessage message1 = new SsdpRequest(addr, data1, data1.length);
            final DeviceImpl.Builder deviceBuilder = spy(new DeviceImpl.Builder(mCp, mock(SubscribeManager.class), message1));
            mLoadingDeviceMap.put(deviceBuilder.getUuid(), deviceBuilder);
            final byte[] data2 = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
            final SsdpMessage message2 = new SsdpRequest(addr, data2, data2.length);
            mCp.onAcceptSsdpMessage(message2);
            verify(deviceBuilder).updateSsdpMessage(message2);
        }
    }

    @RunWith(JUnit4.class)
    public static class PinnedDevice {
        private ControlPointImpl mCp;
        private HttpClient mHttpClient;

        @Before
        public void setUp() throws Exception {
            mCp = spy(new ControlPointImpl(Protocol.DEFAULT,
                    NetworkUtils.getAvailableInet4Interfaces(), false,
                    new DiFactory(Protocol.DEFAULT)));

            mHttpClient = mock(HttpClient.class);
            doReturn(TestUtils.getResourceAsString("device.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/device.xml"));
            doReturn(TestUtils.getResourceAsString("cds.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/cds.xml"));
            doReturn(TestUtils.getResourceAsString("cms.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/cms.xml"));
            doReturn(TestUtils.getResourceAsString("mmupnp.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/mmupnp.xml"));
            doReturn(mHttpClient).when(mCp).createHttpClient();
            doReturn(InetAddress.getByName("192.0.2.3")).when(mHttpClient).getLocalAddress();
        }

        @Test
        public void tryAddPinnedDevice() throws Exception {
            mCp.tryAddPinnedDevice("http://192.0.2.2:12345/device.xml");
            Thread.sleep(1000); // 読み込みを待つ
            assertThat(mCp.getDeviceListSize(), is(1));
            final Device device = mCp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcdef");
            assertThat(device.isPinned(), is(true));
        }

        @Test
        public void tryAddPinnedDevice2回目() throws Exception {
            mCp.tryAddPinnedDevice("http://192.0.2.2:12345/device.xml");
            Thread.sleep(1000); // 読み込みを待つ
            assertThat(mCp.getDeviceListSize(), is(1));
            final Device device = mCp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcdef");
            assertThat(device.isPinned(), is(true));

            mCp.tryAddPinnedDevice("http://192.0.2.2:12345/device.xml");
        }

        @Test
        public void tryAddPinnedDevice_すでに発見済み() throws Exception {
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
            final InetAddress address = InetAddress.getByName("192.0.2.3");
            final SsdpMessage message = new SsdpRequest(address, data, data.length);
            mCp.onAcceptSsdpMessage(message);
            Thread.sleep(1000); // 読み込みを待つ
            mCp.tryAddPinnedDevice("http://192.0.2.2:12345/device.xml");
            Thread.sleep(1000); // 読み込みを待つ
            assertThat(mCp.getDeviceListSize(), is(1));
            final Device device = mCp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcdef");
            assertThat(device.isPinned(), is(true));
        }

        @Test
        public void tryAddPinnedDeviceの後に発見() throws Exception {
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
            final InetAddress address = InetAddress.getByName("192.0.2.3");
            final SsdpMessage message = new SsdpRequest(address, data, data.length);
            mCp.onAcceptSsdpMessage(message);
            Thread.sleep(1000); // 読み込みを待つ
            final Device device = mCp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcdef");
            assertThat(device.isPinned(), is(false));

            mCp.tryAddPinnedDevice("http://192.0.2.2:12345/device.xml");
            Thread.sleep(1000); // 読み込みを待つ
            assertThat(mCp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcdef").isPinned(), is(true));

            mCp.discoverDevice(device);
            assertThat(mCp.getDevice("uuid:01234567-89ab-cdef-0123-456789abcdef").isPinned(), is(true));
        }

        @Test
        public void tryAddPinnedDevice_Exceptionが発生してもクラッシュしない() throws Exception {
            doThrow(new IOException()).when(mHttpClient).downloadString(any());
            mCp.tryAddPinnedDevice("http://192.0.2.2:12345/device.xml");
            Thread.sleep(1000); // 読み込みを待つ
        }

        @Test
        public void removePinnedDevice() throws Exception {
            mCp.tryAddPinnedDevice("http://192.0.2.2:12345/device.xml");
            Thread.sleep(1000); // 読み込みを待つ
            assertThat(mCp.getDeviceListSize(), is(1));

            mCp.removePinnedDevice("http://192.0.2.2:12345/device.xml");
            assertThat(mCp.getDeviceListSize(), is(0));
        }

        @Test
        public void removePinnedDevice_before_load() throws Exception {
            mCp.tryAddPinnedDevice("http://192.0.2.2:12345/device.xml");
            assertThat(mCp.getDeviceListSize(), is(0));
            mCp.removePinnedDevice("http://192.0.2.2:12345/device.xml");
            Thread.sleep(1000); // 読み込みを待つ
            assertThat(mCp.getDeviceListSize(), is(0));
        }
    }

    @RunWith(JUnit4.class)
    public static class イベント伝搬テスト {
        private ControlPointImpl mCp;
        private SubscribeManager mSubscribeManager;
        private final Map<String, DeviceImpl.Builder> mLoadingDeviceMap = spy(new HashMap<>());
        private DeviceHolder mDeviceHolder;
        private final SsdpSearchServerList mSsdpSearchServerList = mock(SsdpSearchServerList.class);
        private final SsdpNotifyReceiverList mSsdpNotifyReceiverList = mock(SsdpNotifyReceiverList.class);
        private ResponseListener mResponseListener;
        private NotifyListener mNotifyListener;

        @Before
        public void setUp() throws Exception {
            mCp = spy(new ControlPointImpl(Protocol.DEFAULT,
                    NetworkUtils.getAvailableInet4Interfaces(), false,
                    new DiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        public Map<String, DeviceImpl.Builder> createLoadingDeviceMap() {
                            return mLoadingDeviceMap;
                        }

                        @Nonnull
                        @Override
                        public DeviceHolder createDeviceHolder(@Nonnull final ExpireListener listener) {
                            if (mDeviceHolder == null) {
                                mDeviceHolder = spy(new DeviceHolder(listener));
                            }
                            return mDeviceHolder;
                        }

                        @Nonnull
                        @Override
                        public SsdpSearchServerList createSsdpSearchServerList(
                                @Nonnull final Collection<NetworkInterface> interfaces,
                                @Nonnull final ResponseListener listener) {
                            mResponseListener = listener;
                            return mSsdpSearchServerList;
                        }

                        @Nonnull
                        @Override
                        public SsdpNotifyReceiverList createSsdpNotifyReceiverList(
                                @Nonnull final Collection<NetworkInterface> interfaces,
                                @Nonnull final NotifyListener listener) {
                            mNotifyListener = listener;
                            return mSsdpNotifyReceiverList;
                        }

                        @Nonnull
                        @Override
                        public SubscribeManager createSubscribeManager(
                                @Nonnull final TaskHandler taskHandler,
                                @Nonnull final NotifyEventListener listener) {
                            mSubscribeManager = spy(new SubscribeManager(taskHandler, listener, this));
                            return mSubscribeManager;
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
            mSubscribeManager.register(service, 1000L, false);
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
            doThrow(new IOException()).when(service).unsubscribe();
            mSubscribeManager.register(service, 1000L, false);
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
            final SsdpResponse message = new SsdpResponse(mock(InetAddress.class), data, data.length);
            mResponseListener.onReceiveResponse(message);
            Thread.sleep(100);
            verify(mDeviceHolder).get(udn);
        }


        @Test
        public void onReceiveSsdp_NotifyListenerから伝搬() throws Exception {
            final String udn = "uuid:01234567-89ab-cdef-0123-456789abcdef";
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-byebye0.bin");
            final SsdpRequest message = new SsdpRequest(mock(InetAddress.class), data, data.length);
            mNotifyListener.onReceiveNotify(message);
            Thread.sleep(100);
            verify(mDeviceHolder).get(udn);
        }
    }

    @RunWith(JUnit4.class)
    public static class EventReceiverに起因するテスト {
        private ControlPointImpl mCp;
        private SubscribeManager mSubscribeManager;

        @Before
        public void setUp() throws Exception {
            mCp = spy(new ControlPointImpl(Protocol.DEFAULT,
                    NetworkUtils.getAvailableInet4Interfaces(), false,
                    new DiFactory(Protocol.DEFAULT) {
                        @Nonnull
                        @Override
                        public SsdpSearchServerList createSsdpSearchServerList(
                                @Nonnull final Collection<NetworkInterface> interfaces,
                                @Nonnull final ResponseListener listener) {
                            return mock(SsdpSearchServerList.class);
                        }

                        @Nonnull
                        @Override
                        public SsdpNotifyReceiverList createSsdpNotifyReceiverList(
                                @Nonnull final Collection<NetworkInterface> interfaces,
                                @Nonnull final NotifyListener listener) {
                            return mock(SsdpNotifyReceiverList.class);
                        }


                        @Nonnull
                        @Override
                        public SubscribeManager createSubscribeManager(
                                @Nonnull final TaskHandler taskHandler,
                                @Nonnull final NotifyEventListener listener) {
                            mSubscribeManager = spy(new SubscribeManager(taskHandler, listener, this));
                            return mSubscribeManager;
                        }
                    }));
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

            mSubscribeManager.register(service, 1000L, false);

            final NotifyEventListener l = mock(NotifyEventListener.class);
            mCp.addNotifyEventListener(l);

            final String value = "value";
            mSubscribeManager.onEventReceived(sid, 0, Collections.singletonList(new StringPair(variableName, value)));

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

            mSubscribeManager.register(service, 1000L, false);

            final NotifyEventListener l = mock(NotifyEventListener.class);
            mCp.addNotifyEventListener(l);
            mCp.removeNotifyEventListener(l);

            final String value = "value";
            mSubscribeManager.onEventReceived(sid, 0, Collections.singletonList(new StringPair(variableName, value)));
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

            mSubscribeManager.register(service, 1000L, false);

            final NotifyEventListener l = mock(NotifyEventListener.class);
            mCp.addNotifyEventListener(l);

            final String value = "value";
            mSubscribeManager.onEventReceived(sid, 0, Collections.singletonList(new StringPair(variableName + 1, value)));
            Thread.sleep(100);

            verify(l, never()).onNotifyEvent(service, 0, variableName, value);
        }
    }

    @RunWith(JUnit4.class)
    public static class SsdpMessageFilterのテスト {
        private ControlPointImpl mCp;
        private SsdpMessage mSsdpMessage;

        @Before
        public void setUp() throws Exception {
            mCp = spy(new ControlPointImpl(Protocol.DEFAULT,
                    NetworkUtils.getAvailableInet4Interfaces(), false,
                    new DiFactory(Protocol.DEFAULT)));
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
            final InetAddress addr = InetAddress.getByName("192.0.2.3");
            mSsdpMessage = new SsdpRequest(addr, data, data.length);
        }

        @Test
        public void デフォルトでは受け付ける() throws Exception {
            mCp.onReceiveSsdpMessage(mSsdpMessage);
            verify(mCp).onAcceptSsdpMessage(mSsdpMessage);
        }

        @Test
        public void filterが機能する() throws Exception {
            final ArgumentCaptor<SsdpMessage> captor = ArgumentCaptor.forClass(SsdpMessage.class);
            final SsdpMessageFilter filter = mock(SsdpMessageFilter.class);
            doReturn(false).when(filter).accept(captor.capture());
            mCp.setSsdpMessageFilter(filter);
            mCp.onReceiveSsdpMessage(mSsdpMessage);
            assertThat(captor.getValue(), is(mSsdpMessage));
            verify(mCp, never()).onAcceptSsdpMessage(any());
        }

        @Test
        public void filterにnullを指定すると受け付ける() throws Exception {
            mCp.setSsdpMessageFilter(message -> false);
            mCp.onReceiveSsdpMessage(mSsdpMessage);
            verify(mCp, never()).onAcceptSsdpMessage(any());
            mCp.setSsdpMessageFilter(null);
            mCp.onReceiveSsdpMessage(mSsdpMessage);
            verify(mCp).onAcceptSsdpMessage(mSsdpMessage);
        }
    }
}
