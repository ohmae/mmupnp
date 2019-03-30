/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl;

import net.mm2d.upnp.Device;
import net.mm2d.upnp.Http;
import net.mm2d.upnp.HttpClient;
import net.mm2d.upnp.HttpRequest;
import net.mm2d.upnp.HttpResponse;
import net.mm2d.upnp.Property;
import net.mm2d.upnp.Service;
import net.mm2d.upnp.SsdpMessage;
import net.mm2d.upnp.StateVariable;
import net.mm2d.upnp.internal.manager.SubscribeManager;
import net.mm2d.upnp.internal.message.SsdpRequest;
import net.mm2d.upnp.internal.parser.DeviceParser;
import net.mm2d.upnp.util.TestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(Enclosed.class)
public class ServiceTest {

    @RunWith(JUnit4.class)
    public static class Builderによる生成からのテスト {
        @Test
        public void build_成功() {
            final Service service = new ServiceImpl.Builder()
                    .setDevice(mock(DeviceImpl.class))
                    .setSubscribeManager(mock(SubscribeManager.class))
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();

            assertThat(service, is(notNullValue()));
        }

        @Test(expected = IllegalStateException.class)
        public void build_Device不足() {
            new ServiceImpl.Builder()
                    .setSubscribeManager(mock(SubscribeManager.class))
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
        }

        @Test(expected = IllegalStateException.class)
        public void build_SubscribeManager不足() {
            new ServiceImpl.Builder()
                    .setDevice(mock(DeviceImpl.class))
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
        }

        @Test(expected = IllegalStateException.class)
        public void build_ServiceType不足() {
            new ServiceImpl.Builder()
                    .setDevice(mock(DeviceImpl.class))
                    .setSubscribeManager(mock(SubscribeManager.class))
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
        }

        @Test(expected = IllegalStateException.class)
        public void build_ServiceId不足() {
            new ServiceImpl.Builder()
                    .setDevice(mock(DeviceImpl.class))
                    .setSubscribeManager(mock(SubscribeManager.class))
                    .setServiceType("serviceType")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
        }

        @Test(expected = IllegalStateException.class)
        public void build_ScpdUrl不足() {
            new ServiceImpl.Builder()
                    .setDevice(mock(DeviceImpl.class))
                    .setSubscribeManager(mock(SubscribeManager.class))
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
        }

        @Test(expected = IllegalStateException.class)
        public void build_ControlUrl不足() {
            new ServiceImpl.Builder()
                    .setDevice(mock(DeviceImpl.class))
                    .setSubscribeManager(mock(SubscribeManager.class))
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
        }

        @Test(expected = IllegalStateException.class)
        public void build_EventSubUrl不足() {
            new ServiceImpl.Builder()
                    .setDevice(mock(DeviceImpl.class))
                    .setSubscribeManager(mock(SubscribeManager.class))
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setDescription("description")
                    .build();
        }

        @Test(expected = IllegalStateException.class)
        public void build_argumentのRelatedStateVariableNameが指定されていない() {
            final ActionImpl.Builder actionBuilder = new ActionImpl.Builder()
                    .setName("action")
                    .addArgumentBuilder(new ArgumentImpl.Builder()
                            .setName("argumentName")
                            .setDirection("in"));
            final Service service = new ServiceImpl.Builder()
                    .setDevice(mock(DeviceImpl.class))
                    .setSubscribeManager(mock(SubscribeManager.class))
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .addActionBuilder(actionBuilder)
                    .build();

            assertThat(service, is(notNullValue()));
        }

        @Test(expected = IllegalStateException.class)
        public void build_argumentのRelatedStateVariableNameがに対応するStateVariableがない() {
            final ActionImpl.Builder actionBuilder = new ActionImpl.Builder()
                    .setName("action")
                    .addArgumentBuilder(new ArgumentImpl.Builder()
                            .setName("argumentName")
                            .setDirection("in")
                            .setRelatedStateVariableName("StateVariableName"));
            final Service service = new ServiceImpl.Builder()
                    .setDevice(mock(DeviceImpl.class))
                    .setSubscribeManager(mock(SubscribeManager.class))
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .addActionBuilder(actionBuilder)
                    .build();

            assertThat(service, is(notNullValue()));
        }

