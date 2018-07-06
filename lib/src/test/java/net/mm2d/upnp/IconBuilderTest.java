/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class IconBuilderTest {
    @Test
    public void build() throws Exception {
        final Device device = mock(Device.class);
        final String mimeType = "mimeType";
        final int height = 200;
        final int width = 300;
        final int depth = 24;
        final String url = "http://192.168.0.1/";
        final Icon icon = new IconImpl.Builder()
                .setDevice(device)
                .setMimeType(mimeType)
                .setWidth(String.valueOf(width))
                .setHeight(String.valueOf(height))
                .setDepth(String.valueOf(depth))
                .setUrl(url)
                .build();

        assertThat(icon.getDevice(), is(device));
        assertThat(icon.getMimeType(), is(mimeType));
        assertThat(icon.getWidth(), is(width));
        assertThat(icon.getHeight(), is(height));
        assertThat(icon.getDepth(), is(depth));
        assertThat(icon.getUrl(), is(url));
    }

    @Test(expected = IllegalStateException.class)
    public void build_deviceなし() throws Exception {
        final String mimeType = "mimeType";
        final int height = 200;
        final int width = 300;
        final int depth = 24;
        final String url = "http://192.168.0.1/";
        new IconImpl.Builder()
                .setMimeType(mimeType)
                .setWidth(String.valueOf(width))
                .setHeight(String.valueOf(height))
                .setDepth(String.valueOf(depth))
                .setUrl(url)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_mimeTypeなし() throws Exception {
        final Device device = mock(Device.class);
        final int height = 200;
        final int width = 300;
        final int depth = 24;
        final String url = "http://192.168.0.1/";
        new IconImpl.Builder()
                .setDevice(device)
                .setWidth(String.valueOf(width))
                .setHeight(String.valueOf(height))
                .setDepth(String.valueOf(depth))
                .setUrl(url)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_width異常() throws Exception {
        final Device device = mock(Device.class);
        final String mimeType = "mimeType";
        final int height = 200;
        final int depth = 24;
        final String url = "http://192.168.0.1/";
        new IconImpl.Builder()
                .setDevice(device)
                .setMimeType(mimeType)
                .setWidth("width")
                .setHeight(String.valueOf(height))
                .setDepth(String.valueOf(depth))
                .setUrl(url)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_height異常() throws Exception {
        final Device device = mock(Device.class);
        final String mimeType = "mimeType";
        final int width = 300;
        final int depth = 24;
        final String url = "http://192.168.0.1/";
        new IconImpl.Builder()
                .setDevice(device)
                .setMimeType(mimeType)
                .setWidth(String.valueOf(width))
                .setHeight("height")
                .setDepth(String.valueOf(depth))
                .setUrl(url)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_depth異常() throws Exception {
        final Device device = mock(Device.class);
        final String mimeType = "mimeType";
        final int height = 200;
        final int width = 300;
        final int depth = 24;
        final String url = "http://192.168.0.1/";
        new IconImpl.Builder()
                .setDevice(device)
                .setMimeType(mimeType)
                .setWidth(String.valueOf(width))
                .setHeight(String.valueOf(height))
                .setDepth("depth")
                .setUrl(url)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_urlなし() throws Exception {
        final Device device = mock(Device.class);
        final String mimeType = "mimeType";
        final int height = 200;
        final int width = 300;
        final int depth = 24;
        new IconImpl.Builder()
                .setDevice(device)
                .setMimeType(mimeType)
                .setWidth(String.valueOf(width))
                .setHeight(String.valueOf(height))
                .setDepth(String.valueOf(depth))
                .build();
    }
}
