/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.message;

import net.mm2d.upnp.Http;
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(Enclosed.class)
public class SsdpResponseTest {

    private static SsdpResponse makeFromResource(final String name) throws IOException {
        final byte[] data = TestUtils.getResourceAsByteArray(name);
        return SsdpResponse.create(mock(InetAddress.class), data, data.length);
    }

    @RunWith(JUnit4.class)
    public static class 作成 {
        @Test
        public void buildUp_受信データから作成() throws Exception {
            final SsdpResponse message = makeFromResource("ssdp-search-response0.bin");

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.getMessage().writeData(baos);
            final byte[] actual = baos.toByteArray();

            final byte[] expected = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin");

            assertThat(new String(actual), is(new String(expected)));
        }

        @Test
        public void setStatusCode() throws Exception {
            final SsdpResponse message = makeFromResource("ssdp-search-response0.bin");
            message.setStatusCode(404);
            assertThat(message.getStatusCode(), is(404));
        }

        @Test
        public void setReasonPhrase() throws Exception {
            final SsdpResponse message = makeFromResource("ssdp-search-response0.bin");
            message.setReasonPhrase("Not Found");
            assertThat(message.getReasonPhrase(), is("Not Found"));
        }

        @Test
        public void setStatus() throws Exception {
            final SsdpResponse message = makeFromResource("ssdp-search-response0.bin");
            message.setStatus(Http.Status.HTTP_NOT_FOUND);
            assertThat(message.getStatus(), is(Http.Status.HTTP_NOT_FOUND));
        }

        @Test
        public void isPinned() throws Exception {
            final SsdpResponse message = makeFromResource("ssdp-search-response0.bin");
            assertThat(message.isPinned(), is(false));
        }
    }

    @RunWith(Theories.class)
    public static class Responseメッセージ {
        @DataPoints
        public static SsdpResponse[] getMessages() throws IOException {
            return new SsdpResponse[]{
                    makeFromResource("ssdp-search-response0.bin"),
                    makeFromResource("ssdp-search-response1.bin"),
            };
        }

        @Theory
        public void getStatus(final SsdpResponse message) {
            assertThat(message.getStatus(), is(Http.Status.HTTP_OK));
        }

        @Theory
        public void getStatusCode(final SsdpResponse message) {
            assertThat(message.getStatusCode(), is(200));
        }

        @Theory
        public void getReasonPhrase(final SsdpResponse message) {
            assertThat(message.getReasonPhrase(), is("OK"));
        }

        @Theory
        public void getUuid_記述の値であること(final SsdpResponse message) {
            assertThat(message.getUuid(), is("uuid:01234567-89ab-cdef-0123-456789abcdef"));
        }

        @Theory
        public void getMaxAge_CACHE_CONTROLの値が取れること(final SsdpResponse message) {
            assertThat(message.getMaxAge(), is(300));
        }

        @Theory
        public void getLocation_Locationの値が取れること(final SsdpResponse message) {
            assertThat(message.getLocation(), is("http://192.0.2.2:12345/device.xml"));
        }
    }
}
