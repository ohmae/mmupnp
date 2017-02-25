/*
 * Copyright(C)  2017 大前良介(OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import javax.annotation.Nonnull;

public class TestUtils {
    @Nonnull
    public static File getResourceAsFile(String name) {
        return new File(getClassLoader().getResource(name).getFile());
    }

    @Nonnull
    public static InputStream getResourceAsStream(String name) {
        return getClassLoader().getResourceAsStream(name);
    }

    @Nonnull
    public static byte[] getResourceAsByteArray(String name) throws IOException {
        return Files.readAllBytes(getResourceAsFile(name).toPath());
    }

    @Nonnull
    public static String getResourceAsString(String name) throws IOException {
        return new String(Files.readAllBytes(getResourceAsFile(name).toPath()));
    }

    @Nonnull
    private static ClassLoader getClassLoader() {
        return TestUtils.class.getClassLoader();
    }
}
