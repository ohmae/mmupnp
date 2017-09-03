/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import org.junit.Test;

import java.net.URL;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class HttpClientTest {
    @Test
    public void downloadString() throws Exception {
        final String result = new HttpClient(false).downloadString(new URL("http://www.example.com/index.html"));
        assertThat(result, is(not((nullValue()))));
    }

    @Test
    public void downloadBinary() throws Exception {
        final byte[] result = new HttpClient(false).downloadBinary(new URL("http://www.example.com/index.html"));
        assertThat(result, is(not((nullValue()))));
    }
}