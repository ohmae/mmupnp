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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class ServiceTest {
    private static final String SID = "11234567-89ab-cdef-0123-456789abcdef";
    private static final String INTERFACE_ADDRESS = "192.0.2.3";
    private static final int EVENT_PORT = 100;
    private HttpClient mHttpClient;
    private SsdpMessage mSsdpMessage;
    private ControlPoint mControlPoint;
    private Device mDevice;
    private Service mCms;
    private Service mCds;
    private Service mMmupnp;

    @Before
    public void setUp() throws Exception {
        mHttpClient = mock(HttpClient.class);
        doReturn(TestUtils.getResourceAsString("device.xml"))
                .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/device.xml"));
        doReturn(TestUtils.getResourceAsString("cds.xml"))
                .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/cds.xml"));
        doReturn(TestUtils.getResourceAsString("cms.xml"))
                .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/cms.xml"));
        doReturn(TestUtils.getResourceAsString("mmupnp.xml"))
                .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/mmupnp.xml"));
        doReturn(TestUtils.getResourceAsByteArray("icon/icon120.jpg"))
                .when(mHttpClient).downloadBinary(new URL("http://192.0.2.2:12345/icon/icon120.jpg"));
        doReturn(TestUtils.getResourceAsByteArray("icon/icon48.jpg"))
                .when(mHttpClient).downloadBinary(new URL("http://192.0.2.2:12345/icon/icon48.jpg"));
        doReturn(TestUtils.getResourceAsByteArray("icon/icon120.png"))
                .when(mHttpClient).downloadBinary(new URL("http://192.0.2.2:12345/icon/icon120.png"));
        doReturn(TestUtils.getResourceAsByteArray("icon/icon48.png"))
                .when(mHttpClient).downloadBinary(new URL("http://192.0.2.2:12345/icon/icon48.png"));
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");
        final InterfaceAddress interfaceAddress = mock(InterfaceAddress.class);
        doReturn(InetAddress.getByName(INTERFACE_ADDRESS)).when(interfaceAddress).getAddress();
        mSsdpMessage = new SsdpRequestMessage(interfaceAddress, data, data.length);
        mControlPoint = mock(ControlPoint.class);
        doReturn(EVENT_PORT).when(mControlPoint).getEventPort();
        final Device.Builder builder = new Device.Builder(mControlPoint, mSsdpMessage);
        DeviceParser.loadDescription(mHttpClient, builder);
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
    public void findStateVariable() throws Exception {
        final String name = "A_ARG_TYPE_BrowseFlag";
        final StateVariable variable = mCds.findStateVariable(name);
        assertThat(variable.getName(), is(name));
    }

    private HttpResponse createSubscribeResponse() {
        final HttpResponse response = new HttpResponse();
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
    public void renewSubscribe() throws Exception {
        final MockHttpClientFactory factory = new MockHttpClientFactory();
        factory.setResponse(createSubscribeResponse());
        mCds.setHttpClientFactory(factory);
        mCds.subscribe();
        mCds.renewSubscribe();

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