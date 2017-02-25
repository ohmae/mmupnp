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
import java.net.InterfaceAddress;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class SsdpResponseMessageTest {

    private static SsdpResponseMessage makeFromResource(String name) throws IOException {
        final byte[] data = TestUtils.getResourceAsByteArray(name);
        return new SsdpResponseMessage(mock(InterfaceAddress.class), data, data.length);
    }

    @RunWith(JUnit4.class)
    public static class 作成 {
        @Test
        public void buildUp_受信データから作成() throws IOException {
            final SsdpResponseMessage message = makeFromResource("ssdp-search-response0.bin");

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.getMessage().writeData(baos);
            final byte[] actual = baos.toByteArray();

            final byte[] expected = TestUtils.getResourceAsByteArray("ssdp-search-response0.bin");

            assertThat(new String(actual), is(new String(expected)));
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
        public void getUuid_記述の値であること(SsdpResponseMessage message) {
            assertThat(message.getUuid(), is("uuid:01234567-89ab-cdef-0123-456789abcdef"));
        }

        @Theory
        public void getMaxAge_CACHE_CONTROLの値が取れること(SsdpResponseMessage message) {
            assertThat(message.getMaxAge(), is(300));
        }

        @Theory
        public void getLocation_Locationの値が取れること(SsdpResponseMessage message) {
            assertThat(message.getLocation(), is("http://192.0.2.2:12345/device.xml"));
        }
    }
}
