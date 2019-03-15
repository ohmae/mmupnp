/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.parser;

import net.mm2d.upnp.HttpClient;
import net.mm2d.upnp.internal.impl.DeviceImpl;
import net.mm2d.upnp.internal.impl.ServiceImpl;

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
