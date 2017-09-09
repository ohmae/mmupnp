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

import java.net.InterfaceAddress;
import java.net.URL;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class DeviceTest {
    private HttpClient mHttpClient;
    private SsdpMessage mSsdpMessage;
    private ControlPoint mControlPoint;
    private Device.Builder mBuilder;

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
        mSsdpMessage = new SsdpRequestMessage(interfaceAddress, data, data.length);
        mControlPoint = mock(ControlPoint.class);
        mBuilder = new Device.Builder(mControlPoint, mSsdpMessage);
        DeviceParser.loadDescription(mHttpClient, mBuilder);
    }

    @Test
    public void loadIconBinary_NONE() throws Exception {
        final Device device = mBuilder.build();
        device.loadIconBinary(mHttpClient, IconFilter.NONE);
        final List<Icon> list = device.getIconList();
        for (final Icon icon : list) {
            assertThat(icon.getBinary(), is(nullValue()));
        }
    }

    @Test
    public void loadIconBinary_ALL() throws Exception {
        final Device device = mBuilder.build();
        device.loadIconBinary(mHttpClient, IconFilter.ALL);
        final List<Icon> list = device.getIconList();
        for (final Icon icon : list) {
            assertThat(icon.getBinary(), is(notNullValue()));
        }
    }

    @Test
    public void getControlPoint() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getControlPoint(), is(mControlPoint));
    }

    @Test
    public void setSsdpMessage() throws Exception {
        final Device device = mBuilder.build();
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive1.bin");
        final InterfaceAddress interfaceAddress = mock(InterfaceAddress.class);
        final SsdpMessage message = new SsdpRequestMessage(interfaceAddress, data, data.length);
        device.updateSsdpMessage(message);

        assertThat(device.getSsdpMessage(), is(message));
    }

    @Test
    public void getSsdpMessage() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getSsdpMessage(), is(mSsdpMessage));
    }

    @Test
    public void getExpireTime() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getExpireTime(), is(mSsdpMessage.getExpireTime()));
    }

    @Test
    public void getDescription() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getDescription(), is(TestUtils.getResourceAsString("device.xml")));
    }

    @Test
    public void getAbsoluteUrl_locationがホスト名のみ() throws Exception {
        final Device device = mBuilder.build();
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive2.bin");
        final InterfaceAddress interfaceAddress = mock(InterfaceAddress.class);
        final SsdpMessage message = new SsdpRequestMessage(interfaceAddress, data, data.length);

        final String url1 = "http://10.0.0.1:1000/hoge/fuga";
        final String url2 = "/hoge/fuga";
        final String url3 = "fuga";

        message.setHeader(Http.LOCATION, "http://10.0.0.1:1000/");
        message.updateHeader();
        device.updateSsdpMessage(message);

        assertThat(device.getAbsoluteUrl(url1), is(new URL("http://10.0.0.1:1000/hoge/fuga")));
        assertThat(device.getAbsoluteUrl(url2), is(new URL("http://10.0.0.1:1000/hoge/fuga")));
        assertThat(device.getAbsoluteUrl(url3), is(new URL("http://10.0.0.1:1000/fuga")));
    }

    @Test
    public void getAbsoluteUrl_locationがホスト名のみで末尾のスラッシュなし() throws Exception {
        final Device device = mBuilder.build();
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive2.bin");
        final InterfaceAddress interfaceAddress = mock(InterfaceAddress.class);
        final SsdpMessage message = new SsdpRequestMessage(interfaceAddress, data, data.length);

        final String url1 = "http://10.0.0.1:1000/hoge/fuga";
        final String url2 = "/hoge/fuga";
        final String url3 = "fuga";

        message.setHeader(Http.LOCATION, "http://10.0.0.1:1000");
        message.updateHeader();
        device.updateSsdpMessage(message);

        assertThat(device.getAbsoluteUrl(url1), is(new URL("http://10.0.0.1:1000/hoge/fuga")));
        assertThat(device.getAbsoluteUrl(url2), is(new URL("http://10.0.0.1:1000/hoge/fuga")));
        assertThat(device.getAbsoluteUrl(url3), is(new URL("http://10.0.0.1:1000/fuga")));
    }

    @Test
    public void getAbsoluteUrl_locationがファイル名で終わる() throws Exception {
        final Device device = mBuilder.build();
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive2.bin");
        final InterfaceAddress interfaceAddress = mock(InterfaceAddress.class);
        final SsdpMessage message = new SsdpRequestMessage(interfaceAddress, data, data.length);

        final String url1 = "http://10.0.0.1:1000/hoge/fuga";
        final String url2 = "/hoge/fuga";
        final String url3 = "fuga";

        message.setHeader(Http.LOCATION, "http://10.0.0.1:1000/hoge/fuga");
        message.updateHeader();
        device.updateSsdpMessage(message);

        assertThat(device.getAbsoluteUrl(url1), is(new URL("http://10.0.0.1:1000/hoge/fuga")));
        assertThat(device.getAbsoluteUrl(url2), is(new URL("http://10.0.0.1:1000/hoge/fuga")));
        assertThat(device.getAbsoluteUrl(url3), is(new URL("http://10.0.0.1:1000/hoge/fuga")));
    }

    @Test
    public void getAbsoluteUrl_locationがディレクトリ名で終わる() throws Exception {
        final Device device = mBuilder.build();
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive2.bin");
        final InterfaceAddress interfaceAddress = mock(InterfaceAddress.class);
        final SsdpMessage message = new SsdpRequestMessage(interfaceAddress, data, data.length);

        final String url1 = "http://10.0.0.1:1000/hoge/fuga";
        final String url2 = "/hoge/fuga";
        final String url3 = "fuga";

        message.setHeader(Http.LOCATION, "http://10.0.0.1:1000/hoge/fuga/");
        message.updateHeader();
        device.updateSsdpMessage(message);

        assertThat(device.getAbsoluteUrl(url1), is(new URL("http://10.0.0.1:1000/hoge/fuga")));
        assertThat(device.getAbsoluteUrl(url2), is(new URL("http://10.0.0.1:1000/hoge/fuga")));
        assertThat(device.getAbsoluteUrl(url3), is(new URL("http://10.0.0.1:1000/hoge/fuga/fuga")));
    }

    @Test
    public void getAbsoluteUrl_locationにクエリーがついている() throws Exception {
        final Device device = mBuilder.build();
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive2.bin");
        final InterfaceAddress interfaceAddress = mock(InterfaceAddress.class);
        final SsdpMessage message = new SsdpRequestMessage(interfaceAddress, data, data.length);

        final String url1 = "http://10.0.0.1:1000/hoge/fuga";
        final String url2 = "/hoge/fuga";
        final String url3 = "fuga";

        message.setHeader(Http.LOCATION, "http://10.0.0.1:1000/hoge/fuga?a=foo&b=bar");
        message.updateHeader();
        device.updateSsdpMessage(message);

        assertThat(device.getAbsoluteUrl(url1), is(new URL("http://10.0.0.1:1000/hoge/fuga")));
        assertThat(device.getAbsoluteUrl(url2), is(new URL("http://10.0.0.1:1000/hoge/fuga")));
        assertThat(device.getAbsoluteUrl(url3), is(new URL("http://10.0.0.1:1000/hoge/fuga")));
    }

    @Test
    public void getBaseUrl_URLBaseがなければLocationの値() throws Exception {
        final String location = "http://10.0.0.1:1000/";
        final Device device = mBuilder.build();
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive2.bin");
        final InterfaceAddress interfaceAddress = mock(InterfaceAddress.class);
        final SsdpMessage message = new SsdpRequestMessage(interfaceAddress, data, data.length);
        message.setHeader(Http.LOCATION, location);
        message.updateHeader();
        device.updateSsdpMessage(message);

        assertThat(device.getBaseUrl(), is(location));
    }

    @Test
    public void getBaseUrl_URLBaseがあればURLBaseの値() throws Exception {
        final String location = "http://10.0.0.1:1000/";
        final String urlBase = "http://10.0.0.1:1001/";
        mBuilder.setUrlBase(urlBase);
        final Device device = mBuilder.build();
        final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive2.bin");
        final InterfaceAddress interfaceAddress = mock(InterfaceAddress.class);
        final SsdpMessage message = new SsdpRequestMessage(interfaceAddress, data, data.length);
        message.setHeader(Http.LOCATION, location);
        message.updateHeader();
        device.updateSsdpMessage(message);

        assertThat(device.getBaseUrl(), is(urlBase));
    }

    @Test
    public void getValue() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getValue("deviceType"),
                is("urn:schemas-upnp-org:device:MediaServer:1"));
    }

    @Test
    public void getValue_with_ns() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getValue("deviceType", "urn:schemas-upnp-org:device-1-0"),
                is("urn:schemas-upnp-org:device:MediaServer:1"));
    }

    @Test
    public void getLocation() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getLocation(), is(mSsdpMessage.getLocation()));
    }

    @Test
    public void getIpAddress() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getIpAddress(), is("192.0.2.2"));
    }

    @Test
    public void getUdn() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getUdn(), is("uuid:01234567-89ab-cdef-0123-456789abcdef"));
    }

    @Test
    public void getDeviceType() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getDeviceType(), is("urn:schemas-upnp-org:device:MediaServer:1"));
    }

    @Test
    public void getFriendlyName() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getFriendlyName(), is("mmupnp"));
    }

    @Test
    public void getManufacture() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getManufacture(), is("mm2d.net"));
    }

    @Test
    public void getManufactureUrl() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getManufactureUrl(), is("http://www.mm2d.net/"));
    }

    @Test
    public void getModelName() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getModelName(), is("mmupnp"));
    }

    @Test
    public void getModelUrl() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getModelUrl(), is("http://www.mm2d.net/"));
    }

    @Test
    public void getModelDescription() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getModelDescription(), is("mmupnp test server"));
    }

    @Test
    public void getModelNumber() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getModelNumber(), is("ABCDEFG"));
    }

    @Test
    public void getSerialNumber() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getSerialNumber(), is("0123456789ABC"));
    }

    @Test
    public void getPresentationUrl() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getPresentationUrl(), is("http://192.0.2.2:12346/"));
    }

    @Test
    public void getIconList() throws Exception {
        final Device device = mBuilder.build();
        final List<Icon> list = device.getIconList();
        for (final Icon icon : list) {
            assertThat(icon.getMimeType(), is(anyOf(is("image/jpeg"), is("image/png"))));
            assertThat(icon.getWidth(), is(anyOf(is(48), is(120))));
            assertThat(icon.getHeight(), is(anyOf(is(48), is(120))));
            assertThat(icon.getDepth(), is(24));
            assertThat(icon.getUrl(), is(anyOf(
                    is("/icon/icon120.jpg"),
                    is("/icon/icon48.jpg"),
                    is("/icon/icon120.png"),
                    is("/icon/icon48.png"))));
        }
    }

    @Test
    public void getServiceList() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.getServiceList(), hasSize(3));
    }

    @Test
    public void findServiceById() throws Exception {
        final Device device = mBuilder.build();
        final Service cds = device.findServiceById("urn:upnp-org:serviceId:ContentDirectory");

        assertThat(cds, is(notNullValue()));
        assertThat(cds.getDevice(), is(device));
    }

    @Test
    public void findServiceByType() throws Exception {
        final Device device = mBuilder.build();
        final Service cds = device.findServiceByType("urn:schemas-upnp-org:service:ContentDirectory:1");

        assertThat(cds, is(notNullValue()));
        assertThat(cds.getDevice(), is(device));
    }

    @Test
    public void findAction() throws Exception {
        final Device device = mBuilder.build();
        final Action browse = device.findAction("Browse");

        assertThat(browse, is(notNullValue()));
        assertThat(browse.getService().getDevice(), is(device));

        final Action hogehoge = device.findAction("hogehoge");

        assertThat(hogehoge, is(nullValue()));
    }

    @Test
    public void toString_Nullでない() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.toString(), is(notNullValue()));
    }

    @Test
    public void hashCode_Exceptionが発生しない() throws Exception {
        final Device device = mBuilder.build();
        device.hashCode();
    }

    @Test
    public void equals_null比較可能() throws Exception {
        final Device device = mBuilder.build();
        assertThat(device.equals(null), is(false));
    }
}