        @Test
        public void getCallback() throws Exception {
            final ControlPointImpl cp = mock(ControlPointImpl.class);
            final SubscribeManager manager = mock(SubscribeManager.class);
            final SsdpMessage message = mock(SsdpMessage.class);
            doReturn("location").when(message).getLocation();
            doReturn("uuid").when(message).getUuid();
            final DeviceImpl device = new DeviceImpl.Builder(cp, manager, message)
                    .setDescription("description")
                    .setUdn("uuid")
                    .setUpc("upc")
                    .setDeviceType("deviceType")
                    .setFriendlyName("friendlyName")
                    .setManufacture("manufacture")
                    .setModelName("modelName")
                    .build();
            final Service service = new ServiceImpl.Builder()
                    .setDevice(device)
                    .setSubscribeManager(manager)
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
            doReturn(InetAddress.getByName("192.168.0.1"))
                    .when(message).getLocalAddress();
            doReturn(80).when(manager).getEventPort();

            assertThat(((ServiceImpl) service).getCallback(), is("<http://192.168.0.1/>"));

            doReturn(8080).when(manager).getEventPort();

            assertThat(((ServiceImpl) service).getCallback(), is("<http://192.168.0.1:8080/>"));
        }

        @Test
        public void hashCode_Exceptionが発生しない() {
            final Service service = new ServiceImpl.Builder()
                    .setDevice(mock(DeviceImpl.class))
                    .setSubscribeManager(mock(SubscribeManager.class))
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
            service.hashCode();
        }

        @Test
        public void equals_比較可能() {
            final Service service = new ServiceImpl.Builder()
                    .setDevice(mock(DeviceImpl.class))
                    .setSubscribeManager(mock(SubscribeManager.class))
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
            assertThat(service.equals(null), is(false));
            assertThat(service.equals(""), is(false));
            assertThat(service.equals(service), is(true));
        }

        @Test
        public void equals_同一の情報() {
            final SubscribeManager manager = mock(SubscribeManager.class);
            final SsdpMessage message = mock(SsdpMessage.class);
            doReturn("location").when(message).getLocation();
            doReturn("uuid").when(message).getUuid();
            final DeviceImpl device = new DeviceImpl.Builder(mock(ControlPointImpl.class), manager, message)
                    .setDescription("description")
                    .setUdn("uuid")
                    .setUpc("upc")
                    .setDeviceType("deviceType")
                    .setFriendlyName("friendlyName")
                    .setManufacture("manufacture")
                    .setModelName("modelName")
                    .build();
            final Service service1 = new ServiceImpl.Builder()
                    .setDevice(device)
                    .setSubscribeManager(manager)
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
            final Service service2 = new ServiceImpl.Builder()
                    .setDevice(device)
                    .setSubscribeManager(manager)
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
            assertThat(service1.equals(service2), is(true));
        }

        @Test
        public void equals_不一致を無視() {
            final SubscribeManager manager = mock(SubscribeManager.class);
            final SsdpMessage message = mock(SsdpMessage.class);
            doReturn("location").when(message).getLocation();
            doReturn("uuid").when(message).getUuid();
            final DeviceImpl device = new DeviceImpl.Builder(mock(ControlPointImpl.class), manager, message)
                    .setDescription("description")
                    .setUdn("uuid")
                    .setUpc("upc")
                    .setDeviceType("deviceType")
                    .setFriendlyName("friendlyName")
                    .setManufacture("manufacture")
                    .setModelName("modelName")
                    .build();
            final Service service1 = new ServiceImpl.Builder()
                    .setDevice(device)
                    .setSubscribeManager(manager)
                    .setServiceType("serviceType1")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl1")
                    .setControlUrl("controlUrl1")
                    .setEventSubUrl("eventSubUrl1")
                    .setDescription("description1")
                    .build();
            final Service service2 = new ServiceImpl.Builder()
                    .setDevice(device)
                    .setSubscribeManager(manager)
                    .setServiceType("serviceType2")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl2")
                    .setControlUrl("controlUrl2")
                    .setEventSubUrl("eventSubUrl2")
                    .setDescription("description2")
                    .build();
            assertThat(service1.equals(service2), is(true));
        }

        @Test
        public void equals_ServiceId不一致() {
            final SubscribeManager manager = mock(SubscribeManager.class);
            final SsdpMessage message = mock(SsdpMessage.class);
            doReturn("location").when(message).getLocation();
            doReturn("uuid").when(message).getUuid();
            final DeviceImpl device = new DeviceImpl.Builder(mock(ControlPointImpl.class), manager, message)
                    .setDescription("description")
                    .setUdn("uuid")
                    .setUpc("upc")
                    .setDeviceType("deviceType")
                    .setFriendlyName("friendlyName")
                    .setManufacture("manufacture")
                    .setModelName("modelName")
                    .build();
            final Service service1 = new ServiceImpl.Builder()
                    .setDevice(device)
                    .setSubscribeManager(manager)
                    .setServiceType("serviceType")
                    .setServiceId("serviceId1")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
            final Service service2 = new ServiceImpl.Builder()
                    .setDevice(device)
                    .setSubscribeManager(manager)
                    .setServiceType("serviceType")
                    .setServiceId("serviceId2")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
            assertThat(service1.equals(service2), is(false));
        }

