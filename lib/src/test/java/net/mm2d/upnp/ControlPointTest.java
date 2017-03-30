/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
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

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.NetworkInterface;
import java.util.Collection;
import java.util.Collections;

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

        @Test(timeout = 1000L)
        public void initialize_terminate() throws Exception {
            final ControlPoint cp = new ControlPoint();
            cp.initialize();
            cp.terminate();
        }

        @Test(timeout = 1000L)
        public void start_stop() throws Exception {
            final ControlPoint cp = new ControlPoint();
            cp.initialize();
            cp.start();
            cp.stop();
            cp.terminate();
        }
    }

    @RunWith(JUnit4.class)
    public static class ネットワーク未使用 {
        private ControlPoint mCp;

        @Before
        public void setUp() throws Exception {
            mCp = spy(new ControlPoint(NetworkUtils.getAvailableInet4Interfaces(),
                    new ControlPointFactory() {
                        @Override
                        SsdpSearchServerList createSsdpSearchServerList(
                                @Nonnull Collection<NetworkInterface> interfaces, @Nonnull ResponseListener listener) {
                            return mock(SsdpSearchServerList.class);
                        }

                        @Override
                        SsdpNotifyReceiverList createSsdpNotifyReceiverList(
                                @Nonnull Collection<NetworkInterface> interfaces, @Nonnull NotifyListener listener) {
                            return mock(SsdpNotifyReceiverList.class);
                        }

                        @Override
                        EventReceiver createEventReceiver(@Nonnull EventMessageListener listener) {
                            return mock(EventReceiver.class);
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
            Thread.sleep(1);

            assertThat(mCp.getDevice(uuid), is(device));
            assertThat(mCp.getDeviceList(), hasItem(device));
            assertThat(mCp.getDeviceListSize(), is(1));
            verify(l).onDiscover(device);
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
            Thread.sleep(1);
            mCp.lostDevice(device);
            Thread.sleep(1);

            assertThat(mCp.getDevice(uuid), is(nullValue()));
            assertThat(mCp.getDeviceListSize(), is(0));
            verify(l).onLost(device);
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
            Thread.sleep(1);

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
            Thread.sleep(1);

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

    public static class EventReceiverに起因するテスト {
        private static final int PORT = 1234;
        private ControlPoint mCp;
        private EventReceiver mEventReceiver;
        private EventMessageListener mEventMessageListener;

        @Before
        public void setUp() throws Exception {
            mEventReceiver = mock(EventReceiver.class);
            doReturn(PORT).when(mEventReceiver).getLocalPort();

            mCp = spy(new ControlPoint(NetworkUtils.getAvailableInet4Interfaces(),
                    new ControlPointFactory() {
                        @Override
                        SsdpSearchServerList createSsdpSearchServerList(
                                @Nonnull Collection<NetworkInterface> interfaces, @Nonnull ResponseListener listener) {
                            return mock(SsdpSearchServerList.class);
                        }

                        @Override
                        SsdpNotifyReceiverList createSsdpNotifyReceiverList(
                                @Nonnull Collection<NetworkInterface> interfaces, @Nonnull NotifyListener listener) {
                            return mock(SsdpNotifyReceiverList.class);
                        }

                        @Override
                        EventReceiver createEventReceiver(@Nonnull EventMessageListener listener) {
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

            Thread.sleep(1);

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
            Thread.sleep(1);

            verify(l, never()).onNotifyEvent(service, 0, variableName, value);
        }
    }
}