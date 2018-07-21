/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

import static org.mockito.Mockito.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class ServiceParserTest {
    @Test(expected = IOException.class)
    public void loadDescription_パラメータがとれないとException() throws Exception {
        ServiceParser.loadDescription(mock(HttpClient.class), mock(DeviceImpl.Builder.class), mock(ServiceImpl.Builder.class));
    }
}
