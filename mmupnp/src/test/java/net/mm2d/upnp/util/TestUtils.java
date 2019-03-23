/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;

import javax.annotation.Nonnull;

public final class TestUtils {
    @Nonnull
    public static File getResourceAsFile(final String name) {
        return new File(getClassLoader().getResource(name).getFile());
    }

    @Nonnull
    public static InputStream getResourceAsStream(final String name) {
        return getClassLoader().getResourceAsStream(name);
    }

    @Nonnull
    public static byte[] getResourceAsByteArray(final String name) throws IOException {
        return Files.readAllBytes(getResourceAsFile(name).toPath());
    }

    @Nonnull
    public static String getResourceAsString(final String name) throws IOException {
        return new String(Files.readAllBytes(getResourceAsFile(name).toPath()));
    }

    @Nonnull
    private static ClassLoader getClassLoader() {
        return TestUtils.class.getClassLoader();
    }

    @Nonnull
    public static InterfaceAddress createInterfaceAddress(
            final String address,
            final String broadcast,
            final int maskLength)
            throws ReflectiveOperationException, UnknownHostException {
        final InterfaceAddress interfaceAddress = Reflection.getConstructor(InterfaceAddress.class).newInstance();
        Reflection.setFieldValue(interfaceAddress, "address", InetAddress.getByName(address));
        Reflection.setFieldValue(interfaceAddress, "broadcast", InetAddress.getByName(broadcast));
        Reflection.setFieldValue(interfaceAddress, "maskLength", (short) maskLength);
        return interfaceAddress;
    }

    @Nonnull
    public static InterfaceAddress createInterfaceAddress(
            final InetAddress address,
            final String broadcast,
            final int maskLength)
            throws ReflectiveOperationException, UnknownHostException {
        final InterfaceAddress interfaceAddress = Reflection.getConstructor(InterfaceAddress.class).newInstance();
        Reflection.setFieldValue(interfaceAddress, "address", address);
        Reflection.setFieldValue(interfaceAddress, "broadcast", InetAddress.getByName(broadcast));
        Reflection.setFieldValue(interfaceAddress, "maskLength", (short) maskLength);
        return interfaceAddress;
    }
}
