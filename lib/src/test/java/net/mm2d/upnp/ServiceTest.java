/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.TestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class ServiceTest {

    @RunWith(JUnit4.class)
    public static class Builderによる生成からのテスト {
        @Test
        public void build_成功() throws Exception {
            final Service service = new ServiceImpl.Builder()
                    .setDevice(mock(Device.class))
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
        public void build_Device不足() throws Exception {
            new ServiceImpl.Builder()
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
        }

        @Test(expected = IllegalStateException.class)
        public void build_ServiceType不足() throws Exception {
            new ServiceImpl.Builder()
                    .setDevice(mock(Device.class))
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
        }

        @Test(expected = IllegalStateException.class)
        public void build_ServiceId不足() throws Exception {
            new ServiceImpl.Builder()
                    .setDevice(mock(Device.class))
                    .setServiceType("serviceType")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
        }

        @Test(expected = IllegalStateException.class)
        public void build_ScpdUrl不足() throws Exception {
            new ServiceImpl.Builder()
                    .setDevice(mock(Device.class))
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
        }

        @Test(expected = IllegalStateException.class)
        public void build_ControlUrl不足() throws Exception {
            new ServiceImpl.Builder()
                    .setDevice(mock(Device.class))
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
        }

        @Test(expected = IllegalStateException.class)
        public void build_EventSubUrl不足() throws Exception {
            new ServiceImpl.Builder()
                    .setDevice(mock(Device.class))
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setDescription("description")
                    .build();
        }

        @Test(expected = IllegalStateException.class)
        public void build_argumentのRelatedStateVariableNameが指定されていない() throws Exception {
            final ActionImpl.Builder actionBuilder = new ActionImpl.Builder()
                    .setName("action")
                    .addArgumentBuilder(new ArgumentImpl.Builder()
                            .setName("argumentName")
                            .setDirection("in"));
            final Service service = new ServiceImpl.Builder()
                    .setDevice(mock(Device.class))
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
        public void build_argumentのRelatedStateVariableNameがに対応するStateVariableがない() throws Exception {
            final ActionImpl.Builder actionBuilder = new ActionImpl.Builder()
                    .setName("action")
                    .addArgumentBuilder(new ArgumentImpl.Builder()
                            .setName("argumentName")
                            .setDirection("in")
                            .setRelatedStateVariableName("StateVariableName"));
            final Service service = new ServiceImpl.Builder()
                    .setDevice(mock(Device.class))
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
            final ControlPoint cp = mock(ControlPoint.class);
            final SsdpMessage message = mock(SsdpMessage.class);
            doReturn("location").when(message).getLocation();
            doReturn("uuid").when(message).getUuid();
            final Device device = new DeviceImpl.Builder(cp, message)
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
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
            doReturn(TestUtils.createInterfaceAddress("192.168.0.1", "255.255.255.0", 24))
                    .when(message).getInterfaceAddress();
            doReturn(80).when(cp).getEventPort();

            assertThat(((ServiceImpl) service).getCallback(), is("<http://192.168.0.1/>"));

            doReturn(8080).when(cp).getEventPort();

            assertThat(((ServiceImpl) service).getCallback(), is("<http://192.168.0.1:8080/>"));
        }

        @Test
        public void hashCode_Exceptionが発生しない() throws Exception {
            final Service service = new ServiceImpl.Builder()
                    .setDevice(mock(Device.class))
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
        public void equals_比較可能() throws Exception {
            final Service service = new ServiceImpl.Builder()
                    .setDevice(mock(Device.class))
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
        public void equals_同一の情報() throws Exception {
            final SsdpMessage message = mock(SsdpMessage.class);
            doReturn("location").when(message).getLocation();
            doReturn("uuid").when(message).getUuid();
            final Device device = new DeviceImpl.Builder(mock(ControlPoint.class), message)
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
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
            final Service service2 = new ServiceImpl.Builder()
                    .setDevice(device)
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
        public void equals_不一致を無視() throws Exception {
            final SsdpMessage message = mock(SsdpMessage.class);
            doReturn("location").when(message).getLocation();
            doReturn("uuid").when(message).getUuid();
            final Device device = new DeviceImpl.Builder(mock(ControlPoint.class), message)
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
                    .setServiceType("serviceType1")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl1")
                    .setControlUrl("controlUrl1")
                    .setEventSubUrl("eventSubUrl1")
                    .setDescription("description1")
                    .build();
            final Service service2 = new ServiceImpl.Builder()
                    .setDevice(device)
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
        public void equals_ServiceId不一致() throws Exception {
            final SsdpMessage message = mock(SsdpMessage.class);
            doReturn("location").when(message).getLocation();
            doReturn("uuid").when(message).getUuid();
            final Device device = new DeviceImpl.Builder(mock(ControlPoint.class), message)
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
                    .setServiceType("serviceType")
                    .setServiceId("serviceId1")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
            final Service service2 = new ServiceImpl.Builder()
                    .setDevice(device)
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
        public void equals_device不一致() throws Exception {
            final SsdpMessage message1 = mock(SsdpMessage.class);
            doReturn("location").when(message1).getLocation();
            doReturn("uuid1").when(message1).getUuid();
            final Device device1 = new DeviceImpl.Builder(mock(ControlPoint.class), message1)
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
            final Device device2 = new DeviceImpl.Builder(mock(ControlPoint.class), message2)
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
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build();
            final Service service2 = new ServiceImpl.Builder()
                    .setDevice(device2)
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
        public void createHttpClient() throws Exception {
            final Service service = new ServiceImpl.Builder()
                    .setDevice(mock(Device.class))
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
        private ControlPoint mControlPoint;
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
            final InterfaceAddress interfaceAddress = mock(InterfaceAddress.class);
            doReturn(InetAddress.getByName(INTERFACE_ADDRESS)).when(interfaceAddress).getAddress();
            final SsdpMessage ssdpMessage = new SsdpRequest(interfaceAddress, data, data.length);
            mControlPoint = mock(ControlPoint.class);
            doReturn(EVENT_PORT).when(mControlPoint).getEventPort();
            final DeviceImpl.Builder builder = new DeviceImpl.Builder(mControlPoint, ssdpMessage);
            DeviceParser.loadDescription(httpClient, builder);
            mDevice = builder.build();
            mCms = (ServiceImpl) mDevice.findServiceById("urn:upnp-org:serviceId:ConnectionManager");
            mCds = (ServiceImpl) spy(mDevice.findServiceById("urn:upnp-org:serviceId:ContentDirectory"));
            mMmupnp = (ServiceImpl) mDevice.findServiceById("urn:upnp-org:serviceId:X_mmupnp");
        }

        @Test
        public void getDevice() throws Exception {
            assertThat(mCms.getDevice(), is(mDevice));
        }

        @Test
        public void getAbsoluteUrl_deviceのメソッドと等価() throws Exception {
            final String url = "test";
            assertThat(mCms.getAbsoluteUrl(url), is(mDevice.getAbsoluteUrl(url)));
        }

        @Test
        public void getServiceType() throws Exception {
            assertThat(mCms.getServiceType(), is("urn:schemas-upnp-org:service:ConnectionManager:1"));
            assertThat(mCds.getServiceType(), is("urn:schemas-upnp-org:service:ContentDirectory:1"));
            assertThat(mMmupnp.getServiceType(), is("urn:schemas-mm2d-net:service:X_mmupnp:1"));
        }

        @Test
        public void getServiceId() throws Exception {
            assertThat(mCms.getServiceId(), is("urn:upnp-org:serviceId:ConnectionManager"));
            assertThat(mCds.getServiceId(), is("urn:upnp-org:serviceId:ContentDirectory"));
            assertThat(mMmupnp.getServiceId(), is("urn:upnp-org:serviceId:X_mmupnp"));
        }

        @Test
        public void getScpdUrl() throws Exception {
            assertThat(mCms.getScpdUrl(), is("/cms.xml"));
            assertThat(mCds.getScpdUrl(), is("/cds.xml"));
            assertThat(mMmupnp.getScpdUrl(), is("/mmupnp.xml"));
        }

        @Test
        public void getControlUrl() throws Exception {
            assertThat(mCms.getControlUrl(), is("/cms/control"));
            assertThat(mCds.getControlUrl(), is("/cds/control"));
            assertThat(mMmupnp.getControlUrl(), is("/mmupnp/control"));
        }

        @Test
        public void getEventSubUrl() throws Exception {
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
        public void getActionList() throws Exception {
            assertThat(mCms.getActionList(), hasSize(3));
            assertThat(mCds.getActionList(), hasSize(4));
            assertThat(mMmupnp.getActionList(), hasSize(1));
            assertThat(mCms.getActionList(), is(mCms.getActionList()));
        }

        @Test
        public void findAction() throws Exception {
            assertThat(mCms.findAction("Browse"), is(nullValue()));
            assertThat(mCds.findAction("Browse"), is(notNullValue()));
        }

        @Test
        public void getStateVariableList() throws Exception {
            assertThat(mCds.getStateVariableList(), hasSize(11));
            assertThat(mCds.getStateVariableList(), is(mCds.getStateVariableList()));
        }

        @Test
        public void getStateVariableParam() throws Exception {
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
        public void findStateVariable() throws Exception {
            final String name = "A_ARG_TYPE_BrowseFlag";
            final StateVariable variable = mCds.findStateVariable(name);
            assertThat(variable.getName(), is(name));
        }

        private HttpResponse createSubscribeResponse() {
            return new HttpResponse()
                    .setStatus(Http.Status.HTTP_OK)
                    .setHeader(Http.SERVER, Property.SERVER_VALUE)
                    .setHeader(Http.DATE, Http.formatDate(System.currentTimeMillis()))
                    .setHeader(Http.CONNECTION, Http.CLOSE)
                    .setHeader(Http.SID, SID)
                    .setHeader(Http.TIMEOUT, "Second-300")
                    .setHeader(Http.CONTENT_LENGTH, "0");
        }

        @Test
        public void subscribe() throws Exception {
            final HttpClient client = spy(new HttpClient());
            final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            doReturn(createSubscribeResponse()).when(client).post(captor.capture());
            doReturn(client).when(mCds).createHttpClient();
            mCds.subscribe();

            final HttpRequest request = captor.getValue();
            assertThat(request.getUri(), is(mCds.getEventSubUrl()));
            verify(mControlPoint).registerSubscribeService(mCds, false);

            final String callback = request.getHeader(Http.CALLBACK);
            assertThat(callback.charAt(0), is('<'));
            assertThat(callback.charAt(callback.length() - 1), is('>'));
            final URL url = new URL(callback.substring(1, callback.length() - 2));
            assertThat(url.getHost(), is(INTERFACE_ADDRESS));
            assertThat(url.getPort(), is(EVENT_PORT));
            System.out.println();
        }

        @Test
        public void subscribe_keep() throws Exception {
            final HttpClient client = spy(new HttpClient());
            final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            doReturn(createSubscribeResponse()).when(client).post(captor.capture());
            doReturn(client).when(mCds).createHttpClient();
            mCds.subscribe(true);

            final HttpRequest request = captor.getValue();
            assertThat(request.getUri(), is(mCds.getEventSubUrl()));
            verify(mControlPoint).registerSubscribeService(mCds, true);
        }

        @Test
        public void renewSubscribe1() throws Exception {
            final HttpClient client = spy(new HttpClient());
            final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            doReturn(createSubscribeResponse()).when(client).post(captor.capture());
            doReturn(client).when(mCds).createHttpClient();
            mCds.subscribe();
            mCds.renewSubscribe();

            final HttpRequest request = captor.getValue();
            assertThat(request.getUri(), is(mCds.getEventSubUrl()));
        }

        @Test
        public void renewSubscribe2() throws Exception {
            final HttpClient client = spy(new HttpClient());
            final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            doReturn(createSubscribeResponse()).when(client).post(captor.capture());
            doReturn(client).when(mCds).createHttpClient();
            mCds.subscribe();
            mCds.subscribe();

            final HttpRequest request = captor.getValue();
            assertThat(request.getUri(), is(mCds.getEventSubUrl()));
        }

        @Test
        public void unsubscribe() throws Exception {
            final HttpClient client = spy(new HttpClient());
            final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            doReturn(createSubscribeResponse()).when(client).post(captor.capture());
            doReturn(client).when(mCds).createHttpClient();
            mCds.subscribe();
            mCds.unsubscribe();

            final HttpRequest request = captor.getValue();
            assertThat(request.getUri(), is(mCds.getEventSubUrl()));
            verify(mControlPoint).unregisterSubscribeService(mCds);
        }

        @Test
        public void expired() throws Exception {
            final HttpClient client = mock(HttpClient.class);
            doReturn(createSubscribeResponse()).when(client).post(ArgumentMatchers.any(HttpRequest.class));
            doReturn(client).when(mCds).createHttpClient();
            mCds.subscribe();

            mCds.expired();
            assertThat(mCds.getSubscriptionId(), is(nullValue()));
            assertThat(mCds.getSubscriptionExpiryTime(), is(0L));
            assertThat(mCds.getSubscriptionStart(), is(0L));
            assertThat(mCds.getSubscriptionTimeout(), is(0L));
        }

        @Test
        public void getSubscriptionId() throws Exception {
            final HttpClient client = mock(HttpClient.class);
            doReturn(createSubscribeResponse()).when(client).post(ArgumentMatchers.any(HttpRequest.class));
            doReturn(client).when(mCds).createHttpClient();
            mCds.subscribe();

            assertThat(mCds.getSubscriptionId(), is(SID));
        }

        @Test
        public void getSubscriptionStart() throws Exception {
            final HttpClient client = mock(HttpClient.class);
            doReturn(createSubscribeResponse()).when(client).post(ArgumentMatchers.any(HttpRequest.class));
            doReturn(client).when(mCds).createHttpClient();
            final long before = System.currentTimeMillis();
            mCds.subscribe();
            final long after = System.currentTimeMillis();

            assertThat(mCds.getSubscriptionStart(), greaterThanOrEqualTo(before));
            assertThat(mCds.getSubscriptionStart(), lessThanOrEqualTo(after));
        }

        @Test
        public void getSubscriptionTimeout() throws Exception {
            final HttpClient client = mock(HttpClient.class);
            doReturn(createSubscribeResponse()).when(client).post(ArgumentMatchers.any(HttpRequest.class));
            doReturn(client).when(mCds).createHttpClient();
            mCds.subscribe();

            assertThat(mCds.getSubscriptionTimeout(), is(TimeUnit.SECONDS.toMillis(300L)));
        }

        @Test
        public void getSubscriptionExpiryTime() throws Exception {
            final HttpClient client = mock(HttpClient.class);
            doReturn(createSubscribeResponse()).when(client).post(ArgumentMatchers.any(HttpRequest.class));
            doReturn(client).when(mCds).createHttpClient();
            final long before = System.currentTimeMillis();
            mCds.subscribe();
            final long after = System.currentTimeMillis();

            assertThat(mCds.getSubscriptionExpiryTime(),
                    greaterThanOrEqualTo(before + TimeUnit.SECONDS.toMillis(300L)));
            assertThat(mCds.getSubscriptionExpiryTime(),
                    lessThanOrEqualTo(after + TimeUnit.SECONDS.toMillis(300L)));
        }
    }

    @RunWith(JUnit4.class)
    public static class subscribe_パーサー機能のテスト {
        private static final long DEFAULT_SUBSCRIPTION_TIMEOUT = TimeUnit.SECONDS.toMillis(300);

        @Test
        public void parseTimeout_情報がない場合デフォルト() {
            final HttpResponse response = new HttpResponse();
            response.setStatusLine("HTTP/1.1 200 OK");
            assertThat(ServiceImpl.parseTimeout(response), is(DEFAULT_SUBSCRIPTION_TIMEOUT));
        }

        @Test
        public void parseTimeout_infiniteの場合デフォルト() {
            final HttpResponse response = new HttpResponse();
            response.setStatusLine("HTTP/1.1 200 OK");
            response.setHeader(Http.TIMEOUT, "infinite");
            assertThat(ServiceImpl.parseTimeout(response), is(DEFAULT_SUBSCRIPTION_TIMEOUT));
        }

        @Test
        public void parseTimeout_secondの指定通り() {
            final HttpResponse response = new HttpResponse();
            response.setStatusLine("HTTP/1.1 200 OK");
            response.setHeader(Http.TIMEOUT, "second-100");
            assertThat(ServiceImpl.parseTimeout(response), is(TimeUnit.SECONDS.toMillis(100)));
        }

        @Test
        public void parseTimeout_フォーマットエラーはデフォルト() {
            final HttpResponse response = new HttpResponse();
            response.setStatusLine("HTTP/1.1 200 OK");
            response.setHeader(Http.TIMEOUT, "seconds-100");
            assertThat(ServiceImpl.parseTimeout(response), is(DEFAULT_SUBSCRIPTION_TIMEOUT));

            response.setHeader(Http.TIMEOUT, "second-ff");
            assertThat(ServiceImpl.parseTimeout(response), is(DEFAULT_SUBSCRIPTION_TIMEOUT));
        }
    }

    @RunWith(JUnit4.class)
    public static class subscribe_機能のテスト {
        private ControlPoint mControlPoint;
        private Device mDevice;
        private ServiceImpl mService;
        private HttpClient mHttpClient;

        @Before
        public void setUp() throws Exception {
            mControlPoint = mock(ControlPoint.class);
            mDevice = mock(Device.class);
            doReturn(mControlPoint).when(mDevice).getControlPoint();
            doReturn(new URL("http://192.0.2.2/")).when(mDevice).getAbsoluteUrl(anyString());
            mService = (ServiceImpl) spy(new ServiceImpl.Builder()
                    .setDevice(mDevice)
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setEventSubUrl("eventSubUrl")
                    .setDescription("description")
                    .build());
            doReturn("").when(mService).getCallback();
            mHttpClient = mock(HttpClient.class);
            doReturn(mHttpClient).when(mService).createHttpClient();
        }

        @Test
        public void subscribe_成功() throws Exception {
            final HttpResponse response = new HttpResponse();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.subscribe(), is(true));
        }

        @Test
        public void subscribe_SIDなし() throws Exception {
            final HttpResponse response = new HttpResponse();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.subscribe(), is(false));
        }

        @Test
        public void subscribe_Timeout値異常() throws Exception {
            final HttpResponse response = new HttpResponse();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-0");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.subscribe(), is(false));
        }

        @Test
        public void subscribe_Http応答異常() throws Exception {
            final HttpResponse response = new HttpResponse();
            response.setStatus(Http.Status.HTTP_INTERNAL_ERROR);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.subscribe(), is(false));
        }

        @Test
        public void subscribe_2回目はrenewがコールされfalseが返されると失敗() throws Exception {
            final HttpResponse response = new HttpResponse();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.subscribe(), is(true));

            doReturn(false).when(mService).renewSubscribeInner();

            assertThat(mService.subscribe(), is(false));
            verify(mService, times(1)).renewSubscribeInner();
        }

        @Test
        public void renewSubscribe_subscribe前はsubscribeが実行される() throws Exception {
            final HttpResponse response = new HttpResponse();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.renewSubscribe(), is(true));
            verify(mService, times(1)).subscribeInner(anyBoolean());
        }

        @Test
        public void renewSubscribe_2回目はrenewが実行される() throws Exception {
            final HttpResponse response = new HttpResponse();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.renewSubscribe(), is(true));
            verify(mService, times(1)).subscribeInner(anyBoolean());

            assertThat(mService.renewSubscribe(), is(true));
            verify(mService, times(1)).renewSubscribeInner();
        }

        @Test
        public void renewSubscribe_renewの応答ステータス異常で失敗() throws Exception {
            final HttpResponse response = new HttpResponse();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.renewSubscribe(), is(true));

            response.setStatus(Http.Status.HTTP_INTERNAL_ERROR);
            assertThat(mService.renewSubscribe(), is(false));
        }

        @Test
        public void renewSubscribe_sid不一致() throws Exception {
            final HttpResponse response = new HttpResponse();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.renewSubscribe(), is(true));

            response.setHeader(Http.SID, "sid2");
            assertThat(mService.renewSubscribe(), is(false));
        }

        @Test
        public void renewSubscribe_Timeout値異常() throws Exception {
            final HttpResponse response = new HttpResponse();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));

            assertThat(mService.renewSubscribe(), is(true));

            response.setHeader(Http.TIMEOUT, "second-0");
            assertThat(mService.renewSubscribe(), is(false));
        }

        @Test
        public void unsubscribe_subscribeする前に実行すると失敗() throws Exception {
            assertThat(mService.unsubscribe(), is(false));
        }

        @Test
        public void unsubscribe_正常() throws Exception {
            final HttpResponse response = new HttpResponse();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
            assertThat(mService.subscribe(), is(true));

            assertThat(mService.unsubscribe(), is(true));
        }

        @Test
        public void unsubscribe_OK以外は失敗() throws Exception {
            final HttpResponse response = new HttpResponse();
            response.setStatus(Http.Status.HTTP_OK);
            response.setHeader(Http.SID, "sid");
            response.setHeader(Http.TIMEOUT, "second-300");
            doReturn(response).when(mHttpClient).post(ArgumentMatchers.any(HttpRequest.class));
            assertThat(mService.subscribe(), is(true));

            response.setStatus(Http.Status.HTTP_INTERNAL_ERROR);

            assertThat(mService.unsubscribe(), is(false));
        }
    }
}