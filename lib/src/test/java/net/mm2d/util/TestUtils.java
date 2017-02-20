/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import net.mm2d.upnp.SsdpRequestMessageTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.annotation.Nonnull;

public class TestUtils {
    @Nonnull
    public static File getResourceFile(String name) {
        return new File(TestUtils.class.getClassLoader().getResource(name).getFile());
    }

    @Nonnull
    public static byte[] getResourceData(String name) throws IOException {
        return Files.readAllBytes(getResourceFile(name).toPath());
    }
}
