/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.parser;

import net.mm2d.upnp.ControlPoint;
import net.mm2d.upnp.Device;
import net.mm2d.upnp.HttpClient;
import net.mm2d.upnp.SsdpMessage;
import net.mm2d.upnp.internal.impl.ControlPointImpl;
import net.mm2d.upnp.internal.impl.DeviceImpl;
import net.mm2d.upnp.internal.manager.SubscribeManager;
import net.mm2d.upnp.internal.message.SsdpRequest;
import net.mm2d.upnp.util.TestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(Enclosed.class)
public class DeviceParserTest {
    @RunWith(JUnit4.class)
    public static class 全行程のテスト {
        private HttpClient mHttpClient;
        private SsdpMessage mSsdpMessage;
        private ControlPoint mControlPoint;
        private SubscribeManager mSubscribeManager;

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
            mSsdpMessage = SsdpRequest.create(mock(InetAddress.class), data, data.length);
            mControlPoint = mock(ControlPoint.class);
            mSubscribeManager = mock(SubscribeManager.class);
        }

        @Test
        public void loadDescription_正常系() throws Exception {
            doReturn(TestUtils.getResourceAsString("device.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/device.xml"));

            final DeviceImpl.Builder builder = new DeviceImpl.Builder(mControlPoint, mSubscribeManager, mSsdpMessage);
            DeviceParser.loadDescription(mHttpClient, builder);
            final Device device = builder.build();
            assertThat(device.getIconList(), hasSize(4));
            assertThat(device.getServiceList(), hasSize(3));

            assertThat(ControlPointImpl.collectEmbeddedUdn(device), empty());
        }

        @Test
        public void loadDescription_想定外のタグは無視する() throws Exception {
            doReturn(TestUtils.getResourceAsString("device-with-gabage.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/device.xml"));

            final DeviceImpl.Builder builder = new DeviceImpl.Builder(mControlPoint, mSubscribeManager, mSsdpMessage);
            DeviceParser.loadDescription(mHttpClient, builder);
            final Device device = builder.build();
            assertThat(device.getIconList(), hasSize(4));
            assertThat(device.getServiceList(), hasSize(4));
        }

        @Test
        public void loadDescription_特別対応() throws Exception {
            doReturn(TestUtils.getResourceAsString("device.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/device.xml"));
            doReturn(TestUtils.getResourceAsString("mmupnp-with-mistake.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/mmupnp.xml"));

            final DeviceImpl.Builder builder = new DeviceImpl.Builder(mControlPoint, mSubscribeManager, mSsdpMessage);
            DeviceParser.loadDescription(mHttpClient, builder);
            final Device device = builder.build();
            assertThat(device.getIconList(), hasSize(4));
            assertThat(device.getServiceList(), hasSize(3));
        }

        @Test
        public void loadDescription_no_icon_device() throws Exception {
            doReturn(TestUtils.getResourceAsString("device-no-icon.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/device.xml"));

            final DeviceImpl.Builder builder = new DeviceImpl.Builder(mControlPoint, mSubscribeManager, mSsdpMessage);
            DeviceParser.loadDescription(mHttpClient, builder);
            final Device device = builder.build();
            assertThat(device.getIconList(), hasSize(0));
            assertThat(device.getServiceList(), hasSize(3));
        }

        @Test
        public void loadDescription_no_service_device() throws Exception {
            doReturn(TestUtils.getResourceAsString("device-no-service.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/device.xml"));

            final DeviceImpl.Builder builder = new DeviceImpl.Builder(mControlPoint, mSubscribeManager, mSsdpMessage);
            DeviceParser.loadDescription(mHttpClient, builder);
            final Device device = builder.build();
            assertThat(device.getIconList(), hasSize(4));
            assertThat(device.getServiceList(), hasSize(0));
        }

        @Test
        public void loadDescription_with_url_base() throws Exception {
            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive0-for-url-base.bin");
            mSsdpMessage = SsdpRequest.create(mock(InetAddress.class), data, data.length);
            doReturn(TestUtils.getResourceAsString("device-with-url-base.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.3:12345/device.xml"));

            final DeviceImpl.Builder builder = new DeviceImpl.Builder(mControlPoint, mSubscribeManager, mSsdpMessage);
            DeviceParser.loadDescription(mHttpClient, builder);
            final Device device = builder.build();
            assertThat(device.getBaseUrl(), is("http://192.0.2.2:12345/"));
        }