        @Test
        public void equals_device不一致() {
            final SubscribeManager manager = mock(SubscribeManager.class);
            final SsdpMessage message1 = mock(SsdpMessage.class);
            doReturn("location").when(message1).getLocation();
            doReturn("uuid1").when(message1).getUuid();
            final DeviceImpl device1 = new DeviceImpl.Builder(mock(ControlPointImpl.class), manager, message1)
                    .setDescription("description")
                    .setUdn("uuid1")
                    .setUpc("upc")
                    .setDeviceType("deviceType")
                    .setFriendlyName("friendlyName")
                    .setManufacture("manufacture")
                    .setModelName("modelName")
                    .build();
            final SsdpMessage message2 = mock(SsdpMessage.class);
            doReturn("location").when(message2).getLocation();
            doReturn("uuid2").when(message2).getUuid();
            final DeviceImpl device2 = new DeviceImpl.Builder(mock(ControlPointImpl.class), manager, message2)
                    .setDescription("description")
                    .setUdn("uuid2")
                    .setUpc("upc")
                    .setDeviceType("deviceType")
                    .setFriendlyName("friendlyName")
                    .setManufacture("manufacture")
                    .setModelName("modelName")
                    .build();
            final Service service1 = new ServiceImpl.Builder()
                    .setDevice(device1)
                    .setSubscribeManager(manager)
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
            final Service service2 = new ServiceImpl.Builder()
                    .setDevice(device2)
                    .setSubscribeManager(manager)
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
            assertThat(service1.equals(service2), is(false));
        }

        @Test
        public void createHttpClient() {
            final Service service = new ServiceImpl.Builder()
                    .setDevice(mock(DeviceImpl.class))
                    .setSubscribeManager(mock(SubscribeManager.class))
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();

            final HttpClient client = ((ServiceImpl) service).createHttpClient();
            assertThat(client.isKeepAlive(), is(false));
        }
    }

    @RunWith(JUnit4.class)
    public static class DeviceParserによる生成からのテスト {
        private static final String SID = "11234567-89ab-cdef-0123-456789abcdef";
        private static final String INTERFACE_ADDRESS = "192.0.2.3";
        private static final int EVENT_PORT = 100;
        private ControlPointImpl mControlPoint;
        private SubscribeManager mSubscribeManager;
        private Device mDevice;
        private ServiceImpl mCms;
        private ServiceImpl mCds;
        private ServiceImpl mMmupnp;

        @Before
        public void setUp() throws Exception {
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
            final SsdpMessage ssdpMessage = SsdpRequest.create(InetAddress.getByName(INTERFACE_ADDRESS), data, data.length);
            mControlPoint = mock(ControlPointImpl.class);
            mSubscribeManager = mock(SubscribeManager.class);
            doReturn(EVENT_PORT).when(mSubscribeManager).getEventPort();
            final DeviceImpl.Builder builder = new DeviceImpl.Builder(mControlPoint, mSubscribeManager, ssdpMessage);
            DeviceParser.loadDescription(httpClient, builder);
            mDevice = builder.build();
            mCms = (ServiceImpl) mDevice.findServiceById("urn:upnp-org:serviceId:ConnectionManager");
            mCds = (ServiceImpl) spy(mDevice.findServiceById("urn:upnp-org:serviceId:ContentDirectory"));
            mMmupnp = (ServiceImpl) mDevice.findServiceById("urn:upnp-org:serviceId:X_mmupnp");
        }

        @Test
        public void getDevice() {
            assertThat(mCms.getDevice(), is(mDevice));
        }

        @Test
        public void getServiceType() {
            assertThat(mCms.getServiceType(), is("urn:schemas-upnp-org:service:ConnectionManager:1"));
            assertThat(mCds.getServiceType(), is("urn:schemas-upnp-org:service:ContentDirectory:1"));
            assertThat(mMmupnp.getServiceType(), is("urn:schemas-mm2d-net:service:X_mmupnp:1"));
        }

