/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp;

import javax.annotation.Nonnull;

public interface TaskExecutor {
    boolean execute(@Nonnull Runnable task);

    void terminate();
}
