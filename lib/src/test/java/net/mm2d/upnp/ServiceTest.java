/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
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
            final Service service = new Service.Builder()
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
            new Service.Builder()
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
            new Service.Builder()
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
            new Service.Builder()
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
            new Service.Builder()
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
            new Service.Builder()
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
            new Service.Builder()
                    .setDevice(mock(Device.class))
                    .setServiceType("serviceType")
                    .setServiceId("serviceId")
                    .setScpdUrl("scpdUrl")
                    .setControlUrl("controlUrl")
                    .setDescription("description")
                    .build();
        }
    }

    @RunWith(JUnit4.class)
    public static class DeviceParserによる生成からのテスト {
        private static final String SID = "11234567-89ab-cdef-0123-456789abcdef";
        private static final String INTERFACE_ADDRESS = "192.0.2.3";
        private static final int EVENT_PORT = 100;
        private ControlPoint mControlPoint;
        private Device mDevice;
        private Service mCms;
        private Service mCds;
        private Service mMmupnp;

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
            final SsdpMessage ssdpMessage = new SsdpRequestMessage(interfaceAddress, data, data.length);
            mControlPoint = mock(ControlPoint.class);
            doReturn(EVENT_PORT).when(mControlPoint).getEventPort();
            final Device.Builder builder = new Device.Builder(mControlPoint, ssdpMessage);
            DeviceParser.loadDescription(httpClient, builder);
            mDevice = builder.build();
            mCms = mDevice.findServiceById("urn:upnp-org:serviceId:ConnectionManager");
            mCds = mDevice.findServiceById("urn:upnp-org:serviceId:ContentDirectory");
            mMmupnp = mDevice.findServiceById("urn:upnp-org:serviceId:X_mmupnp");
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
        }

        @Test
        public void findAction() throws Exception {
            assertThat(mCms.findAction("Browse"), is(nullValue()));
            assertThat(mCds.findAction("Browse"), is(notNullValue()));
        }

        @Test
        public void getStateVariableList() throws Exception {
            assertThat(mCds.getStateVariableList(), hasSize(11));
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
            final MockHttpClientFactory factory = new MockHttpClientFactory();
            factory.setResponse(createSubscribeResponse());
            mCds.setHttpClientFactory(factory);
            mCds.subscribe();

            final HttpRequest request = factory.getHttpRequest();
            assertThat(request.getUri(), is(mCds.getEventSubUrl()));
            verify(mControlPoint).registerSubscribeService(mCds, false);

            final String callback = factory.getHttpRequest().getHeader(Http.CALLBACK);
            assertThat(callback.charAt(0), is('<'));
            assertThat(callback.charAt(callback.length() - 1), is('>'));
            final URL url = new URL(callback.substring(1, callback.length() - 2));
            assertThat(url.getHost(), is(INTERFACE_ADDRESS));
            assertThat(url.getPort(), is(EVENT_PORT));
            System.out.println();
        }

        @Test
        public void subscribe_keep() throws Exception {
            final MockHttpClientFactory factory = new MockHttpClientFactory();
            factory.setResponse(createSubscribeResponse());
            mCds.setHttpClientFactory(factory);
            mCds.subscribe(true);

            final HttpRequest request = factory.getHttpRequest();
            assertThat(request.getUri(), is(mCds.getEventSubUrl()));
            verify(mControlPoint).registerSubscribeService(mCds, true);
        }

        @Test
        public void renewSubscribe1() throws Exception {
            final MockHttpClientFactory factory = new MockHttpClientFactory();
            factory.setResponse(createSubscribeResponse());
            mCds.setHttpClientFactory(factory);
            mCds.subscribe();
            mCds.renewSubscribe();

            final HttpRequest request = factory.getHttpRequest();
            assertThat(request.getUri(), is(mCds.getEventSubUrl()));
        }

        @Test
        public void renewSubscribe2() throws Exception {
            final MockHttpClientFactory factory = new MockHttpClientFactory();
            factory.setResponse(createSubscribeResponse());
            mCds.setHttpClientFactory(factory);
            mCds.subscribe();
            mCds.subscribe();

            final HttpRequest request = factory.getHttpRequest();
            assertThat(request.getUri(), is(mCds.getEventSubUrl()));
        }

        @Test
        public void unsubscribe() throws Exception {
            final MockHttpClientFactory factory = new MockHttpClientFactory();
            factory.setResponse(createSubscribeResponse());
            mCds.setHttpClientFactory(factory);
            mCds.subscribe();
            mCds.unsubscribe();

            final HttpRequest request = factory.getHttpRequest();
            assertThat(request.getUri(), is(mCds.getEventSubUrl()));
            verify(mControlPoint).unregisterSubscribeService(mCds);
        }

        @Test
        public void expired() throws Exception {
            final MockHttpClientFactory factory = new MockHttpClientFactory();
            factory.setResponse(createSubscribeResponse());
            mCds.setHttpClientFactory(factory);
            mCds.subscribe();

            mCds.expired();
            assertThat(mCds.getSubscriptionId(), is(nullValue()));
            assertThat(mCds.getSubscriptionExpiryTime(), is(0L));
            assertThat(mCds.getSubscriptionStart(), is(0L));
            assertThat(mCds.getSubscriptionTimeout(), is(0L));
        }

        @Test
        public void getSubscriptionId() throws Exception {
            final MockHttpClientFactory factory = new MockHttpClientFactory();
            factory.setResponse(createSubscribeResponse());
            mCds.setHttpClientFactory(factory);
            mCds.subscribe();

            assertThat(mCds.getSubscriptionId(), is(SID));
        }

        @Test
        public void getSubscriptionStart() throws Exception {
            final MockHttpClientFactory factory = new MockHttpClientFactory();
            factory.setResponse(createSubscribeResponse());
            mCds.setHttpClientFactory(factory);
            final long before = System.currentTimeMillis();
            mCds.subscribe();
            final long after = System.currentTimeMillis();

            assertThat(mCds.getSubscriptionStart(), greaterThanOrEqualTo(before));
            assertThat(mCds.getSubscriptionStart(), lessThanOrEqualTo(after));
        }

        @Test
        public void getSubscriptionTimeout() throws Exception {
            final MockHttpClientFactory factory = new MockHttpClientFactory();
            factory.setResponse(createSubscribeResponse());
            mCds.setHttpClientFactory(factory);
            mCds.subscribe();

            assertThat(mCds.getSubscriptionTimeout(), is(TimeUnit.SECONDS.toMillis(300L)));
        }

        @Test
        public void getSubscriptionExpiryTime() throws Exception {
            final MockHttpClientFactory factory = new MockHttpClientFactory();
            factory.setResponse(createSubscribeResponse());
            mCds.setHttpClientFactory(factory);
            final long before = System.currentTimeMillis();
            mCds.subscribe();
            final long after = System.currentTimeMillis();

            assertThat(mCds.getSubscriptionExpiryTime(),
                    greaterThanOrEqualTo(before + TimeUnit.SECONDS.toMillis(300L)));
            assertThat(mCds.getSubscriptionExpiryTime(),
                    lessThanOrEqualTo(after + TimeUnit.SECONDS.toMillis(300L)));
        }

        @Test
        public void hashCode_Exceptionが発生しない() throws Exception {
            mCds.hashCode();
        }

        @Test
        public void equals_null比較可能() throws Exception {
            assertThat(mCds.equals(null), is(false));
        }
    }
}