        @Test
        public void getServiceId() {
            assertThat(mCms.getServiceId(), is("urn:upnp-org:serviceId:ConnectionManager"));
            assertThat(mCds.getServiceId(), is("urn:upnp-org:serviceId:ContentDirectory"));
            assertThat(mMmupnp.getServiceId(), is("urn:upnp-org:serviceId:X_mmupnp"));
        }

        @Test
        public void getScpdUrl() {
            assertThat(mCms.getScpdUrl(), is("/cms.xml"));
            assertThat(mCds.getScpdUrl(), is("/cds.xml"));
            assertThat(mMmupnp.getScpdUrl(), is("/mmupnp.xml"));
        }

        @Test
        public void getControlUrl() {
            assertThat(mCms.getControlUrl(), is("/cms/control"));
            assertThat(mCds.getControlUrl(), is("/cds/control"));
            assertThat(mMmupnp.getControlUrl(), is("/mmupnp/control"));
        }

        @Test
        public void getEventSubUrl() {
            assertThat(mCms.getEventSubUrl(), is("/cms/event"));
            assertThat(mCds.getEventSubUrl(), is("/cds/event"));
            assertThat(mMmupnp.getEventSubUrl(), is("/mmupnp/event"));
        }

        @Test
        public void getDescription() throws Exception {
            assertThat(mCms.getDescription(), is(TestUtils.getResourceAsString("cms.xml")));
            assertThat(mCds.getDescription(), is(TestUtils.getResourceAsString("cds.xml")));
            assertThat(mMmupnp.getDescription(), is(TestUtils.getResourceAsString("mmupnp.xml")));
        }

        @Test
        public void getActionList() {
            assertThat(mCms.getActionList(), hasSize(3));
            assertThat(mCds.getActionList(), hasSize(4));
            assertThat(mMmupnp.getActionList(), hasSize(1));
            assertThat(mCms.getActionList(), is(mCms.getActionList()));
        }

        @Test
        public void findAction() {
            assertThat(mCms.findAction("Browse"), is(nullValue()));
            assertThat(mCds.findAction("Browse"), is(notNullValue()));
        }

        @Test
        public void getStateVariableList() {
            assertThat(mCds.getStateVariableList(), hasSize(11));
            assertThat(mCds.getStateVariableList(), is(mCds.getStateVariableList()));
        }

        @Test
        public void getStateVariableParam() {
            final StateVariable type = mMmupnp.findStateVariable("A_ARG_TYPE_Type");
            assertThat(type.getName(), is("A_ARG_TYPE_Type"));
            assertThat(type.isSendEvents(), is(false));
            assertThat(type.getDataType(), is("string"));

            final StateVariable value = mMmupnp.findStateVariable("A_ARG_TYPE_Value");
            assertThat(value.getName(), is("A_ARG_TYPE_Value"));
            assertThat(value.isSendEvents(), is(true));
            assertThat(value.getDataType(), is("i4"));
            assertThat(value.getDefaultValue(), is("10"));
            assertThat(value.getStep(), is("1"));
            assertThat(value.getMinimum(), is("0"));
            assertThat(value.getMaximum(), is("100"));
        }

        @Test
        public void findStateVariable() {
            final String name = "A_ARG_TYPE_BrowseFlag";
            final StateVariable variable = mCds.findStateVariable(name);
            assertThat(variable.getName(), is(name));
        }

        private HttpResponse createSubscribeResponse() {
            final HttpResponse response = HttpResponse.create();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SERVER, Property.SERVER_VALUE);
            response.setHeader(Http.DATE, Http.formatDate(System.currentTimeMillis()));
            response.setHeader(Http.CONNECTION, Http.CLOSE);
            response.setHeader(Http.SID, SID);
            response.setHeader(Http.TIMEOUT, "Second-300");
            response.setHeader(Http.CONTENT_LENGTH, "0");
            return response;
        }

        @Test
        public void subscribeSync() throws Exception {
            final HttpClient client = spy(new HttpClient());
            final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            doReturn(createSubscribeResponse()).when(client).post(captor.capture());
            doReturn(client).when(mCds).createHttpClient();
            mCds.subscribeSync();

            final HttpRequest request = captor.getValue();
            assertThat(request.getUri(), is(mCds.getEventSubUrl()));
            verify(mSubscribeManager).register(mCds, TimeUnit.SECONDS.toMillis(300), false);

            final String callback = request.getHeader(Http.CALLBACK);
            assertThat(callback.charAt(0), is('<'));
            assertThat(callback.charAt(callback.length() - 1), is('>'));
            final URL url = new URL(callback.substring(1, callback.length() - 2));
            assertThat(url.getHost(), is(INTERFACE_ADDRESS));
            assertThat(url.getPort(), is(EVENT_PORT));
            System.out.println();
        }

