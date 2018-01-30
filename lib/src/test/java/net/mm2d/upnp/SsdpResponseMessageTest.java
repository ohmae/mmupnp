/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import net.mm2d.util.TestUtils;

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
import java.net.InterfaceAddress;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class SsdpResponseMessageTest {

    private static SsdpResponseMessage makeFromResource(final String name) throws IOException {
        final byte[] data = TestUtils.getResourceAsByteArray(name);
        return new SsdpResponseMessage(mock(InterfaceAddress.class), data, data.length);
    }

    @RunWith(JUnit4.class)
    public static class 作成 {
        @Test
        public void buildUp_受信データから作成() throws Exception {
            final SsdpResponseMessage message = makeFromResource("ssdp-search-response0.bin");

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.getMessage().writeData(baos);
            final byte[] actual = baos.toByteArray();

            final byte[] expected = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin");

            assertThat(new String(actual), is(new String(expected)));
        }

        @Test
        public void setStatusCode() throws Exception {
            final SsdpResponseMessage message = makeFromResource("ssdp-search-response0.bin");
            message.setStatusCode(404);
            assertThat(message.getStatusCode(), is(404));
        }

        @Test
        public void setReasonPhrase() throws Exception {
            final SsdpResponseMessage message = makeFromResource("ssdp-search-response0.bin");
            message.setReasonPhrase("Not Found");
            assertThat(message.getReasonPhrase(), is("Not Found"));
        }

        @Test
        public void setStatus() throws Exception {
            final SsdpResponseMessage message = makeFromResource("ssdp-search-response0.bin");
            message.setStatus(Http.Status.HTTP_NOT_FOUND);
            assertThat(message.getStatus(), is(Http.Status.HTTP_NOT_FOUND));
        }

        @Test
        public void hasInvalidLocation_アドレス一致() throws Exception {
            final SsdpResponseMessage message = makeFromResource("ssdp-search-response0.bin");
            assertThat(message.hasInvalidLocation(InetAddress.getByName("192.0.2.2")), is(false));
        }

        @Test
        public void hasInvalidLocation_http以外() throws Exception {
            final SsdpResponseMessage message = makeFromResource("ssdp-search-response-invalid-location0.bin");
            assertThat(message.hasInvalidLocation(InetAddress.getByName("192.0.2.2")), is(true));
        }

        @Test
        public void hasInvalidLocation_表記に問題() throws Exception {
            final SsdpResponseMessage message = makeFromResource("ssdp-search-response-invalid-location1.bin");
            assertThat(message.hasInvalidLocation(InetAddress.getByName("192.0.2.2")), is(true));
        }
    }

    @RunWith(Theories.class)
    public static class Responseメッセージ {
        @DataPoints
        public static SsdpResponseMessage[] getMessages() throws IOException {
            return new SsdpResponseMessage[]{
                    makeFromResource("ssdp-search-response0.bin"),
                    makeFromResource("ssdp-search-response1.bin"),
            };
        }

        @Theory
        public void getStatus(final SsdpResponseMessage message) {
            assertThat(message.getStatus(), is(Http.Status.HTTP_OK));
        }

        @Theory
        public void getStatusCode(final SsdpResponseMessage message) {
            assertThat(message.getStatusCode(), is(200));
        }

        @Theory
        public void getReasonPhrase(final SsdpResponseMessage message) {
            assertThat(message.getReasonPhrase(), is("OK"));
        }

        @Theory
        public void getUuid_記述の値であること(final SsdpResponseMessage message) {
            assertThat(message.getUuid(), is("uuid:01234567-89ab-cdef-0123-456789abcdef"));
        }

        @Theory
        public void getMaxAge_CACHE_CONTROLの値が取れること(final SsdpResponseMessage message) {
            assertThat(message.getMaxAge(), is(300));
        }

        @Theory
        public void getLocation_Locationの値が取れること(final SsdpResponseMessage message) {
            assertThat(message.getLocation(), is("http://192.0.2.2:12345/device.xml"));
        }
    }
}