        @Test
        public void loadDescription_with_embedded_device() throws Exception {
            doReturn(TestUtils.getResourceAsString("device-with-embedded-device.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/device.xml"));

            final DeviceImpl.Builder builder = new DeviceImpl.Builder(mControlPoint, mSubscribeManager, mSsdpMessage);
            DeviceParser.loadDescription(mHttpClient, builder);
            final Device device = builder.build();

            assertThat(device.isEmbeddedDevice(), is(false));
            assertThat(device.getParent(), is(nullValue()));
            // 無限ループしない
            assertThat(device.findDeviceByTypeRecursively(""), is(nullValue()));
            assertThat(device.getDeviceList(), hasSize(1));
            final Device device1 = device.findDeviceByType("urn:schemas-upnp-org:device:WANDevice:1");
            assertThat(device1.getDeviceList(), hasSize(1));
            assertThat(device1.findServiceById("urn:upnp-org:serviceId:WANCommonIFC1"), is(notNullValue()));
            assertThat(device1.getParent(), is(device));
            assertThat(device1.isEmbeddedDevice(), is(true));

            final Device device2 = device.findDeviceByTypeRecursively("urn:schemas-upnp-org:device:WANConnectionDevice:1");
            assertThat(device2.findServiceById("urn:upnp-org:serviceId:WANIPConn1"), is(notNullValue()));

            assertThat(device2.getUpc(), is("000000000000"));
            assertThat(device2.getParent(), is(device1));
            assertThat(device2.isEmbeddedDevice(), is(true));

            final Set<String> udns = ControlPointImpl.collectEmbeddedUdn(device);
            assertThat(udns, hasItem("uuid:01234567-89ab-cdef-0123-456789abcdee"));
            assertThat(udns, hasItem("uuid:01234567-89ab-cdef-0123-456789abcded"));
            assertThat(udns, not(hasItem("uuid:01234567-89ab-cdef-0123-456789abcdef")));
        }

        @Test
        public void loadDescription_with_embedded_device_異常系() throws Exception {
            doReturn(TestUtils.getResourceAsString("device-with-embedded-device.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/device.xml"));

            final DeviceImpl.Builder builder = new DeviceImpl.Builder(mControlPoint, mSubscribeManager, mSsdpMessage);
            DeviceParser.loadDescription(mHttpClient, builder);
            final Device device = builder.build();

            assertThat(device.findDeviceByType("urn:schemas-upnp-org:device:WANDevice:11"), is(nullValue()));
            assertThat(device.findDeviceByTypeRecursively("urn:schemas-upnp-org:device:WANConnectionDevice:11"), is(nullValue()));
        }

        @Test
        public void loadDescription_with_embedded_device_embedded_deviceへの伝搬() throws Exception {
            doReturn(TestUtils.getResourceAsString("device-with-embedded-device.xml"))
                    .when(mHttpClient).downloadString(new URL("http://192.0.2.2:12345/device.xml"));

            final DeviceImpl.Builder builder = new DeviceImpl.Builder(mControlPoint, mSubscribeManager, mSsdpMessage);
            DeviceParser.loadDescription(mHttpClient, builder);
            final Device device = builder.build();

            final byte[] data = TestUtils.getResourceAsByteArray("ssdp-notify-alive1.bin");
            final SsdpMessage message = SsdpRequest.create(mock(InetAddress.class), data, data.length);
            device.updateSsdpMessage(message);

            final Device device1 = device.findDeviceByType("urn:schemas-upnp-org:device:WANDevice:1");
            assertThat(device1.getSsdpMessage(), is(message));
        }
    }

    @RunWith(JUnit4.class)
    public static class 機能ごとのテスト {
        @Test(expected = IOException.class)
        public void loadDescription_ダウンロード失敗でIOException() throws Exception {
            final DeviceImpl.Builder builder = mock(DeviceImpl.Builder.class);
            doReturn("http://192.168.0.1/").when(builder).getLocation();
            doReturn(mock(SsdpMessage.class)).when(builder).getSsdpMessage();
            DeviceParser.loadDescription(mock(HttpClient.class), builder);
        }

        @Test(expected = IOException.class)
        public void parseDescription_deviceノードのないXMLを渡すとException() throws Exception {
            DeviceParser.parseDescription(mock(DeviceImpl.Builder.class),
                    "<?xml version=\"1.0\"?>\n" +
                            "<root xmlns=\"urn:schemas-upnp-org:device-1-0\">\n" +
                            "</root>");
        }
    }
}