        @Test
        public void subscribeSync_keep() throws Exception {
            final HttpClient client = spy(new HttpClient());
            final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            doReturn(createSubscribeResponse()).when(client).post(captor.capture());
            doReturn(client).when(mCds).createHttpClient();
            mCds.subscribeSync(true);

            final HttpRequest request = captor.getValue();
            assertThat(request.getUri(), is(mCds.getEventSubUrl()));
            verify(mSubscribeManager).register(mCds, TimeUnit.SECONDS.toMillis(300), true);
        }

        @Test
        public void renewSubscribeSync1() throws Exception {
            final HttpClient client = spy(new HttpClient());
            final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            doReturn(createSubscribeResponse()).when(client).post(captor.capture());
            doReturn(client).when(mCds).createHttpClient();
            mCds.subscribeSync();
            mCds.renewSubscribeSync();

            final HttpRequest request = captor.getValue();
            assertThat(request.getUri(), is(mCds.getEventSubUrl()));
        }

        @Test
        public void renewSubscribeSync2() throws Exception {
            final HttpClient client = spy(new HttpClient());
            final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            doReturn(createSubscribeResponse()).when(client).post(captor.capture());
            doReturn(client).when(mCds).createHttpClient();
            mCds.subscribeSync();
            mCds.subscribeSync();

            final HttpRequest request = captor.getValue();
            assertThat(request.getUri(), is(mCds.getEventSubUrl()));
        }

        @Test
        public void unsubscribeSync() throws Exception {
            final HttpClient client = spy(new HttpClient());
            final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            doReturn(createSubscribeResponse()).when(client).post(captor.capture());
            doReturn(client).when(mCds).createHttpClient();
            mCds.subscribeSync();
            mCds.unsubscribeSync();

            final HttpRequest request = captor.getValue();
            assertThat(request.getUri(), is(mCds.getEventSubUrl()));
            verify(mSubscribeManager).unregister(mCds);
        }

