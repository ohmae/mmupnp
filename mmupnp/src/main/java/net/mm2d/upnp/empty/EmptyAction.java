/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.empty;

import net.mm2d.upnp.Action;
import net.mm2d.upnp.Argument;
import net.mm2d.upnp.ControlPoints;
import net.mm2d.upnp.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EmptyAction implements Action {
    @Nonnull
    @Override
    public Service getService() {
        return ControlPoints.emptyService();
    }

    @Nonnull
    @Override
    public String getName() {
        return "";
    }

    @Nonnull
    @Override
    public List<Argument> getArgumentList() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Argument findArgument(@Nonnull final String name) {
        return null;
    }

    @Nonnull
    @Override
    public Map<String, String> invokeSync(@Nonnull final Map<String, String> argumentValues) throws IOException {
        throw new IOException("empty object");
    }

    @Nonnull
    @Override
    public Map<String, String> invokeSync(
            @Nonnull final Map<String, String> argumentValues,
            final boolean returnErrorResponse) throws IOException {
        throw new IOException("empty object");
    }

    @Nonnull
    @Override
    public Map<String, String> invokeCustomSync(
            @Nonnull final Map<String, String> argumentValues,
            @Nullable final Map<String, String> customNamespace,
            @Nonnull final Map<String, String> customArguments) throws IOException {
        throw new IOException("empty object");
    }

    @Nonnull
    @Override
    public Map<String, String> invokeCustomSync(
            @Nonnull final Map<String, String> argumentValues,
            @Nullable final Map<String, String> customNamespace,
            @Nonnull final Map<String, String> customArguments,
            final boolean returnErrorResponse) throws IOException {
        throw new IOException("empty object");
    }
}
