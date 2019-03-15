/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.internal.impl;

import net.mm2d.upnp.Icon;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(JUnit4.class)
public class IconBuilderTest {
    @Test
    public void build() throws Exception {
        final String mimeType = "mimeType";
        final int height = 200;
        final int width = 300;
        final int depth = 24;
        final String url = "http://192.168.0.1/";
        final Icon icon = new IconImpl.Builder()
                .setMimeType(mimeType)
                .setWidth(String.valueOf(width))
                .setHeight(String.valueOf(height))
                .setDepth(String.valueOf(depth))
                .setUrl(url)
                .build();

        assertThat(icon.getMimeType(), is(mimeType));
        assertThat(icon.getWidth(), is(width));
        assertThat(icon.getHeight(), is(height));
        assertThat(icon.getDepth(), is(depth));
        assertThat(icon.getUrl(), is(url));
    }

    @Test(expected = IllegalStateException.class)
    public void build_mimeTypeなし() throws Exception {
        final int height = 200;
        final int width = 300;
        final int depth = 24;
        final String url = "http://192.168.0.1/";
        new IconImpl.Builder()
                .setWidth(String.valueOf(width))
                .setHeight(String.valueOf(height))
                .setDepth(String.valueOf(depth))
                .setUrl(url)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_width異常() throws Exception {
        final String mimeType = "mimeType";
        final int height = 200;
        final int depth = 24;
        final String url = "http://192.168.0.1/";
        new IconImpl.Builder()
                .setMimeType(mimeType)
                .setWidth("width")
                .setHeight(String.valueOf(height))
                .setDepth(String.valueOf(depth))
                .setUrl(url)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_height異常() throws Exception {
        final String mimeType = "mimeType";
        final int width = 300;
        final int depth = 24;
        final String url = "http://192.168.0.1/";
        new IconImpl.Builder()
                .setMimeType(mimeType)
                .setWidth(String.valueOf(width))
                .setHeight("height")
                .setDepth(String.valueOf(depth))
                .setUrl(url)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_depth異常() throws Exception {
        final String mimeType = "mimeType";
        final int height = 200;
        final int width = 300;
        final String url = "http://192.168.0.1/";
        new IconImpl.Builder()
                .setMimeType(mimeType)
                .setWidth(String.valueOf(width))
                .setHeight(String.valueOf(height))
                .setDepth("depth")
                .setUrl(url)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void build_urlなし() throws Exception {
        final String mimeType = "mimeType";
        final int height = 200;
        final int width = 300;
        final int depth = 24;
        new IconImpl.Builder()
                .setMimeType(mimeType)
                .setWidth(String.valueOf(width))
                .setHeight(String.valueOf(height))
                .setDepth(String.valueOf(depth))
                .build();
    }
}