        @Test
        public void getSubscriptionId() throws Exception {
            final HttpClient client = mock(HttpClient.class);
            doReturn(createSubscribeResponse()).when(client).post(ArgumentMatchers.any(HttpRequest.class));
            doReturn(client).when(mCds).createHttpClient();
            mCds.subscribeSync();

            assertThat(mCds.getSubscriptionId(), is(SID));
        }
    }

    @RunWith(JUnit4.class)
    public static class subscribe_パーサー機能のテスト {
        private static final long DEFAULT_SUBSCRIPTION_TIMEOUT = TimeUnit.SECONDS.toMillis(300);

        @Test
        public void parseTimeout_情報がない場合デフォルト() {
            final HttpResponse response = HttpResponse.create();
            response.setStartLine("HTTP/1.1 200 OK");
            assertThat(ServiceImpl.parseTimeout(response), is(DEFAULT_SUBSCRIPTION_TIMEOUT));
        }

        @Test
        public void parseTimeout_infiniteの場合デフォルト() {
            final HttpResponse response = HttpResponse.create();
            response.setStartLine("HTTP/1.1 200 OK");
            response.setHeader(Http.TIMEOUT, "infinite");
            assertThat(ServiceImpl.parseTimeout(response), is(DEFAULT_SUBSCRIPTION_TIMEOUT));
        }

        @Test
        public void parseTimeout_secondの指定通り() {
            final HttpResponse response = HttpResponse.create();
            response.setStartLine("HTTP/1.1 200 OK");
            response.setHeader(Http.TIMEOUT, "second-100");
            assertThat(ServiceImpl.parseTimeout(response), is(TimeUnit.SECONDS.toMillis(100)));
        }

        @Test
        public void parseTimeout_フォーマットエラーはデフォルト() {
            final HttpResponse response = HttpResponse.create();
            response.setStartLine("HTTP/1.1 200 OK");
            response.setHeader(Http.TIMEOUT, "seconds-100");
            assertThat(ServiceImpl.parseTimeout(response), is(DEFAULT_SUBSCRIPTION_TIMEOUT));

            response.setHeader(Http.TIMEOUT, "second-ff");
            assertThat(ServiceImpl.parseTimeout(response), is(DEFAULT_SUBSCRIPTION_TIMEOUT));
        }
    }

    @RunWith(JUnit4.class)
    public static class subscribe_機能のテスト {
        private ControlPointImpl mControlPoint;
        private DeviceImpl mDevice;
        private SubscribeManager mSubscribeManager;
        private ServiceImpl mService;
        private HttpClient mHttpClient;

        @Before
        public void setUp() throws Exception {
            mControlPoint = mock(ControlPointImpl.class);
            mDevice = mock(DeviceImpl.class);
            mSubscribeManager = mock(SubscribeManager.class);
            doReturn(mControlPoint).when(mDevice).getControlPoint();
            mService = (ServiceImpl) spy(new ServiceImpl.Builder()
                    .setDevice(mDevice)
                    .setSubscribeManager(mSubscribeManager)
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build());
            doReturn(new URL("http://192.0.2.2/")).when(mService).makeAbsoluteUrl(anyString());
            doReturn("").when(mService).getCallback();
            mHttpClient = mock(HttpClient.class);
            doReturn(mHttpClient).when(mService).createHttpClient();
        }

        @Test
        public void subscribeSync_成功() throws Exception {
            final HttpResponse response = HttpResponse.create();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.subscribeSync(), is(true));
        }

        @Test
        public void subscribeSync_SIDなし() throws Exception {
            final HttpResponse response = HttpResponse.create();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.subscribeSync(), is(false));
        }

        @Test
        public void subscribeSync_Timeout値異常() throws Exception {
            final HttpResponse response = HttpResponse.create();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-0");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.subscribeSync(), is(false));
        }

        @Test
        public void subscribeSync_Http応答異常() throws Exception {
            final HttpResponse response = HttpResponse.create();
            response.setStatus(Http.Status.HTTP_INTERNAL_ERROR);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.subscribeSync(), is(false));
        }

        @Test
        public void subscribeSync_2回目はrenewがコールされfalseが返されると失敗() throws Exception {
            final HttpResponse response = HttpResponse.create();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.subscribeSync(), is(true));

            doReturn(false).when(mService).renewSubscribeInner();

            assertThat(mService.subscribeSync(), is(false));
            verify(mService, times(1)).renewSubscribeInner();
        }

        @Test
        public void renewSubscribeSync_subscribe前はsubscribeが実行される() throws Exception {
            final HttpResponse response = HttpResponse.create();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.renewSubscribeSync(), is(true));
            verify(mService, times(1)).subscribeInner(anyBoolean());
        }

        @Test
        public void renewSubscribeSync_2回目はrenewが実行される() throws Exception {
            final HttpResponse response = HttpResponse.create();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.renewSubscribeSync(), is(true));
            verify(mService, times(1)).subscribeInner(anyBoolean());

            assertThat(mService.renewSubscribeSync(), is(true));
            verify(mService, times(1)).renewSubscribeInner();
        }

        @Test
        public void renewSubscribeSync_renewの応答ステータス異常で失敗() throws Exception {
            final HttpResponse response = HttpResponse.create();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.renewSubscribeSync(), is(true));

            response.setStatus(Http.Status.HTTP_INTERNAL_ERROR);
            assertThat(mService.renewSubscribeSync(), is(false));
        }

        @Test
        public void renewSubscribeSync_sid不一致() throws Exception {
            final HttpResponse response = HttpResponse.create();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.renewSubscribeSync(), is(true));

            response.setHeader(Http.SID, "sid2");
            assertThat(mService.renewSubscribeSync(), is(false));
        }

        @Test
        public void renewSubscribeSync_Timeout値異常() throws Exception {
            final HttpResponse response = HttpResponse.create();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.renewSubscribeSync(), is(true));

            response.setHeader(Http.TIMEOUT, "second-0");
            assertThat(mService.renewSubscribeSync(), is(false));
        }

        @Test
        public void unsubscribeSync_subscribeする前に実行すると失敗() {
            assertThat(mService.unsubscribeSync(), is(false));
        }

        @Test
        public void unsubscribeSync_正常() throws Exception {
            final HttpResponse response = HttpResponse.create();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
            assertThat(mService.subscribeSync(), is(true));

            assertThat(mService.unsubscribeSync(), is(true));
        }

        @Test
        public void unsubscribeSync_OK以外は失敗() throws Exception {
            final HttpResponse response = HttpResponse.create();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
            assertThat(mService.subscribeSync(), is(true));

            response.setStatus(Http.Status.HTTP_INTERNAL_ERROR);

            assertThat(mService.unsubscribeSync(), is(false));
        }
    }
}