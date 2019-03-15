/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message;

import net.mm2d.upnp.Http;
import net.mm2d.upnp.SsdpMessage;
import net.mm2d.upnp.internal.server.Address;
import net.mm2d.upnp.internal.server.SsdpSearchServer;
import net.mm2d.upnp.util.TestUtils;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(Enclosed.class)
public class SsdpRequestTest {

    private static SsdpRequest makeFromResource(final String name) throws IOException {
        final byte[] data = TestUtils.getResourceAsByteArray(name);
        return SsdpRequest.create(mock(InetAddress.class), data, data.length);
    }

    @RunWith(JUnit4.class)
    public static class 作成 {
        @Test
        public void buildUp_所望のバイナリに変換できる() throws IOException {
            final SsdpRequest message = SsdpRequest.create();
            message.setMethod(SsdpMessage.M_SEARCH);
            message.setUri("*");
            message.setHeader(Http.HOST, Address.IP_V4.getAddressString());
            message.setHeader(Http.MAN, SsdpMessage.SSDP_DISCOVER);
            message.setHeader(Http.MX, "1");
            message.setHeader(Http.ST, SsdpSearchServer.ST_ROOTDEVICE);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.getMessage().writeData(baos);
            final byte[] actual = baos.toByteArray();

            final byte[] expected = TestUtils.getResourceAsByteArray("ssdp-search-request.bin");

            assertThat(new String(actual), is(new String(expected)));
        }

        @Test
        public void buildUp_受信データから作成() throws IOException {
            final SsdpRequest message = makeFromResource("ssdp-notify-alive0.bin");

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.getMessage().writeData(baos);
            final byte[] actual = baos.toByteArray();

            final byte[] expected = TestUtils.getResourceAsByteArray("ssdp-notify-alive0.bin");

            assertThat(new String(actual), is(new String(expected)));
        }

        @Test
        public void toString_messageのtoStringと等価() throws Exception {
            final SsdpRequest message = makeFromResource("ssdp-notify-alive0.bin");
            assertThat(message.toString(), is(message.getMessage().toString()));
        }

        @Test
        public void isPinned() throws Exception {
            final SsdpRequest message = makeFromResource("ssdp-notify-alive0.bin");
            assertThat(message.isPinned(), is(false));
        }
    }

    @RunWith(JUnit4.class)
    public static class 個別パラメータ {
        @Test
        public void getType() throws IOException {
            final SsdpRequest message = makeFromResource("ssdp-notify-alive2.bin");

            assertThat(message.getType(), is("urn:schemas-upnp-org:service:ContentDirectory:1"));

        }

        @Test
        public void getExpireTime() throws IOException {
            final long beforeTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(300);
            final SsdpRequest message = makeFromResource("ssdp-notify-alive2.bin");
            final long afterTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(300);

            assertThat(message.getExpireTime(), greaterThanOrEqualTo(beforeTime));
            assertThat(message.getExpireTime(), lessThanOrEqualTo(afterTime));
        }
    }

    @RunWith(Theories.class)
    public static class Notifyメッセージ {
        @DataPoints
        public static SsdpRequest[] getMessages() throws IOException {
            return new SsdpRequest[]{
                    makeFromResource("ssdp-notify-alive0.bin"),
                    makeFromResource("ssdp-notify-alive1.bin"),
                    makeFromResource("ssdp-notify-alive2.bin"),
                    makeFromResource("ssdp-notify-byebye0.bin"),
                    makeFromResource("ssdp-notify-byebye1.bin"),
                    makeFromResource("ssdp-notify-byebye2.bin"),
            };
        }

        @Theory
        public void getMethod_NOTIFYであること(final SsdpRequest message) {
            assertThat(message.getMethod(), is(SsdpMessage.NOTIFY));
        }

        @Theory
        public void getUri_アスタリスクであること(final SsdpRequest message) {
            assertThat(message.getUri(), is("*"));
        }

        @Theory
        public void getUuid_記述の値であること(final SsdpRequest message) {
            assertThat(message.getUuid(), is("uuid:01234567-89ab-cdef-0123-456789abcdef"));
        }

        @Theory
        public void getHeader_HOST_SSDPのアドレスであること(final SsdpRequest message) {
            assertThat(message.getHeader(Http.HOST), is(Address.IP_V4.getAddressString()));
        }
    }


    @RunWith(Theories.class)
    public static class Aliveメッセージ {
        @DataPoints
        public static SsdpRequest[] getMessages() throws IOException {
            return new SsdpRequest[]{
                    makeFromResource("ssdp-notify-alive0.bin"),
                    makeFromResource("ssdp-notify-alive1.bin"),
                    makeFromResource("ssdp-notify-alive2.bin"),
            };
        }

        @Theory
        public void getNts_NTSがAliveであること(final SsdpRequest message) {
            assertThat(message.getNts(), is(SsdpMessage.SSDP_ALIVE));
        }

        @Theory
        public void getMaxAge_CACHE_CONTROLの値が取れること(final SsdpRequest message) {
            assertThat(message.getMaxAge(), is(300));
        }

        @Theory
        public void getLocation_Locationの値が取れること(final SsdpRequest message) {
            assertThat(message.getLocation(), is("http://192.0.2.2:12345/device.xml"));
        }
    }

    @RunWith(Theories.class)
    public static class ByeByeメッセージ {
        @DataPoints
        public static SsdpRequest[] getMessages() throws IOException {
            return new SsdpRequest[]{
                    makeFromResource("ssdp-notify-byebye0.bin"),
                    makeFromResource("ssdp-notify-byebye1.bin"),
                    makeFromResource("ssdp-notify-byebye2.bin"),
            };
        }

        @Theory
        public void getNts_NTSがByebyeであること(final SsdpRequest message) {
            assertThat(message.getNts(), is(SsdpMessage.SSDP_BYEBYE));
        }
    }
}
