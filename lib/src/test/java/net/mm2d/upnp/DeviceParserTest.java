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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class DeviceParserTest {
    private HttpClient mHttpClient;
    private SsdpMessage mSsdpMessage;
    private ControlPoint mControlPoint;

    @Before
    public void setUp() throws Exception {
        mHttpClient = mock(HttpClient.class);
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
    }

    @Test
    public void loadDescription1() throws Exception {
        doReturn(TestUtils.getResourceAsString("device.xml"))
                .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/device.xml"));

        final Device.Builder builder = new Device.Builder(mControlPoint, mSsdpMessage);
        DeviceParser.loadDescription(mHttpClient, builder);
        final Device device = builder.build();
        assertThat(device.getIconList(), hasSize(4));
        assertThat(device.getServiceList(), hasSize(3));
    }

    @Test
    public void loadDescription_no_icon_device() throws Exception {
        doReturn(TestUtils.getResourceAsString("device-no-icon.xml"))
                .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/device.xml"));

        final Device.Builder builder = new Device.Builder(mControlPoint, mSsdpMessage);
        DeviceParser.loadDescription(mHttpClient, builder);
        final Device device = builder.build();
        assertThat(device.getIconList(), hasSize(0));
        assertThat(device.getServiceList(), hasSize(3));
    }

    @Test
    public void loadDescription_no_service_device() throws Exception {
        doReturn(TestUtils.getResourceAsString("device-no-service.xml"))
                .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/device.xml"));

        final Device.Builder builder = new Device.Builder(mControlPoint, mSsdpMessage);
        DeviceParser.loadDescription(mHttpClient, builder);
        final Device device = builder.build();
        assertThat(device.getIconList(), hasSize(4));
        assertThat(device.getServiceList(), hasSize(0));
    }
}
